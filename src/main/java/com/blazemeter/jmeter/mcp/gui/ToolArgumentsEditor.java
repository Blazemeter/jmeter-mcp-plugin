package com.blazemeter.jmeter.mcp.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.blazemeter.jmeter.mcp.client.McpToolSchemaLoader.LoadedSchema;
import com.blazemeter.jmeter.mcp.client.ToolArgumentsSchemaSupport;
import com.blazemeter.jmeter.mcp.client.ToolArgumentsSchemaSupport.ValidationResult;
import com.blazemeter.jmeter.mcp.util.Strings;
import org.apache.jmeter.gui.util.JSyntaxTextArea;
import org.apache.jmeter.gui.util.JTextScrollPane;

/**
 * JSON arguments editor for {@code CALL_TOOL} with optional input-schema preview,
 * sample generation, and validation.
 */
public final class ToolArgumentsEditor extends JPanel {

    private final JSyntaxTextArea argumentsArea = JSyntaxTextArea.getInstance(8, 60);
    private final JTextArea schemaPreviewArea = new JTextArea(4, 60);
    private final JLabel statusLabel = new JLabel(" ");
    private final JPanel schemaPanel = new JPanel(new BorderLayout(0, 4));
    private final JPanel callToolExtras = new JPanel(new BorderLayout(0, 4));
    private final JPanel toolActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    private final JButton loadSchemaButton = new JButton("Load schema");
    private final JButton generateButton = new JButton("Generate sample");
    private final JButton validateButton = new JButton("Validate");

    private Supplier<String> configNameSupplier = () -> "mcpClient";
    private Supplier<String> toolNameSupplier = () -> "";

    private Map<String, Object> loadedSchema;

    public ToolArgumentsEditor() {
        super(new BorderLayout(0, 5));
        setBorder(BorderFactory.createTitledBorder("Arguments (JSON object)"));

        schemaPreviewArea.setEditable(false);
        schemaPreviewArea.setLineWrap(true);
        schemaPreviewArea.setWrapStyleWord(true);
        schemaPreviewArea.setName("mcpSampler.toolInputSchema");

        toolActions.add(loadSchemaButton);
        toolActions.add(generateButton);
        toolActions.add(validateButton);

        schemaPanel.setBorder(BorderFactory.createTitledBorder("Tool input schema (read-only)"));
        schemaPanel.add(new javax.swing.JScrollPane(schemaPreviewArea), BorderLayout.CENTER);

        callToolExtras.add(toolActions, BorderLayout.NORTH);
        callToolExtras.add(schemaPanel, BorderLayout.CENTER);
        callToolExtras.add(statusLabel, BorderLayout.SOUTH);

        argumentsArea.setName("mcpSampler.arguments");
        add(JTextScrollPane.getInstance(argumentsArea), BorderLayout.CENTER);
        add(callToolExtras, BorderLayout.SOUTH);

        loadSchemaButton.setName("mcpSampler.loadSchema");
        generateButton.setName("mcpSampler.generateSample");
        validateButton.setName("mcpSampler.validateArguments");

        loadSchemaButton.addActionListener(e -> loadSchema());
        generateButton.addActionListener(e -> generateSample());
        validateButton.addActionListener(e -> validateArguments());
    }

    boolean isCallToolExtrasVisible() {
        return callToolExtras.isVisible();
    }

    public void setConfigNameSupplier(Supplier<String> configNameSupplier) {
        if (configNameSupplier != null) {
            this.configNameSupplier = configNameSupplier;
        }
    }

    public void setToolNameSupplier(Supplier<String> toolNameSupplier) {
        if (toolNameSupplier != null) {
            this.toolNameSupplier = toolNameSupplier;
        }
    }

    public void setCallToolMode(boolean callTool) {
        callToolExtras.setVisible(callTool);
        if (!callTool) {
            clearSchemaState();
        }
        revalidate();
        repaint();
    }

    public String getArgumentsText() {
        return argumentsArea.getText();
    }

    public void setArgumentsText(String text) {
        argumentsArea.setText(text == null ? "" : text);
    }

    public void clear() {
        argumentsArea.setText("");
        clearSchemaState();
    }

    /** Package-private for unit tests (no MCP connection). */
    void applyLoadedSchema(Map<String, Object> schemaMap, String prettySchemaJson) {
        loadedSchema = schemaMap;
        schemaPreviewArea.setText(prettySchemaJson == null ? "" : prettySchemaJson);
    }

