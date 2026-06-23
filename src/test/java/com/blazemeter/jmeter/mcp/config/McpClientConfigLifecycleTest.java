package com.blazemeter.jmeter.mcp.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.blazemeter.jmeter.mcp.client.McpClientSettings;
import com.blazemeter.jmeter.mcp.client.TransportType;
import com.blazemeter.jmeter.mcp.server.McpServerProcessManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class McpClientConfigLifecycleTest {

    @AfterEach
    void tearDown() {
        McpServerProcessManager.getInstance().stop();
    }

    @Test
    void shouldNotStopManagedServerWhenClientTestEndsWithoutPreviewServer() throws Exception {
        Process sleeper = new ProcessBuilder("/bin/sleep", "60").start();
        try {
            McpServerProcessManager manager = McpServerProcessManager.getInstance();
            java.lang.reflect.Field processField =
                    McpServerProcessManager.class.getDeclaredField("process");
            processField.setAccessible(true);
            processField.set(manager, sleeper);

            McpClientConfig config = new McpClientConfig();
            config.setProperty(McpClientConfig.NAME, "mcpClient");
            config.setProperty(McpClientConfig.TRANSPORT, TransportType.STREAMABLE_HTTP.name());
            config.setProperty(McpClientConfig.KEEP_SERVER_RUNNING_AFTER_TEST, false);

            config.testEnded();

            assertTrue(sleeper.isAlive(), "MCP Server Process keep is independent of client config");
        } finally {
            sleeper.destroyForcibly();
            McpServerProcessManager.getInstance().stop();
        }
    }
}
