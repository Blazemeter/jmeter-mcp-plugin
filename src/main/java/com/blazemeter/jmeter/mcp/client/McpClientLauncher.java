package com.blazemeter.jmeter.mcp.client;

import java.net.URI;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blazemeter.jmeter.mcp.util.Strings;
import java.net.URI;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orchestrates GUI Connect / Stop on MCP Client Config via {@link McpClientRegistry}.
 */
public final class McpClientLauncher {

  private static final Logger LOG = LoggerFactory.getLogger(McpClientLauncher.class);

  private McpClientLauncher() {

    public static void connect(McpClientSettings settings) {
        Objects.requireNonNull(settings, "settings");
        McpClientRegistry.getInstance().connectNow(settings, false);
    }

    public static void disconnect(McpClientSettings settings) {
        Objects.requireNonNull(settings, "settings");
        McpClientRegistry.getInstance().disconnectNow(settings.getName());
    }
  }

  /**
   * @return {@code true} when this call started a managed subprocess
   */
  private static boolean ensureHttpServerReachable(McpClientSettings settings) {
    ReadyEndpoint ready = resolveReadyEndpoint(settings);
    if (McpServerProcessManager.isPortOpen(ready.host(), ready.port(), 500)) {
      return false;
    }
    String command = Strings.trimToDefault(settings.getServerLaunchCommand(), "").trim();
    if (command.isEmpty()) {
      throw new IllegalStateException(
          "MCP server is not reachable at "
              + ready.host()
              + ":"
              + ready.port()
              + ". Start it manually, add bzm - MCP Server Process to the test plan,"
              + " or fill in Server launch command on this config for Start Now.");
    }
    McpServerProcessManager.getInstance()
        .start(
            command,
            settings.getServerLaunchArgs(),
            settings.getServerLaunchEnv(),
            ready.host(),
            ready.port(),
            settings.getServerStartupWaitMs());
    return true;
  }

  static ReadyEndpoint resolveReadyEndpoint(McpClientSettings settings) {
    String host = Strings.trimToDefault(settings.getServerReadyHost(), "localhost");
    int port =
        settings.getServerReadyPort() > 0
            ? settings.getServerReadyPort()
            : portFromServerUrl(settings.getServerUrl(), 3001);
    return new ReadyEndpoint(host, port);
  }

  static int portFromServerUrl(String serverUrl, int defaultPort) {
    if (serverUrl == null || serverUrl.isBlank()) {
      return defaultPort;
    }
    String trimmed = serverUrl.trim();
    try {
      URI uri = URI.create(trimmed);
      if (uri.getPort() > 0) {
        return uri.getPort();
      }
      String scheme = uri.getScheme();
      if ("https".equalsIgnoreCase(scheme)) {
        return 443;
      }
      if ("http".equalsIgnoreCase(scheme)) {
        return 80;
      }
      LOG.warn(
          "Could not resolve port from MCP server URL '{}' (no explicit port and scheme is not"
              + " http/https), using default port {}",
          trimmed,
          defaultPort);
    } catch (IllegalArgumentException ex) {
      LOG.warn(
          "Could not parse MCP server URL '{}', using default port {}", trimmed, defaultPort, ex);
    }
    return defaultPort;
  }

  record ReadyEndpoint(String host, int port) {
  }
}
