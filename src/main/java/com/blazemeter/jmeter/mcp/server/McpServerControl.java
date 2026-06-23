package com.blazemeter.jmeter.mcp.server;

/**
 * Facade for GUI manual start/stop and server reachability checks.
 */
public interface McpServerControl {

  void start(McpServerLaunchSettings settings);

  void stop();

  Long getManagedProcessPid();

  boolean isPortOpen(String host, int port, int timeoutMs);
}
