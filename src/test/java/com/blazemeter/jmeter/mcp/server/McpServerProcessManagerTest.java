package com.blazemeter.jmeter.mcp.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpServerProcessManagerTest {

    @AfterEach
    void tearDown() {
        McpServerProcessManager.getInstance().stop();
    }

    @Test
    void rejectsBlankCommand() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> McpServerProcessManager.getInstance().start(
                        "  ", "", "", "localhost", 3001, 1000));
        assertTrue(ex.getMessage().contains("command must not be empty"));
    }

    @Test
    void rejectsNullCommand() {
        assertThrows(IllegalArgumentException.class,
                () -> McpServerProcessManager.getInstance().start(
                        null, "", "", "localhost", 3001, 1000));
    }

    @Test
    void rejectsNonexistentExecutable() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> McpServerProcessManager.getInstance().start(
                        "/no-such-mcp-server-cmd-xyzzy", "-bad-arg", "",
                        "localhost", 3001, 500));
        assertTrue(ex.getMessage().contains("Failed to start MCP server process"));
    }

    @Test
    void rejectsWhenReadyPortNeverOpens() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> McpServerProcessManager.getInstance().start(
                        "/usr/bin/false", "", "", "127.0.0.1", 31999, 800));
        assertTrue(ex.getMessage().contains("did not become reachable"));
    }
}
