package io.github.jmeter.mcp.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.modelcontextprotocol.client.McpSyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process-wide registry of live {@link McpSyncClient} instances keyed by the
 * configuration element name.
 *
 * <p>JMeter clones config elements per thread, but the underlying connection
 * is heavy and must be shared. {@link #registerDeferred(String, McpClientSettings)}
 * is invoked from {@code testStarted()} (fast — no network I/O). The first
 * sampler that calls {@link #getOrConnect(String)} performs the actual
 * transport setup and {@code initialize()} so the GUI thread is not blocked
 * for tens of seconds before JMeter flips to the &quot;running&quot; state.
 */
public final class McpClientRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(McpClientRegistry.class);

    private static final McpClientRegistry INSTANCE = new McpClientRegistry();

    private final Map<String, McpClientSettings> deferredSettings = new ConcurrentHashMap<>();
    private final Map<String, McpSyncClient> clients = new ConcurrentHashMap<>();
    private final Map<String, Object> connectLocks = new ConcurrentHashMap<>();

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
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("MCP client name must not be empty");
        }
        deferredSettings.put(name, settings);
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
        Object lock = connectLocks.computeIfAbsent(name, k -> new Object());
        synchronized (lock) {
            existing = clients.get(name);
            if (existing != null) {
                return existing;
            }
            McpClientSettings settings = deferredSettings.get(name);
            if (settings == null) {
                return null;
            }
            LOG.info("Connecting MCP client '{}' (lazy init, transport {})",
                    name, settings.getTransport());
            McpSdkLogSilencer.ensureApplied();
            McpSyncClient client = McpClientFactory.buildAndInitialize(settings);
            clients.put(name, client);
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
            LOG.warn("Error while closing MCP client gracefully", ex);
            try {
                client.close();
            } catch (RuntimeException ignored) {
                // best effort
            }
        }
    }
}
