package com.blazemeter.jmeter.mcp.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpClientRegistryTest {

    private static final String CLIENT = "exception-test-client";

    @AfterEach
    void tearDown() {
        McpClientRegistry.getInstance().remove(CLIENT);
    }

    @Test
    void registerDeferredRejectsBlankName() {
        McpClientSettings settings = new McpClientSettings();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> McpClientRegistry.getInstance().registerDeferred("  ", settings));
        assertTrue(ex.getMessage().contains("name must not be empty"));
    }

    @Test
    void connectOnStartupRejectsNullName() {
        McpClientSettings settings = new McpClientSettings();
        assertThrows(IllegalArgumentException.class,
                () -> McpClientRegistry.getInstance().connectOnStartup(null, settings));
    }

    @Test
    void getOrConnectReturnsNullWhenClientNotRegistered() {
        assertNull(McpClientRegistry.getInstance().getOrConnect("unregistered-client"));
    }

    @Test
    void getOrConnectReturnsNullForBlankName() {
        assertNull(McpClientRegistry.getInstance().getOrConnect("   "));
    }

    @Test
    void getOrConnectPropagatesInvalidStdioSettings() {
        McpClientSettings settings = new McpClientSettings();
        settings.setName(CLIENT);
        settings.setTransport(TransportType.STDIO);
        settings.setStdioCommand("");
        McpClientRegistry.getInstance().registerDeferred(CLIENT, settings);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> McpClientRegistry.getInstance().getOrConnect(CLIENT));
        assertTrue(ex.getMessage().contains("command"));
    }
}
