package com.blazemeter.jmeter.mcp.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpPreviewClientsTest {

    @Test
    void shouldFailWhenPreviewClientIsNotConnected() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> McpPreviewClients.requireConnected("my-config", null));

        assertTrue(ex.getMessage().contains("my-config"));
        assertTrue(ex.getMessage().contains("Start Now"));
    }
}
