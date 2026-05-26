package com.blazemeter.jmeter.mcp.client;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpClientFactoryTest {

    @Test
    void splitsSimpleArgs() {
        assertEquals(List.of("-y", "@modelcontextprotocol/server-everything", "dir"),
                McpClientFactory.splitArgs("-y @modelcontextprotocol/server-everything dir"));
    }

    @Test
    void splitsQuotedArgs() {
        assertEquals(List.of("--message", "hello world", "--other", "v"),
                McpClientFactory.splitArgs("--message \"hello world\" --other v"));
    }

    @Test
    void returnsEmptyForBlankArgs() {
        assertTrue(McpClientFactory.splitArgs("   ").isEmpty());
        assertTrue(McpClientFactory.splitArgs(null).isEmpty());
    }

    @Test
    void trimsTrailingWhitespaceInArgs() {
        assertEquals(List.of("--mcp"),
                McpClientFactory.splitArgs("--mcp "));
    }

    @Test
    void parsesEnvLines() {
        Map<String, String> env = McpClientFactory.parseEnv(
                "FOO=bar\n# comment\nBAZ = qux\n\nEMPTY=");
        assertEquals(3, env.size());
        assertEquals("bar", env.get("FOO"));
        assertEquals("qux", env.get("BAZ"));
        assertEquals("", env.get("EMPTY"));
    }

    @Test
    void buildAndInitializeRejectsNullSettings() {
        assertThrows(NullPointerException.class,
                () -> McpClientFactory.buildAndInitialize(null));
    }

    @Test
    void stdioRejectsBlankCommandAfterTrim() {
        McpClientSettings s = new McpClientSettings();
        s.setTransport(TransportType.STDIO);
        s.setStdioCommand("   ");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> McpClientFactory.buildAndInitialize(s));
        assertTrue(ex.getMessage().contains("command"));
    }

    @Test
    void stdioRejectsNullCommand() {
        McpClientSettings s = new McpClientSettings();
        s.setTransport(TransportType.STDIO);
        s.setStdioCommand(null);
        assertThrows(IllegalArgumentException.class,
                () -> McpClientFactory.buildAndInitialize(s));
    }

    @Test
    void sseRejectsMissingServerUrl() {
        McpClientSettings s = new McpClientSettings();
        s.setTransport(TransportType.SSE);
        s.setServerUrl("  ");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> McpClientFactory.buildAndInitialize(s));
        assertTrue(ex.getMessage().contains("SSE"));
        assertTrue(ex.getMessage().contains("server URL"));
    }

    @Test
    void streamableHttpRejectsMissingServerUrl() {
        McpClientSettings s = new McpClientSettings();
        s.setTransport(TransportType.STREAMABLE_HTTP);
        s.setServerUrl(null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> McpClientFactory.buildAndInitialize(s));
        assertTrue(ex.getMessage().contains("Streamable HTTP"));
        assertTrue(ex.getMessage().contains("server URL"));
    }
}
