package io.github.jmeter.mcp.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpServerProcessManagerTest {

    @Test
    void rejectsBlankCommand() {
        assertThrows(IllegalArgumentException.class,
                () -> McpServerProcessManager.getInstance().start(
                        "  ", "", "", "localhost", 3001, 1000));
    }

    @Test
    void stopIsIdempotentWhenNoProcess() {
        McpServerProcessManager manager = McpServerProcessManager.getInstance();
        manager.stop();
        assertFalse(manager.isManagedProcessRunning());
    }
}