    /** Package-private for unit tests. */
    void triggerLoadSchema() {
        loadSchema();
    }

    /** Package-private for unit tests. */
    void runGenerateSample() {
        generateSample();
    }

    /** Package-private for unit tests. */
    ValidationResult runValidateArguments() {
        if (loadedSchema == null) {
            return new ValidationResult(false, "No input schema loaded");
        }
        ValidationResult result =
                ToolArgumentsSchemaSupport.validateArgumentsJson(getArgumentsText(), loadedSchema);
        statusLabel.setText(result.message());
        return result;
    }

    private void clearSchemaState() {
        loadedSchema = null;
        schemaPreviewArea.setText("");
        statusLabel.setText(" ");
    }

    private void loadSchema() {
        prepareSchemaLoad("Enter a tool name before loading the input schema.")
                .ifPresent(req -> startSchemaLoad(req, schema -> {
                    applyLoadedSchema(schema);
                    statusLabel.setText("Input schema loaded for tool '" + req.toolName() + "'");
                }, true));
    }

    private void generateSample() {
        if (loadedSchema == null) {
            loadSchemaThen(this::generateSampleFromLoadedSchema);
            return;
        }
        generateSampleFromLoadedSchema();
    }

    private void generateSampleFromLoadedSchema() {
        if (loadedSchema == null) {
            return;
        }
        try {
            argumentsArea.setText(ToolArgumentsSchemaSupport.generateSampleJson(loadedSchema));
            statusLabel.setText("Generated sample arguments from the input schema");
        } catch (IOException ex) {
            showError("Could not generate sample JSON: " + ex.getMessage());
        }
    }

    private void validateArguments() {
        if (loadedSchema == null) {
            loadSchemaThen(this::validateAgainstLoadedSchema);
            return;
        }
        validateAgainstLoadedSchema();
    }

    private void validateAgainstLoadedSchema() {
        if (loadedSchema == null) {
            return;
        }
        ValidationResult result =
                ToolArgumentsSchemaSupport.validateArgumentsJson(getArgumentsText(), loadedSchema);
        statusLabel.setText(result.message());
        if (!result.valid()) {
            JOptionPane.showMessageDialog(this,
                    result.message(),
                    "Arguments validation failed",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void loadSchemaThen(Runnable next) {
        prepareSchemaLoad("Enter a tool name first.")
                .ifPresent(req -> startSchemaLoad(req, schema -> {
                    applyLoadedSchema(schema);
                    next.run();
                }, false));
    }

    private Optional<SchemaLoadRequest> prepareSchemaLoad(String emptyToolMessage) {
        String configName = Strings.trimToDefault(configNameSupplier.get(), "mcpClient");
        String toolName = toolNameSupplier.get().trim();
        if (toolName.isEmpty()) {
            showError(emptyToolMessage);
            return Optional.empty();
        }
        return Optional.of(new SchemaLoadRequest(configName, toolName));
    }

    private void startSchemaLoad(SchemaLoadRequest request,
                                 Consumer<LoadedSchema> onSuccess,
                                 boolean clearOnFailure) {
        setSchemaActionsEnabled(false);
        statusLabel.setText("Loading input schema…");
        McpToolSchemaSync.loadAsync(request.configName(), request.toolName(),
                new McpToolSchemaSync.SchemaLoadListener() {
                    @Override
                    public void onSuccess(LoadedSchema schema) {
                        SwingUtilities.invokeLater(() -> {
                            onSuccess.accept(schema);
                            setSchemaActionsEnabled(true);
                        });
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        SwingUtilities.invokeLater(() -> {
                            if (clearOnFailure) {
                                clearSchemaState();
                            }
                            setSchemaActionsEnabled(true);
                            showError(error.getMessage());
                        });
                    }
                });
    }

    private void applyLoadedSchema(LoadedSchema schema) {
        loadedSchema = schema.schemaMap();
        schemaPreviewArea.setText(schema.prettySchemaJson());
    }

    private void setSchemaActionsEnabled(boolean enabled) {
        loadSchemaButton.setEnabled(enabled);
        generateButton.setEnabled(enabled);
        validateButton.setEnabled(enabled);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this,
                message,
                "MCP tool schema",
                JOptionPane.ERROR_MESSAGE);
    }

    private record SchemaLoadRequest(String configName, String toolName) {
    }
}
