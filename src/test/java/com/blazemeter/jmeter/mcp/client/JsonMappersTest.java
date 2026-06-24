package com.blazemeter.jmeter.mcp.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.modelcontextprotocol.json.McpJsonMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

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
