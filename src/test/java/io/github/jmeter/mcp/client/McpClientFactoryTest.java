package io.github.jmeter.mcp.client;

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
    void transportFromStringDefaultsToStdioWhenBlank() {
        assertEquals(TransportType.STDIO, TransportType.fromString(null));
        assertEquals(TransportType.STDIO, TransportType.fromString(""));
        assertEquals(TransportType.STDIO, TransportType.fromString("   "));
    }

    @Test
    void transportFromStringIsCaseInsensitive() {
        assertEquals(TransportType.STDIO, TransportType.fromString("stdio"));
        assertEquals(TransportType.SSE, TransportType.fromString("SSE"));
        assertEquals(TransportType.STREAMABLE_HTTP,
                TransportType.fromString("streamable-http"));
    }

    @Test
    void transportFromStringRejectsGarbage() {
        assertThrows(IllegalArgumentException.class,
                () -> TransportType.fromString("websocket"));
    }

    @Test
    void stdioRejectsBlankCommandAfterTrim() {
        McpClientSettings s = new McpClientSettings();
        s.setTransport(TransportType.STDIO);
        s.setStdioCommand("   ");
        assertThrows(IllegalArgumentException.class,
                () -> McpClientFactory.buildAndInitialize(s));
    }
}
