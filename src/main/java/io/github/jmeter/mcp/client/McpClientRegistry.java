package io.github.jmeter.mcp.client;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private McpClientRegistry() {
    }

    public static McpClientRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Store settings for a named client. Does not open a connection — that
     * happens on the first {@link #getOrConnect(String)} call.
     */
    public void registerDeferred(String name, McpClientSettings settings) {
        requireClientName(name);
        deferredSettings.put(name, settings);
    }

    /**
     * Register settings and schedule connect on a background thread. Does not
     * block {@code testStarted()}.
     */
    public void connectOnStartup(String name, McpClientSettings settings) {
        requireClientName(name);
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
     */
    public McpSyncClient getOrConnect(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
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
                return connectClient(name, "retry after failed startup");
            }
        }
        return connectClient(name, "lazy init");
    }

    private CompletableFuture<McpSyncClient> scheduleConnect(String name) {
        return CompletableFuture.supplyAsync(
                () -> connectClient(name, "startup connect"), CONNECT_EXECUTOR);
    }

    private McpSyncClient connectClient(String name, String reason) {
        Object lock = connectLocks.computeIfAbsent(name, k -> new Object());
        synchronized (lock) {
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
