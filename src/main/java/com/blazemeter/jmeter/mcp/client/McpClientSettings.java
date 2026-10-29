package com.blazemeter.jmeter.mcp.client;

import java.io.Serializable;
import org.apache.jmeter.engine.util.CompoundVariable;

/**
 * Plain value object holding the resolved configuration for an MCP client.
 *
 * <p>This is decoupled from the JMeter {@code TestElement} so it can be reused from samplers, unit
 * tests, or standalone tools.
 */
public final class McpClientSettings implements Serializable {

  private static final long serialVersionUID = 1L;

  private String name = "mcpClient";
  private TransportType transport = TransportType.STDIO;

  // HTTP-based transports
  private String serverUrl = "";
  private String endpoint = "";
  /**
   * Extra HTTP headers, one KEY=value per line. Filled from the referenced HTTP Header Manager,
   * or from legacy inline text when no manager is selected.
   */
  private String requestHeaders = "";

  // STDIO transport
  private String stdioCommand = "";
  private String stdioArgs = "";
  private String stdioEnv = "";

  // Client identity
  private String clientName = "jmeter-mcp-plugin";
  private String clientVersion = "1.0.0";

  // Timeouts (milliseconds)
  private long requestTimeoutMillis = 30_000L;
  private long initializationTimeoutMillis = 30_000L;

  /** When true, schedule connect during {@code testStarted()} instead of on first sampler use. */
  private boolean connectOnStartup = false;

  /** When true (default), keep managed MCP servers running after {@code testEnded()}. */
  private boolean keepServerRunningAfterTest = true;

  /** Optional subprocess launch for HTTP/SSE transports (GUI Start Now). */
  private String serverLaunchCommand = "";

  private String serverLaunchArgs = "";
  private String serverLaunchEnv = "";
  private String serverReadyHost = "localhost";
  private int serverReadyPort = 3001;
  private long serverStartupWaitMs = 60_000L;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public TransportType getTransport() {
    return transport;
  }

  public void setTransport(TransportType transport) {
    this.transport = transport;
  }

  public String getServerUrl() {
    return serverUrl;
  }

  public void setServerUrl(String serverUrl) {
    this.serverUrl = serverUrl;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public String getRequestHeaders() {
    return requestHeaders;
  }

  public void setRequestHeaders(String requestHeaders) {
    this.requestHeaders = requestHeaders;
  }

  public String getStdioCommand() {
    return stdioCommand;
  }

  public void setStdioCommand(String stdioCommand) {
    this.stdioCommand = stdioCommand;
  }

  public String getStdioArgs() {
    return stdioArgs;
  }

  public void setStdioArgs(String stdioArgs) {
    this.stdioArgs = stdioArgs;
  }

  public String getStdioEnv() {
    return stdioEnv;
  }

  public void setStdioEnv(String stdioEnv) {
    this.stdioEnv = stdioEnv;
  }

  public String getClientName() {
    return clientName;
  }

  public void setClientName(String clientName) {
    this.clientName = clientName;
  }

  public String getClientVersion() {
    return clientVersion;
  }

  public void setClientVersion(String clientVersion) {
    this.clientVersion = clientVersion;
  }

  public long getRequestTimeoutMillis() {
    return requestTimeoutMillis;
  }

  public void setRequestTimeoutMillis(long requestTimeoutMillis) {
    this.requestTimeoutMillis = requestTimeoutMillis;
  }

  public long getInitializationTimeoutMillis() {
    return initializationTimeoutMillis;
  }

  public void setInitializationTimeoutMillis(long initializationTimeoutMillis) {
    this.initializationTimeoutMillis = initializationTimeoutMillis;
  }

  public boolean isConnectOnStartup() {
    return connectOnStartup;
  }

  public void setConnectOnStartup(boolean connectOnStartup) {
    this.connectOnStartup = connectOnStartup;
  }

  public boolean isKeepServerRunningAfterTest() {
    return keepServerRunningAfterTest;
  }

  public void setKeepServerRunningAfterTest(boolean keepServerRunningAfterTest) {
    this.keepServerRunningAfterTest = keepServerRunningAfterTest;
  }

  public String getServerLaunchCommand() {
    return serverLaunchCommand;
  }

  public void setServerLaunchCommand(String serverLaunchCommand) {
    this.serverLaunchCommand = serverLaunchCommand;
  }

  public String getServerLaunchArgs() {
    return serverLaunchArgs;
  }

  public void setServerLaunchArgs(String serverLaunchArgs) {
    this.serverLaunchArgs = serverLaunchArgs;
  }

  public String getServerLaunchEnv() {
    return serverLaunchEnv;
  }

  public void setServerLaunchEnv(String serverLaunchEnv) {
    this.serverLaunchEnv = serverLaunchEnv;
  }

  public String getServerReadyHost() {
    return serverReadyHost;
  }

  public void setServerReadyHost(String serverReadyHost) {
    this.serverReadyHost = serverReadyHost;
  }

  public int getServerReadyPort() {
    return serverReadyPort;
  }

  public void setServerReadyPort(int serverReadyPort) {
    this.serverReadyPort = serverReadyPort;
  }

  public long getServerStartupWaitMs() {
    return serverStartupWaitMs;
  }

  public void setServerStartupWaitMs(long serverStartupWaitMs) {
    this.serverStartupWaitMs = serverStartupWaitMs;
  }

  /**
   * Copy with {@code ${mcp_bearer}} and {@code ${__P(...)}} expanded on the current thread.
   * CSV variables exist only after the thread starts, so callers do this at connect time.
   * This object stays unchanged so a failed attempt can retry with the next CSV row.
   */
  public McpClientSettings withPlaceholdersResolved() {
    McpClientSettings copy = copy();
    copy.resolvePlaceholders();
    return copy;
  }

  private McpClientSettings copy() {
    McpClientSettings copy = new McpClientSettings();
    copy.name = name;
    copy.transport = transport;
    copy.serverUrl = serverUrl;
    copy.endpoint = endpoint;
    copy.requestHeaders = requestHeaders;
    copy.stdioCommand = stdioCommand;
    copy.stdioArgs = stdioArgs;
    copy.stdioEnv = stdioEnv;
    copy.clientName = clientName;
    copy.clientVersion = clientVersion;
    copy.requestTimeoutMillis = requestTimeoutMillis;
    copy.initializationTimeoutMillis = initializationTimeoutMillis;
    copy.connectOnStartup = connectOnStartup;
    copy.keepServerRunningAfterTest = keepServerRunningAfterTest;
    copy.serverLaunchCommand = serverLaunchCommand;
    copy.serverLaunchArgs = serverLaunchArgs;
    copy.serverLaunchEnv = serverLaunchEnv;
    copy.serverReadyHost = serverReadyHost;
    copy.serverReadyPort = serverReadyPort;
    copy.serverStartupWaitMs = serverStartupWaitMs;
    return copy;
  }

  private void resolvePlaceholders() {
    serverUrl = resolve(serverUrl);
    endpoint = resolve(endpoint);
    requestHeaders = resolve(requestHeaders);
    stdioCommand = resolve(stdioCommand);
    stdioArgs = resolve(stdioArgs);
    stdioEnv = resolve(stdioEnv);
  }

  private static String resolve(String raw) {
    if (raw == null || raw.isEmpty() || !raw.contains("${")) {
      return raw == null ? "" : raw;
    }
    try {
      String resolved = new CompoundVariable(raw).execute();
      return resolved != null ? resolved : raw;
    } catch (Exception ex) {
      return raw;
    }
  }
}
