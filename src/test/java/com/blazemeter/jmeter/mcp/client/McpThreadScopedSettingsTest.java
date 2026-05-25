package com.blazemeter.jmeter.mcp.client;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpThreadScopedSettingsTest {

    @Test
    void offsetsHttpUrlPortByThreadNum() {
        assertEquals("http://localhost:3002",
                McpThreadScopedSettings.offsetHttpUrl("http://localhost:3001", 1));
        assertEquals("http://localhost:3001",
                McpThreadScopedSettings.offsetHttpUrl("http://localhost:3001", 0));
    }

    @Test
    void serverPortForThreadAddsOffset() {
        assertEquals(3001, McpThreadScopedSettings.serverPortForThread(3001, 0));
        assertEquals(3004, McpThreadScopedSettings.serverPortForThread(3001, 3));
    }

    @Test
    void forThreadAdjustsHttpSettingsOnly() {
        McpClientSettings base = new McpClientSettings();
        base.setTransport(TransportType.SSE);
        base.setServerUrl("http://localhost:3001");

        McpClientSettings thread2 = McpThreadScopedSettings.forThread(base, 2);
        assertEquals("http://localhost:3003", thread2.getServerUrl());
        assertEquals(TransportType.SSE, thread2.getTransport());
    }

    @Test
    void serverEnvSetsPortWhenMissing() {
        Map<String, String> env = McpThreadScopedSettings.serverEnvForPort("# comment\nFOO=bar\n", 3010);
        assertEquals("3010", env.get("PORT"));
        assertEquals("bar", env.get("FOO"));
    }

    @Test
    void rejectsInvalidUrl() {
        assertThrows(IllegalArgumentException.class,
                () -> McpThreadScopedSettings.offsetHttpUrl("not a uri", 1));
    }
}
