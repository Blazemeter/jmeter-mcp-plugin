package com.blazemeter.jmeter.mcp.gui;

import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import com.blazemeter.jmeter.mcp.JMeterTestUtils;
import com.blazemeter.jmeter.mcp.client.ToolArgumentsSchemaSupport.ValidationResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolArgumentsEditorTest {

    @BeforeAll
    static void setupJmeter() {
        JMeterTestUtils.setupJmeterEnv();
    }

    @Test
    void shouldToggleCallToolExtrasWhenModeChanges() throws Exception {
        ToolArgumentsEditor editor = onEdt(ToolArgumentsEditor::new);
        onEdtVoid(() -> {
            editor.setCallToolMode(true);
            assertTrue(editor.isCallToolExtrasVisible());
            editor.setCallToolMode(false);
            assertFalse(editor.isCallToolExtrasVisible());
        });
    }

    @Test
    void shouldGenerateAndValidateWhenSchemaAppliedInEditor() throws Exception {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of("message", Map.of("type", "string")),
                "required", List.of("message"));

        ToolArgumentsEditor editor = onEdt(ToolArgumentsEditor::new);
        onEdtVoid(() -> {
            editor.setCallToolMode(true);
            editor.applyLoadedSchema(schema, "{\"type\":\"object\"}");
            editor.runGenerateSample();
        });

        String generated = onEdt(editor::getArgumentsText);
        assertTrue(generated.contains("message"));

        ValidationResult valid = onEdt(editor::runValidateArguments);
        assertTrue(valid.valid());

        onEdtVoid(() -> editor.setArgumentsText("{}"));
        ValidationResult invalid = onEdt(editor::runValidateArguments);
        assertFalse(invalid.valid());
    }

    @Test
    void shouldNormalizeNullArgumentsText() throws Exception {
        ToolArgumentsEditor editor = onEdt(ToolArgumentsEditor::new);
        onEdtVoid(() -> editor.setArgumentsText(null));
        assertEquals("", onEdt(editor::getArgumentsText));
    }

    @Test
    void shouldReportValidationMessageWhenSchemaNotLoaded() throws Exception {
        ToolArgumentsEditor editor = onEdt(ToolArgumentsEditor::new);
        ValidationResult result = onEdt(editor::runValidateArguments);
        assertFalse(result.valid());
        assertTrue(result.message().contains("No input schema"));
    }

    @Test
    void shouldClearArgumentsAndSchemaWhenClearCalled() throws Exception {
        ToolArgumentsEditor editor = onEdt(ToolArgumentsEditor::new);
        onEdtVoid(() -> {
            editor.setArgumentsText("{\"a\":1}");
            editor.applyLoadedSchema(Map.of("type", "object"), "{}");
            editor.clear();
        });

        assertEquals("", onEdt(editor::getArgumentsText));
        ValidationResult result = onEdt(editor::runValidateArguments);
        assertFalse(result.valid());
    }

    private static void onEdtVoid(EdtRunnable action) throws Exception {
        onEdt(() -> {
            action.run();
            return null;
        });
    }

    private static <T> T onEdt(EdtSupplier<T> action) throws Exception {
        final Object[] holder = new Object[1];
        final Exception[] error = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                holder[0] = action.get();
            } catch (Exception ex) {
                error[0] = ex;
            }
        });
        if (error[0] != null) {
            throw error[0];
        }
        @SuppressWarnings("unchecked")
        T value = (T) holder[0];
        return value;
    }

    @FunctionalInterface
    private interface EdtSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    private interface EdtRunnable {
        void run() throws Exception;
    }
}
