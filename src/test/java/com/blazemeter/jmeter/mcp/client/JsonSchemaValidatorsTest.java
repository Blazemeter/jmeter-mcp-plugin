package com.blazemeter.jmeter.mcp.client;

import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class JsonSchemaValidatorsTest {

    @Test
    void shouldReturnCachedValidatorWhenGetDefaultCalledTwice() {
        JsonSchemaValidator first = JsonSchemaValidators.getDefault();
        assertNotNull(first);
        assertSame(first, JsonSchemaValidators.getDefault());
    }
}
