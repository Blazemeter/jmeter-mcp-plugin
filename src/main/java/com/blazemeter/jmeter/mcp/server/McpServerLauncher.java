package com.blazemeter.jmeter.mcp.server;

import java.util.Objects;

/**
 * Orchestrates GUI Start / Stop on MCP Server Process via {@link McpServerProcessManager}.
 */
public final class McpServerLauncher {

    private McpServerLauncher() {
    }

    public static void start(McpServerLaunchSettings settings) {
        Objects.requireNonNull(settings, "settings");
        McpServerProcessManager.getInstance().start(
                settings.command(),
                settings.args(),
                settings.env(),
                settings.readyHost(),
                settings.readyPort(),
                settings.startupWaitMs());
    }

    public static void stop() {
        McpServerProcessManager.getInstance().stop();
    }

    public static boolean isManagedServerRunning() {
        return McpServerProcessManager.getInstance().isManagedServerAlive();
    }
}
