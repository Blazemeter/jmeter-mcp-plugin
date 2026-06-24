package com.blazemeter.jmeter.mcp.config;

import com.blazemeter.jmeter.mcp.client.McpClientRegistry;
import com.blazemeter.jmeter.mcp.client.McpClientSettings;
import com.blazemeter.jmeter.mcp.client.TransportType;
import com.blazemeter.jmeter.mcp.server.McpServerProcessManager;
import com.blazemeter.jmeter.mcp.util.Strings;
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
  public static final String KEEP_SERVER_RUNNING_AFTER_TEST =
      "McpClientConfig.keepServerRunningAfterTest";
  public static final String SERVER_LAUNCH_COMMAND = "McpClientConfig.serverLaunchCommand";
  public static final String SERVER_LAUNCH_ARGS = "McpClientConfig.serverLaunchArgs";
  public static final String SERVER_LAUNCH_ENV = "McpClientConfig.serverLaunchEnv";
  public static final String SERVER_READY_HOST = "McpClientConfig.serverReadyHost";
  public static final String SERVER_READY_PORT = "McpClientConfig.serverReadyPort";
  public static final String SERVER_STARTUP_WAIT_MS = "McpClientConfig.serverStartupWaitMs";

  private static final long serialVersionUID = 1L;

  private static final Logger LOG = LoggerFactory.getLogger(McpClientConfig.class);

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
    s.setKeepServerRunningAfterTest(
        getPropertyAsBoolean(KEEP_SERVER_RUNNING_AFTER_TEST, true));
    s.setServerLaunchCommand(getPropertyAsString(SERVER_LAUNCH_COMMAND, ""));
    s.setServerLaunchArgs(getPropertyAsString(SERVER_LAUNCH_ARGS, ""));
    s.setServerLaunchEnv(getPropertyAsString(SERVER_LAUNCH_ENV, ""));
    s.setServerReadyHost(getPropertyAsString(SERVER_READY_HOST, "localhost"));
    s.setServerReadyPort((int) getPropertyAsLong(SERVER_READY_PORT, 3001L));
    s.setServerStartupWaitMs(getPropertyAsLong(SERVER_STARTUP_WAIT_MS, 60_000L));
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

    if (settings.isKeepServerRunningAfterTest()) {
      LOG.info("Keeping MCP client '{}' running after test", registryName);
      return;
    }

    LOG.info("Stopping MCP client '{}'", registryName);
    boolean stopPreviewManagedServer =
        McpClientRegistry.getInstance().shouldStopPreviewManagedServer(registryName);
    McpClientRegistry.getInstance().remove(registryName);
    if (stopPreviewManagedServer) {
      McpServerProcessManager.getInstance().stop();
    }
  }
}
