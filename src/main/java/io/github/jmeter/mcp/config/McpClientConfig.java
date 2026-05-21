package io.github.jmeter.mcp.config;

import io.github.jmeter.mcp.client.McpClientRegistry;
import io.github.jmeter.mcp.client.McpClientSettings;
import io.github.jmeter.mcp.client.TransportType;
import io.github.jmeter.mcp.server.McpServerProcessManager;
import io.github.jmeter.mcp.util.Strings;
import org.apache.jmeter.config.ConfigElement;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JMeter Configuration Element that owns the lifecycle of a shared
 * {@link McpSyncClient} for the duration of a test run.
 *
 * <p>On {@link #testStarted()} this element registers connection settings in
 * {@link McpClientRegistry}. When {@link #CONNECT_ON_STARTUP} is enabled it
 * also schedules connect on a background thread during {@code testStarted()}
 * (without blocking the engine). Otherwise the first {@code MCP Sampler} that
 * references the same {@link #NAME} performs connect and {@code initialize()}.
 * The client is closed in {@link #testEnded()}.
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
            if (settings.isConnectOnStartup()) {
                LOG.info("Scheduling MCP client '{}' connect on test start (transport {})",
                        registryName, settings.getTransport());
                McpClientRegistry.getInstance().connectOnStartup(registryName, settings);
            } else {
                LOG.info("Registering MCP client '{}' (transport {}; lazy connect on first sampler)",
                        registryName, settings.getTransport());
                McpClientRegistry.getInstance().registerDeferred(registryName, settings);
            }
        } catch (RuntimeException ex) {
            LOG.error("Failed to register MCP client '{}': {}",
                    registryName, ex.getMessage(), ex);
            throw ex;
        }
    }

    private void stopClient() {
        McpClientSettings settings = toSettings();
        String registryName = settings.getName();
        LOG.info("Stopping MCP client '{}'", registryName);
        McpClientRegistry.getInstance().remove(registryName);
        stopManagedServerIfNeeded(settings);
    }

    private static void stopManagedServerIfNeeded(McpClientSettings settings) {
        if (settings.getTransport() == TransportType.STDIO) {
            return;
        }
        McpServerProcessManager manager = McpServerProcessManager.getInstance();
        if (manager.isManagedProcessRunning()) {
            manager.stop();
        }
    }
}
