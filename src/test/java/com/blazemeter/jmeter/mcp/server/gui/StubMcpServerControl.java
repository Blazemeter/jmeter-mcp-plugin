package com.blazemeter.jmeter.mcp.server.gui;

import com.blazemeter.jmeter.mcp.server.McpServerControl;
import com.blazemeter.jmeter.mcp.server.McpServerLaunchSettings;

final class StubMcpServerControl implements McpServerControl {

    McpServerLaunchSettings lastStart;
    boolean stopCalled;
    Long managedPid;
    boolean portOpen;
    RuntimeException startFailure;

    @Override
    public void start(McpServerLaunchSettings settings) {
        if (startFailure != null) {
            throw startFailure;
        }
        lastStart = settings;
        if (managedPid == null) {
            managedPid = 42L;
        }
    }

    @Override
    public void stop() {
        stopCalled = true;
        managedPid = null;
    }

    @Override
    public Long getManagedProcessPid() {
        return managedPid;
    }

    @Override
    public boolean isPortOpen(String host, int port, int timeoutMs) {
        return portOpen;
    }
}
