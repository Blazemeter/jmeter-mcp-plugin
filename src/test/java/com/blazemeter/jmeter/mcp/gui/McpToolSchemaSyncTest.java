package com.blazemeter.jmeter.mcp.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolSchemaSyncTest {

    @Test
    void shouldFailWhenClientNotConnected() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> McpToolSchemaSync.loadFromConnectedClient(null, "cfg", "echo"));
        assertTrue(ex.getMessage().contains("cfg"));
    }
}
