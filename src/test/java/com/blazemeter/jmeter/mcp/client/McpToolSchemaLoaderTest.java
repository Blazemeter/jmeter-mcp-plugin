package com.blazemeter.jmeter.mcp.client;

import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolSchemaLoaderTest {

    @Test
    void shouldLoadSchemaWhenToolExists() throws Exception {
        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(
                "object",
                Map.of("message", Map.of("type", "string")),
                List.of("message"),
                false,
                null,
                null);
        McpSchema.Tool echo = new McpSchema.Tool("echo", null, null, inputSchema, null, null, null);

        McpToolSchemaLoader.LoadedSchema loaded = McpToolSchemaLoader.load(
                cursor -> new McpSchema.ListToolsResult(List.of(echo), null, null),
                "echo");

        assertTrue(loaded.prettySchemaJson().contains("message"));
        assertEquals("object", loaded.schemaMap().get("type"));
    }

    @Test
    void shouldFailWhenToolMissing() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> McpToolSchemaLoader.load(
                        cursor -> new McpSchema.ListToolsResult(List.of(), null, null),
                        "missing"));
        assertTrue(ex.getMessage().contains("missing"));
    }
}
