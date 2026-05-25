package com.blazemeter.jmeter.mcp.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpClientRegistryTest {

    @AfterEach
    void tearDown() {
        McpClientRegistry.getInstance().removeAllForClientName("testClient");
    }

    @Test
    void slotKeyCombinesNameAndThread() {
        assertEquals("myClient::Thread Group 1-1",
                McpClientRegistry.slotKey("myClient", "Thread Group 1-1"));
    }

    @Test
    void removeAllForClientNameClearsDeferredRegistration() {
        McpClientRegistry registry = McpClientRegistry.getInstance();
        McpClientSettings settings = new McpClientSettings();
        settings.setName("testClient");
        settings.setTransport(TransportType.STDIO);

        registry.registerDeferred("testClient", settings);
        registry.removeAllForClientName("testClient");

        assertNull(registry.getOrConnect("testClient"));
    }

    @Test
    void workerSlotDiffersFromMainThreadSlot() {
        assertEquals("client::" + McpJmeterThreads.MAIN_THREAD_KEY,
                McpClientRegistry.slotKey("client", McpJmeterThreads.MAIN_THREAD_KEY));
        assertEquals("client::Thread Group 1-1",
                McpClientRegistry.slotKey("client", "Thread Group 1-1"));
    }

    @Test
    void rejectsBlankClientName() {
        McpClientSettings settings = new McpClientSettings();
        assertThrows(IllegalArgumentException.class,
                () -> McpClientRegistry.getInstance().registerDeferred("  ", settings));
    }
}
