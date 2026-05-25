package com.blazemeter.jmeter.mcp.config;

import com.blazemeter.jmeter.mcp.client.McpClientRegistry;
import com.blazemeter.jmeter.mcp.client.McpClientSettings;
import com.blazemeter.jmeter.mcp.client.McpJmeterThreads;
import com.blazemeter.jmeter.mcp.client.McpThreadScopedSettings;
import com.blazemeter.jmeter.mcp.client.TransportType;
import com.blazemeter.jmeter.mcp.server.McpServerProcessManager;
import com.blazemeter.jmeter.mcp.util.Strings;
import org.apache.jmeter.config.ConfigElement;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.ThreadListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JMeter Configuration Element that owns the per-thread lifecycle of an
 * {@link io.modelcontextprotocol.client.McpSyncClient}.
 *
 * <p>On {@link #threadStarted()} this element registers connection settings for
 * the current worker thread in {@link McpClientRegistry}. When
 * {@link #CONNECT_ON_STARTUP} is enabled it schedules connect on a background
 * thread pool (without blocking the worker). Otherwise the first
 * {@code MCP Sampler} on that thread performs connect and {@code initialize()}.
 * The client is closed in {@link #threadFinished()} and any remaining slots are
 * cleared in {@link #testEnded()}.
 */
public class McpClientConfig extends ConfigTestElement
        implements ConfigElement, TestStateListener, ThreadListener {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(McpClientConfig.class);

    public static final String NAME = "McpClientConfig.name";
    public static final String TRANSPORT = "McpClientConfig.transport";
    public static final String SERVER_URL = "McpClientConfig.serverUrl";
    public static final String ENDPOINT = "McpClientConfig.endpoint";
    public static final String STDIO_COMMAND = "McpClientConfig.stdioCommand";
    public static final String STDIO_ARGS = "McpClientConfig.stdioArgs";
    public static final String STDIO_ENV = "McpClientConfig.stdioEnv";
    public static final String CLIENT_NAME = "McpClientConfig.clientName";
    public static final String CLIENT_VERSION = "McpClientConfig.clientVersion";
    public static final String REQUEST_TIMEOUT_MS = "McpClientConfig.requestTimeoutMs";
    public static final String INIT_TIMEOUT_MS = "McpClientConfig.initTimeoutMs";
    public static final String CONNECT_ON_STARTUP = "McpClientConfig.connectOnStartup";

    public McpClientSettings toSettings() {
        McpClientSettings s = new McpClientSettings();
        s.setName(getPropertyAsString(NAME, "mcpClient"));
        s.setTransport(TransportType.fromString(getPropertyAsString(TRANSPORT,
                TransportType.STDIO.name())));
        s.setServerUrl(getPropertyAsString(SERVER_URL, ""));
        s.setEndpoint(getPropertyAsString(ENDPOINT, ""));
        s.setStdioCommand(Strings.trimToDefault(getPropertyAsString(STDIO_COMMAND, ""), ""));
        s.setStdioArgs(Strings.trimToDefault(getPropertyAsString(STDIO_ARGS, ""), ""));
        s.setStdioEnv(getPropertyAsString(STDIO_ENV, ""));
        s.setClientName(getPropertyAsString(CLIENT_NAME, "jmeter-mcp-plugin"));
        s.setClientVersion(getPropertyAsString(CLIENT_VERSION, "0.1.0"));
        s.setRequestTimeoutMillis(getPropertyAsLong(REQUEST_TIMEOUT_MS, 30_000L));
        s.setInitializationTimeoutMillis(getPropertyAsLong(INIT_TIMEOUT_MS, 30_000L));
        s.setConnectOnStartup(getPropertyAsBoolean(CONNECT_ON_STARTUP, false));
        return s;
    }

    @Override
    public void addConfigElement(ConfigElement config) {
        // No element to merge; MCP config is self-contained.
    }

    @Override
    public boolean expectsModification() {
        return false;
    }

    @Override
    public void testStarted() {
        // Per-thread registration happens in threadStarted().
    }

    @Override
    public void testStarted(String host) {
        // Per-thread registration happens in threadStarted().
    }

    @Override
    public void testEnded() {
        stopAllClients();
    }

    @Override
    public void testEnded(String host) {
        stopAllClients();
    }

    @Override
    public void threadStarted() {
        startClientForThread();
    }

    @Override
    public void threadFinished() {
        stopClientForThread();
    }

    private void startClientForThread() {
        McpClientSettings settings = scopedSettings();
        String registryName = settings.getName();
        String threadKey = McpJmeterThreads.currentThreadKey();
        try {
            if (settings.isConnectOnStartup()) {
                LOG.info("Scheduling MCP client '{}' on thread '{}' connect (transport {})",
                        registryName, threadKey, settings.getTransport());
                McpClientRegistry.getInstance().connectOnStartup(registryName, settings);
            } else {
                LOG.info("Registering MCP client '{}' on thread '{}' (transport {}; lazy connect)",
                        registryName, threadKey, settings.getTransport());
                McpClientRegistry.getInstance().registerDeferred(registryName, settings);
            }
        } catch (RuntimeException ex) {
            LOG.error("Failed to register MCP client '{}' on thread '{}': {}",
                    registryName, threadKey, ex.getMessage(), ex);
            throw ex;
        }
    }

    private void stopClientForThread() {
        McpClientSettings settings = scopedSettings();
        String registryName = settings.getName();
        LOG.info("Stopping MCP client '{}' on thread '{}'",
                registryName, McpJmeterThreads.currentThreadKey());
        McpClientRegistry.getInstance().remove(registryName);
        stopManagedServerIfNeeded(settings);
    }

    private void stopAllClients() {
        McpClientSettings settings = toSettings();
        String registryName = settings.getName();
        LOG.info("Stopping all MCP client slots for '{}'", registryName);
        McpClientRegistry.getInstance().removeAllForClientName(registryName);
        if (settings.getTransport() != TransportType.STDIO) {
            McpServerProcessManager.getInstance().stopAll();
        }
    }

    private McpClientSettings scopedSettings() {
        return McpThreadScopedSettings.forThread(
                toSettings(), McpJmeterThreads.currentThreadNum());
    }

    private static void stopManagedServerIfNeeded(McpClientSettings settings) {
        if (settings.getTransport() == TransportType.STDIO) {
            return;
        }
        McpServerProcessManager.getInstance().stop();
    }
}
