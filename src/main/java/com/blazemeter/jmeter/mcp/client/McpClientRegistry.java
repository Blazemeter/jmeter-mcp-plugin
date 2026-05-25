package com.blazemeter.jmeter.mcp.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import io.modelcontextprotocol.client.McpSyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry of live {@link McpSyncClient} instances keyed by configuration name
 * and JMeter worker thread.
 *
 * <p>Each thread gets its own client (and typically its own MCP server instance
 * started via {@link com.blazemeter.jmeter.mcp.server.McpServerProcess}).
 * Connect work runs on a bounded background pool so {@code threadStarted()} and
 * {@code testStarted()} do not block the JMeter engine thread.
 */
public final class McpClientRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(McpClientRegistry.class);

    private static final int CONNECT_POOL_SIZE =
            Math.max(4, Runtime.getRuntime().availableProcessors());

    private static final ExecutorService CONNECT_EXECUTOR = Executors.newFixedThreadPool(
            CONNECT_POOL_SIZE,
            new ThreadFactory() {
                private final AtomicInteger seq = new AtomicInteger();

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "mcp-client-connect-" + seq.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                }
            });

    private static final McpClientRegistry INSTANCE = new McpClientRegistry();

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

    static String slotKey(String clientName, String threadKey) {
        return clientName + "::" + threadKey;
    }

    private static String slotKey(String clientName) {
        return slotKey(clientName, McpJmeterThreads.currentThreadKey());
    }

    /**
     * Store settings for the current thread's client. Does not open a connection.
     */
    public void registerDeferred(String name, McpClientSettings settings) {
        requireClientName(name);
        deferredSettings.put(slotKey(name), settings);
    }

    /**
     * Register settings for the current thread and schedule connect on the pool.
     */
    public void connectOnStartup(String name, McpClientSettings settings) {
        requireClientName(name);
        String threadKey = McpJmeterThreads.currentThreadKey();
        String key = slotKey(name, threadKey);
        McpClientSettings previous = deferredSettings.put(key, settings);
        if (previous != null) {
            LOG.warn("MCP client '{}' on thread '{}' settings replaced (was {}, now {})",
                    name, threadKey, previous.getTransport(), settings.getTransport());
        }
        CompletableFuture<McpSyncClient> old = pendingConnects.remove(key);
        if (old != null && !old.isDone()) {
            old.cancel(true);
        }
        pendingConnects.put(key, scheduleConnect(name, threadKey));
    }

    /**
     * Returns an initialized client for the current thread, creating it on first use.
     */
    public McpSyncClient getOrConnect(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String threadKey = McpJmeterThreads.currentThreadKey();
        String key = slotKey(name, threadKey);
        McpSyncClient existing = clients.get(key);
        if (existing != null) {
            return existing;
        }
        CompletableFuture<McpSyncClient> pending = pendingConnects.get(key);
        if (pending != null) {
            try {
                McpSyncClient client = pending.get();
                if (client != null) {
                    return client;
                }
                LOG.warn("Startup connect for MCP client '{}' on thread '{}' returned no client — retrying",
                        name, threadKey);
                return connectClient(name, threadKey, "retry after null startup");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(
                        "Interrupted waiting for MCP client '" + name + "' on thread '"
                                + threadKey + "'", ex);
            } catch (ExecutionException ex) {
                pendingConnects.remove(key, pending);
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                LOG.warn("Startup connect failed for MCP client '{}' on thread '{}': {} — retrying",
                        name, threadKey, cause.getMessage());
                return connectClient(name, threadKey, "retry after failed startup");
            }
        }
        return connectClient(name, threadKey, "lazy init");
    }

    private CompletableFuture<McpSyncClient> scheduleConnect(String name, String threadKey) {
        return CompletableFuture.supplyAsync(
                () -> connectClient(name, threadKey, "startup connect"), CONNECT_EXECUTOR);
    }

    private McpSyncClient connectClient(String name, String threadKey, String reason) {
        String key = slotKey(name, threadKey);
        Object lock = connectLocks.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            McpSyncClient existing = clients.get(key);
            if (existing != null) {
                return existing;
            }
            McpClientSettings settings = deferredSettings.get(key);
            if (settings == null) {
                return null;
            }
            LOG.info("Connecting MCP client '{}' on thread '{}' ({}, transport {})",
                    name, threadKey, reason, settings.getTransport());
            McpSdkLogSilencer.ensureApplied();
            McpSyncClient client;
            try {
                client = McpClientFactory.buildAndInitialize(settings);
            } catch (RuntimeException ex) {
                throw ex;
            }
            if (!deferredSettings.containsKey(key)) {
                closeQuietly(client);
                return null;
            }
            clients.put(key, client);
            pendingConnects.remove(key);
            return client;
        }
    }

    /** Remove and close the client for the current thread. */
    public void remove(String name) {
        removeSlot(slotKey(name));
    }

    /** Remove and close all thread slots registered under the given client name. */
    public void removeAllForClientName(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        String prefix = name + "::";
        List<String> keys = new ArrayList<>();
        for (String key : deferredSettings.keySet()) {
            if (key.startsWith(prefix)) {
                keys.add(key);
            }
        }
        for (String key : clients.keySet()) {
            if (key.startsWith(prefix) && !keys.contains(key)) {
                keys.add(key);
            }
        }
        for (String key : keys) {
            removeSlot(key);
        }
    }

    private void removeSlot(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        Object lock = connectLocks.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            pendingConnects.remove(key);
            deferredSettings.remove(key);
            McpSyncClient client = clients.remove(key);
            if (client != null) {
                closeQuietly(client);
            }
        }
        connectLocks.remove(key, lock);
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
