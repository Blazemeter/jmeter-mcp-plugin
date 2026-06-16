package com.blazemeter.jmeter.mcp.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpServerProcessManagerTest {

    @AfterEach
    void tearDown() {
        McpServerProcessManager.getInstance().stop();
    }

    @Test
    void shouldRejectBlankCommandWhenStart() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> McpServerProcessManager.getInstance().start(
                        "  ", "", "", "localhost", 3001, 1000));
        assertTrue(ex.getMessage().contains("command must not be empty"));
    }

    @Test
    void shouldRejectNullCommandWhenStart() {
        assertThrows(IllegalArgumentException.class,
                () -> McpServerProcessManager.getInstance().start(
                        null, "", "", "localhost", 3001, 1000));
    }

    @Test
    void shouldFailWhenExecutableDoesNotExist() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> McpServerProcessManager.getInstance().start(
                        "/no-such-mcp-server-cmd-xyzzy", "-bad-arg", "",
                        "127.0.0.1", 31998, 500));
        assertTrue(ex.getMessage().contains("Failed to start MCP server process"));
    }

    @Test
    void shouldFailWhenReadyPortNeverOpens() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> McpServerProcessManager.getInstance().start(
                        "/usr/bin/false", "", "", "127.0.0.1", 31997, 800));
        assertTrue(ex.getMessage().contains("did not become reachable"));
    }

    @Test
    void shouldReuseListenerWhenReadyPortAlreadyOpen() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            int port = socket.getLocalPort();
            McpServerProcessManager manager = McpServerProcessManager.getInstance();
            manager.start("npx", "-y pkg", "", "127.0.0.1", port, 500);
            assertFalse(manager.isManagedProcessRunning());
        }
    }

    @Test
    void shutdownAllStopsManagedProcess() {
        McpServerProcessManager manager = McpServerProcessManager.getInstance();
        manager.shutdownAll();
        assertFalse(manager.isManagedProcessRunning());
    }
}
