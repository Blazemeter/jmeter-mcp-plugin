package com.blazemeter.jmeter.mcp;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.blazemeter.jmeter.mcp.server.McpServerProcessManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class McpRuntimeCleanupTest {

    @AfterEach
    void tearDown() {
        McpServerProcessManager.getInstance().stop();
    }

    @Test
    void shutdownAllStopsManagedServerProcess() throws Exception {
        Process sleeper = TestProcesses.startSleeper();
        try {
            McpServerProcessManager manager = McpServerProcessManager.getInstance();
            java.lang.reflect.Field processField =
                    McpServerProcessManager.class.getDeclaredField("process");
            processField.setAccessible(true);
            processField.set(manager, sleeper);

            McpRuntimeCleanup.shutdownAll();

            assertFalse(sleeper.isAlive());
            assertFalse(manager.isManagedServerAlive());
        } finally {
            if (sleeper.isAlive()) {
                sleeper.destroyForcibly();
            }
            McpServerProcessManager.getInstance().stop();
        }
    }
}
