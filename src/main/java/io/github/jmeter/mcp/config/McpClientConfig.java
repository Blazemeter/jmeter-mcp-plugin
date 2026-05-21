package io.github.jmeter.mcp.config;

import io.github.jmeter.mcp.client.McpClientRegistry;
import io.github.jmeter.mcp.client.McpClientSettings;
import io.github.jmeter.mcp.client.TransportType;
import org.apache.jmeter.config.ConfigElement;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JMeter Configuration Element that owns the lifecycle of a shared
 * {@link McpSyncClient} for the duration of a test run.
 *
 * <p>On {@link #testStarted()} this element only registers connection settings
 * (no blocking I/O). The first {@code MCP Sampler} that references the same
 * {@link #NAME} performs transport setup, {@code initialize()}, and caches the
 * {@link McpSyncClient}. The client is closed in {@link #testEnded()}.
 */
public class McpClientConfig extends ConfigTestElement
        implements ConfigElement, TestStateListener {

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

    public McpClientSettings toSettings() {
        McpClientSettings s = new McpClientSettings();
        s.setName(getPropertyAsString(NAME, "mcpClient"));
        s.setTransport(TransportType.fromString(getPropertyAsString(TRANSPORT,
                TransportType.STDIO.name())));
        s.setServerUrl(getPropertyAsString(SERVER_URL, ""));
        s.setEndpoint(getPropertyAsString(ENDPOINT, ""));
        s.setStdioCommand(getPropertyAsString(STDIO_COMMAND, ""));
        s.setStdioArgs(getPropertyAsString(STDIO_ARGS, ""));
        s.setStdioEnv(getPropertyAsString(STDIO_ENV, ""));
        s.setClientName(getPropertyAsString(CLIENT_NAME, "jmeter-mcp-plugin"));
        s.setClientVersion(getPropertyAsString(CLIENT_VERSION, "0.1.0"));
        s.setRequestTimeoutMillis(getPropertyAsLong(REQUEST_TIMEOUT_MS, 30_000L));
        s.setInitializationTimeoutMillis(getPropertyAsLong(INIT_TIMEOUT_MS, 30_000L));
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
        startClient();
    }

    @Override
    public void testStarted(String host) {
        startClient();
    }

    @Override
    public void testEnded() {
        stopClient();
    }

    @Override
    public void testEnded(String host) {
        stopClient();
    }

    private void startClient() {
        McpClientSettings settings = toSettings();
        String registryName = settings.getName();
        try {
            LOG.info("Registering MCP client '{}' (transport {}; lazy connect on first sampler)",
                    registryName, settings.getTransport());
            McpClientRegistry.getInstance().registerDeferred(registryName, settings);
        } catch (RuntimeException ex) {
            LOG.error("Failed to register MCP client '{}': {}",
                    registryName, ex.getMessage(), ex);
            throw ex;
        }
    }

    private void stopClient() {
        String registryName = getPropertyAsString(NAME, "mcpClient");
        LOG.info("Stopping MCP client '{}'", registryName);
        McpClientRegistry.getInstance().remove(registryName);
    }
}
