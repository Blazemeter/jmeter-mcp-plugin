package com.blazemeter.jmeter.mcp.gui;

import java.util.List;
import java.util.Map;

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
        ToolArgumentsEditor editor = GuiEdtTestSupport.onEdt(ToolArgumentsEditor::new);
        GuiEdtTestSupport.onEdtVoid(() -> {
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

        ToolArgumentsEditor editor = GuiEdtTestSupport.onEdt(ToolArgumentsEditor::new);
        GuiEdtTestSupport.onEdtVoid(() -> {
            editor.setCallToolMode(true);
            editor.applyLoadedSchema(schema, "{\"type\":\"object\"}");
            editor.runGenerateSample();
        });

        String generated = GuiEdtTestSupport.onEdt(editor::getArgumentsText);
        assertTrue(generated.contains("message"));

        ValidationResult valid = GuiEdtTestSupport.onEdt(editor::runValidateArguments);
        assertTrue(valid.valid());

        GuiEdtTestSupport.onEdtVoid(() -> editor.setArgumentsText("{}"));
        ValidationResult invalid = GuiEdtTestSupport.onEdt(editor::runValidateArguments);
        assertFalse(invalid.valid());
    }

    @Test
    void shouldNormalizeNullArgumentsText() throws Exception {
        ToolArgumentsEditor editor = GuiEdtTestSupport.onEdt(ToolArgumentsEditor::new);
        GuiEdtTestSupport.onEdtVoid(() -> editor.setArgumentsText(null));
        assertEquals("", GuiEdtTestSupport.onEdt(editor::getArgumentsText));
    }

    @Test
    void shouldReportValidationMessageWhenSchemaNotLoaded() throws Exception {
        ToolArgumentsEditor editor = GuiEdtTestSupport.onEdt(ToolArgumentsEditor::new);
        ValidationResult result = GuiEdtTestSupport.onEdt(editor::runValidateArguments);
        assertFalse(result.valid());
        assertTrue(result.message().contains("No input schema"));
    }

    @Test
    void shouldClearArgumentsAndSchemaWhenClearCalled() throws Exception {
        ToolArgumentsEditor editor = GuiEdtTestSupport.onEdt(ToolArgumentsEditor::new);
        GuiEdtTestSupport.onEdtVoid(() -> {
            editor.setArgumentsText("{\"a\":1}");
            editor.applyLoadedSchema(Map.of("type", "object"), "{}");
            editor.clear();
        });

        assertEquals("", GuiEdtTestSupport.onEdt(editor::getArgumentsText));
        ValidationResult result = GuiEdtTestSupport.onEdt(editor::runValidateArguments);
        assertFalse(result.valid());
    }

    @Test
    void shouldKeepDefaultSuppliersWhenSettersCalledWithNull() throws Exception {
        ToolArgumentsEditor editor = GuiEdtTestSupport.onEdt(ToolArgumentsEditor::new);
        GuiEdtTestSupport.onEdtVoid(() -> {
            editor.setConfigNameSupplier(null);
            editor.setToolNameSupplier(null);
            editor.setConfigNameSupplier(() -> "custom-client");
            editor.setToolNameSupplier(() -> "echo");
        });

        ValidationResult result = GuiEdtTestSupport.onEdt(editor::runValidateArguments);
        assertFalse(result.valid());
        assertTrue(result.message().contains("No input schema"));
    }
}
