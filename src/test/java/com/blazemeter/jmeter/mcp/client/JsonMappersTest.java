package com.blazemeter.jmeter.mcp.client;

import java.util.Map;

import io.modelcontextprotocol.json.McpJsonMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonMappersTest {

    @Test
    void shouldReturnCachedMapperWhenGetDefaultCalledTwice() {
        McpJsonMapper mapper = JsonMappers.getDefault();
        assertNotNull(mapper);
        // second call uses cached instance
        assertEquals(mapper, JsonMappers.getDefault());
    }

    @Test
    void shouldReturnNullLiteralWhenWriteValueAsPrettyStringWithNull() throws Exception {
        assertEquals("null", JsonMappers.writeValueAsPrettyString(null));
    }

    @Test
    void shouldSerializeMapWhenWriteValueAsPrettyString() throws Exception {
        String json = JsonMappers.writeValueAsPrettyString(Map.of("key", "value"));
        assertTrue(json.contains("key"));
        assertTrue(json.contains("value"));
    }
}
