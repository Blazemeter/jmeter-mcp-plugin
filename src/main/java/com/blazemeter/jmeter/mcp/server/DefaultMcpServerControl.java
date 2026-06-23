package com.blazemeter.jmeter.mcp.server;

import java.util.Objects;

/**
 * Default {@link McpServerControl} delegating to {@link McpServerProcessManager}.
 */
public final class DefaultMcpServerControl implements McpServerControl {

  private static final McpServerControl INSTANCE = new DefaultMcpServerControl();

  private DefaultMcpServerControl() {
  }

  public static McpServerControl getInstance() {
    return INSTANCE;
  }

  @Override
  public void start(McpServerLaunchSettings settings) {
    Objects.requireNonNull(settings, "settings");
    McpServerProcessManager.getInstance().start(
        settings.command(),
        settings.args(),
        settings.env(),
        settings.readyHost(),
        settings.readyPort(),
        settings.startupWaitMs());
  }

  @Override
  public void stop() {
    McpServerProcessManager.getInstance().stop();
  }

  @Override
  public Long getManagedProcessPid() {
    return McpServerProcessManager.getInstance().getManagedProcessPid();
  }

  @Override
  public boolean isPortOpen(String host, int port, int timeoutMs) {
    return McpServerProcessManager.isPortOpen(host, port, timeoutMs);
  }
}
