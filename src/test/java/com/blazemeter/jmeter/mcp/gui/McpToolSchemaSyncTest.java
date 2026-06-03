package com.blazemeter.jmeter.mcp.gui;

import java.util.Map;

import com.blazemeter.jmeter.mcp.client.McpToolSchemaLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolSchemaSyncTest {

    @Test
    void shouldFailWhenClientNotConnected() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> McpToolSchemaSync.loadFromConnectedClient(null, "cfg", "echo"));
        assertTrue(ex.getMessage().contains("cfg"));
    }

    @Test
    void shouldMapLoaderResultToGuiRecord() {
        McpToolSchemaLoader.LoadedSchema loader = new McpToolSchemaLoader.LoadedSchema(
                Map.of("type", "object"), "{\n  \"type\": \"object\"\n}");

        McpToolSchemaSync.LoadedSchema gui = McpToolSchemaSync.LoadedSchema.from(loader);

        assertEquals("object", gui.schemaMap().get("type"));
        assertTrue(gui.prettySchemaJson().contains("object"));
    }
}
