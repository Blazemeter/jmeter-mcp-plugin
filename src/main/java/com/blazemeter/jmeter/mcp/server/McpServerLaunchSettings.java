package com.blazemeter.jmeter.mcp.server;

/**
 * Launch parameters for {@link McpServerProcessManager#start(String, String, String, String, int, long)}.
 */
public record McpServerLaunchSettings(
        String command,
        String args,
        String env,
        String readyHost,
        int readyPort,
        long startupWaitMs) {
}
