package com.blazemeter.jmeter.mcp.client;

import java.util.concurrent.TimeUnit;

/** Shared settings builders and environment checks for MCP client tests. */
final class McpClientTestFixtures {

  static final String SERVER_EVERYTHING_ARGS = "-y @modelcontextprotocol/server-everything";
  static final long LIVE_SERVER_TIMEOUT_MS = 120_000L;

  private McpClientTestFixtures() {

  }
  static McpClientSettings stdioServerEverythingSettings(String name) {
    McpClientSettings settings = new McpClientSettings();
    settings.setName(name);
    settings.setTransport(TransportType.STDIO);
    settings.setStdioCommand("npx");
    settings.setStdioArgs(SERVER_EVERYTHING_ARGS);
    settings.setRequestTimeoutMillis(LIVE_SERVER_TIMEOUT_MS);
    settings.setInitializationTimeoutMillis(LIVE_SERVER_TIMEOUT_MS);
    return settings;
  }

  static McpClientSettings sseServerEverythingSettings(String name, int port) {
    McpClientSettings settings = new McpClientSettings();
    settings.setName(name);
    settings.setTransport(TransportType.SSE);
    settings.setServerUrl("http://127.0.0.1:" + port);
    settings.setRequestTimeoutMillis(LIVE_SERVER_TIMEOUT_MS);
    settings.setInitializationTimeoutMillis(LIVE_SERVER_TIMEOUT_MS);
    return settings;
  }

  static McpClientSettings streamableHttpServerEverythingSettings(String name, int port) {
    McpClientSettings settings = new McpClientSettings();
    settings.setName(name);
    settings.setTransport(TransportType.STREAMABLE_HTTP);
    settings.setServerUrl("http://127.0.0.1:" + port);
    settings.setRequestTimeoutMillis(LIVE_SERVER_TIMEOUT_MS);
    settings.setInitializationTimeoutMillis(LIVE_SERVER_TIMEOUT_MS);
    return settings;
  }

  static boolean isNpxAvailable() {
    try {
      Process process = new ProcessBuilder("npx", "--version").redirectErrorStream(true).start();
      return process.waitFor(15, TimeUnit.SECONDS) && process.exitValue() == 0;
    } catch (Exception ex) {
      return false;
    }
  }
}
