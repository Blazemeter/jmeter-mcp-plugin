package com.blazemeter.jmeter.mcp.gui;

import java.util.concurrent.atomic.AtomicReference;

import com.blazemeter.jmeter.mcp.client.McpToolSchemaLoader.LoadedSchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    void shouldNotifyListenerWhenLoadAsyncFailsWithoutPreviewClient() throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();

        McpToolSchemaSync.loadAsync("missing-preview", "echo", new McpToolSchemaSync.SchemaLoadListener() {
            @Override
            public void onSuccess(LoadedSchema schema) {
                throw new AssertionError("Expected failure without preview client");
            }

            @Override
            public void onFailure(Throwable cause) {
                error.set(cause);
            }
        });

        GuiEdtTestSupport.awaitCondition(() -> error.get() != null, 10);
        assertNotNull(error.get());
        assertTrue(error.get().getMessage().contains("missing-preview"));
    }
}
