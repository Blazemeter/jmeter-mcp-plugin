package com.blazemeter.jmeter.mcp.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import com.blazemeter.jmeter.mcp.McpRuntimeCleanup;
import io.modelcontextprotocol.client.McpSyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process-wide registry of live {@link McpSyncClient} instances keyed by the
 * configuration element name.
 *
 * <p>JMeter clones config elements per thread, but the underlying connection
 * is heavy and must be shared. {@link #registerDeferred(String, McpClientSettings)}
 * stores settings during {@code testStarted()} when lazy connect is configured.
 * {@link #connectOnStartup(String, McpClientSettings)} registers settings and
 * schedules connect on a background thread so {@code testStarted()} returns
 * immediately (same responsiveness as lazy init). The first sampler that calls
 * {@link #getOrConnect(String)} waits for that background connect or performs
 * lazy connect if startup connect was not requested.
 *
 * <p>All use of a connected client (including RPCs from samplers) must go through
 * {@link #withClient(String, Function)} so concurrent threads do not call the MCP SDK
 * in parallel on the same transport (STDIO uses a unicast outbound sink that rejects
 * concurrent {@code tryEmitNext} with "Failed to enqueue message").
 */
public final class McpClientRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(McpClientRegistry.class);

    private static final ExecutorService CONNECT_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "mcp-client-connect");
        t.setDaemon(true);
        return t;
    });

    private static final McpClientRegistry INSTANCE = new McpClientRegistry();

    /** STDIO spawns are heavy; serializing avoids parallel process startup timeouts. */
    private static final Object STDIO_CONNECT_LOCK = new Object();

    private final Map<String, McpClientSettings> deferredSettings = new ConcurrentHashMap<>();
    private final Map<String, McpSyncClient> clients = new ConcurrentHashMap<>();
    private final Map<String, Object> connectLocks = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<McpSyncClient>> pendingConnects =
            new ConcurrentHashMap<>();
    private final Set<String> previewStartedManagedServer = ConcurrentHashMap.newKeySet();

    private McpClientRegistry() {
    }

    public static McpClientRegistry getInstance() {
        McpRuntimeCleanup.ensureRegistered();
        return INSTANCE;
    }

    /**
     * Store settings for a named client. Does not open a connection — that
     * happens on the first {@link #getOrConnect(String)} call.
     */
    public void registerDeferred(String name, McpClientSettings settings) {
        requireClientName(name);
        if (adoptExistingConnection(name, settings)) {
            return;
        }
        deferredSettings.put(name, settings);
    }

    /**
     * Returns whether a live, initialized client is registered under {@code name}.
     */
    public boolean isConnected(String name) {
        return name != null && !name.isBlank() && clients.containsKey(name);
    }

    /**
     * Returns the connected client, or {@code null} if not connected.
     */
    public McpSyncClient getConnected(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return clients.get(name);
    }

    /**
     * Connect immediately from the GUI (Start Now). Replaces any existing client
     * registered under the same name.
     */
    public void connectNow(McpClientSettings settings, boolean startedManagedServer) {
        String name = settings != null ? settings.getName() : null;
        requireClientName(name);
        remove(name);
        deferredSettings.put(name, settings);
        if (startedManagedServer) {
            previewStartedManagedServer.add(name);
        }
        connectClientUnderLock(name, "gui start now");
    }

    /**
     * Disconnect a GUI-started client. Returns whether a managed server subprocess
     * started via Start Now should be stopped.
     */
    public boolean disconnectNow(String name) {
        boolean stopManagedServer = previewStartedManagedServer.remove(name);
        remove(name);
        return stopManagedServer;
    }

    /**
     * When Start Now (or a prior test) left a client connected, keep it and only
     * refresh settings for the upcoming run.
     */
    private boolean adoptExistingConnection(String name, McpClientSettings settings) {
        McpSyncClient client = clients.get(name);
        if (client == null) {
            return false;
        }
        if (!isClientHealthy(client)) {
            LOG.info("Discarding stale MCP client '{}' before test run", name);
            remove(name);
            return false;
        }
        LOG.info("Reusing existing MCP client '{}' for test run (transport {})",
                name, settings.getTransport());
        deferredSettings.put(name, settings);
        CompletableFuture<McpSyncClient> pending = pendingConnects.remove(name);
        if (pending != null && !pending.isDone()) {
            pending.cancel(true);
        }
        return true;
    }

    private static boolean isClientHealthy(McpSyncClient client) {
        try {
            client.ping();
            return true;
        } catch (RuntimeException ex) {
            LOG.debug("MCP client health check failed: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Register settings and schedule connect on a background thread. Does not
     * block {@code testStarted()}.
     */
    public void connectOnStartup(String name, McpClientSettings settings) {
        requireClientName(name);
        if (adoptExistingConnection(name, settings)) {
            return;
        }
        McpClientSettings previous = deferredSettings.put(name, settings);
        if (previous != null) {
            LOG.warn("MCP client '{}' settings replaced (was transport {}, now {})",
                    name, previous.getTransport(), settings.getTransport());
        }
        CompletableFuture<McpSyncClient> old = pendingConnects.remove(name);
        if (old != null && !old.isDone()) {
            old.cancel(true);
        }
        pendingConnects.put(name, scheduleConnect(name));
    }

    /**
     * Returns an initialized client, creating it on first use from deferred
     * settings registered via {@link #registerDeferred(String, McpClientSettings)}.
     *
     * <p>Prefer {@link #withClient(String, Function)} for sampler and GUI catalog
     * calls so RPCs are serialized per client name.
     */
    public McpSyncClient getOrConnect(String name) {
        return withClient(name, Function.identity());
    }

    /**
     * Resolves (and if needed connects) the named client, then runs {@code action}
     * while holding the per-client lock so only one thread uses the transport at a time.
     */
    public <T> T withClient(String name, Function<McpSyncClient, T> action) {
        return withClient(name, action, null);
    }

    /**
     * Runs {@code action} under the per-client lock without connecting. For GUI catalog
     * sync when the client was started via Start Now.
     */
    public <T> T withConnectedClient(String name, Function<McpSyncClient, T> action) {
        if (name == null || name.isBlank()) {
            return action.apply(null);
        }
        McpSyncClient client = getConnected(name);
        Object lock = connectLocks.computeIfAbsent(name, k -> new Object());
        synchronized (lock) {
            return action.apply(client);
        }
    }

    /**
     * @param connectMillisHolder if non-null and length &gt; 0, receives time spent in
     *                            {@link #resolveClient(String)} (connect / wait for startup connect)
     */
    public <T> T withClient(String name, Function<McpSyncClient, T> action, long[] connectMillisHolder) {
        if (name == null || name.isBlank()) {
            return action.apply(null);
        }
        long connectStart = System.currentTimeMillis();
        McpSyncClient client = resolveClient(name);
        if (connectMillisHolder != null && connectMillisHolder.length > 0) {
            connectMillisHolder[0] = System.currentTimeMillis() - connectStart;
        }
        Object lock = connectLocks.computeIfAbsent(name, k -> new Object());
        synchronized (lock) {
            return action.apply(client);
        }
    }

    private McpSyncClient resolveClient(String name) {
        McpSyncClient existing = clients.get(name);
        if (existing != null) {
            return existing;
        }
        CompletableFuture<McpSyncClient> pending = pendingConnects.get(name);
        if (pending != null) {
            try {
                return pending.get();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted waiting for MCP client '" + name + "'", ex);
            } catch (ExecutionException ex) {
                pendingConnects.remove(name, pending);
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                LOG.warn("Startup connect failed for MCP client '{}': {} — retrying",
                        name, cause.getMessage());
                return connectClientUnderLock(name, "retry after failed startup");
            }
        }
        return connectClientUnderLock(name, "lazy init");
    }

    private McpSyncClient connectClientUnderLock(String name, String reason) {
        Object lock = connectLocks.computeIfAbsent(name, k -> new Object());
        synchronized (lock) {
            return connectClient(name, reason);
        }
    }

    private CompletableFuture<McpSyncClient> scheduleConnect(String name) {
        return CompletableFuture.supplyAsync(
                () -> connectClientUnderLock(name, "startup connect"), CONNECT_EXECUTOR);
    }

    private McpSyncClient connectClient(String name, String reason) {
        McpSyncClient existing = clients.get(name);
        if (existing != null) {
            return existing;
        }
        McpClientSettings settings = deferredSettings.get(name);
        if (settings == null) {
            return null;
        }
        LOG.info("Connecting MCP client '{}' ({}, transport {})",
                name, reason, settings.getTransport());
        McpSdkLogSilencer.ensureApplied();
        McpSyncClient client = null;
        try {
            if (settings.getTransport() == TransportType.STDIO) {
                synchronized (STDIO_CONNECT_LOCK) {
                    client = McpClientFactory.buildAndInitialize(settings);
                }
            } else {
                client = McpClientFactory.buildAndInitialize(settings);
            }
        } catch (RuntimeException ex) {
            if (client != null) {
                closeQuietly(client);
            }
            throw ex;
        }
        if (!deferredSettings.containsKey(name)) {
            closeQuietly(client);
            return null;
        }
        clients.put(name, client);
        pendingConnects.remove(name);
        return client;
    }

    /** Remove and close the client registered under the given name. */
    public void remove(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        Object lock = connectLocks.computeIfAbsent(name, k -> new Object());
        synchronized (lock) {
            pendingConnects.remove(name);
            deferredSettings.remove(name);
            McpSyncClient client = clients.remove(name);
            if (client != null) {
                closeQuietly(client);
            }
        }
        connectLocks.remove(name, lock);
    }

    public boolean shouldStopPreviewManagedServer(String name) {
        return previewStartedManagedServer.remove(name);
    }

    /** Close every registered client and clear deferred settings. */
    public void shutdownAll() {
        List<String> names = new ArrayList<>();
        names.addAll(clients.keySet());
        names.addAll(deferredSettings.keySet());
        names.addAll(pendingConnects.keySet());
        for (String name : names) {
            remove(name);
        }
        previewStartedManagedServer.clear();
    }

    private static void closeQuietly(McpSyncClient client) {
        try {
            client.closeGracefully();
        } catch (RuntimeException ex) {
            if (isExpectedShutdownFailure(ex)) {
                LOG.debug("MCP client graceful close failed during shutdown (server may be gone): {}",
                        ex.getMessage());
            } else {
                LOG.warn("Error while closing MCP client gracefully", ex);
            }
            try {
                client.close();
            } catch (RuntimeException ignored) {
                // best effort
            }
        }
    }

    private static void requireClientName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("MCP client name must not be empty");
        }
    }

    private static boolean isExpectedShutdownFailure(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof java.net.ConnectException
                    || t instanceof java.nio.channels.ClosedChannelException) {
                return true;
            }
            if (t instanceof java.io.IOException) {
                String msg = t.getMessage();
                if (msg != null && (msg.contains("header parser received no bytes")
                        || msg.contains("Connection reset")
                        || msg.contains("EOF reached while reading")
                        || msg.contains("chunked transfer encoding"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
