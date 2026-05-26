package com.blazemeter.jmeter.mcp.sampler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class McpOperationTest {

    @Test
    void fromStringDefaultsToPingWhenBlank() {
        assertEquals(McpOperation.PING, McpOperation.fromString(null));
        assertEquals(McpOperation.PING, McpOperation.fromString(""));
        assertEquals(McpOperation.PING, McpOperation.fromString("   "));
    }

    @Test
    void fromStringAcceptsHyphenatedNames() {
        assertEquals(McpOperation.CALL_TOOL, McpOperation.fromString("call-tool"));
        assertEquals(McpOperation.LIST_TOOLS, McpOperation.fromString("list_tools"));
    }

    @Test
    void fromStringRejectsUnknownOperation() {
        assertThrows(IllegalArgumentException.class,
                () -> McpOperation.fromString("unknown"));
    }
}
