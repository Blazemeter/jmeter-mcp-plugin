package com.blazemeter.jmeter.mcp.client;

import com.blazemeter.jmeter.mcp.server.McpServerProcessManager;
import com.blazemeter.jmeter.mcp.util.Strings;
import java.net.URI;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orchestrates GUI {@code Start Now} / {@code Stop Now} on MCP Client Config: optionally starts a
 * managed HTTP/SSE server, then connects or disconnects via {@link McpClientRegistry}.
 */
public final class McpClientLauncher {

  private static final Logger LOG = LoggerFactory.getLogger(McpClientLauncher.class);

  private McpClientLauncher() {

  }

  public static void startNow(McpClientSettings settings) {
    Objects.requireNonNull(settings, "settings");
    boolean startedManagedServer = false;
    if (settings.getTransport() != TransportType.STDIO) {
      startedManagedServer = ensureHttpServerReachable(settings);
    }
    try {
      McpClientRegistry.getInstance().connectNow(settings, startedManagedServer);
    } catch (RuntimeException ex) {
      if (startedManagedServer) {
        McpServerProcessManager.getInstance().stop();
      }
      throw ex;
    }
  }

  public static void stopNow(McpClientSettings settings) {
    Objects.requireNonNull(settings, "settings");
    String clientName = settings.getName();
    McpClientRegistry registry = McpClientRegistry.getInstance();
    boolean stopManagedServer = registry.disconnectNow(clientName);
    if (stopManagedServer && settings.getTransport() != TransportType.STDIO) {
      McpServerProcessManager.getInstance().stop();
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
