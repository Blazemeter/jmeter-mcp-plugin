package com.blazemeter.jmeter.mcp.gui;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import com.blazemeter.jmeter.mcp.client.McpCatalogExtractor;
import com.blazemeter.jmeter.mcp.client.McpClientRegistry;
import com.blazemeter.jmeter.mcp.client.McpPreviewClients;
import com.blazemeter.jmeter.mcp.sampler.McpOperation;

/**
 * GUI helper that lists tools, resources, or prompts from a connected preview
 * client and fills {@link EditableCatalogField} choices.
 */
public final class McpSamplerCatalogSync {

    private McpSamplerCatalogSync() {
    }

    public static void syncAsync(String configName, McpOperation operation,
                                 EditableCatalogField field, Runnable onComplete) {
        field.getSyncButton().setEnabled(false);
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() {
                return McpClientRegistry.getInstance().withConnectedClient(configName, client -> {
                    McpPreviewClients.requireConnected(configName, client);
                    return switch (operation) {
                        case CALL_TOOL -> McpCatalogExtractor.toolNames(client);
                        case READ_RESOURCE -> McpCatalogExtractor.resourceUris(client);
                        case GET_PROMPT -> McpCatalogExtractor.promptNames(client);
                        default -> throw new IllegalStateException(
                                "Sync is only available for CALL_TOOL, READ_RESOURCE, and GET_PROMPT");
                    };
                });
            }

            @Override
            protected void done() {
                field.getSyncButton().setEnabled(true);
                try {
                    List<String> choices = get();
                    String previous = field.getText();
                    field.setChoices(choices, previous);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(field,
                            cause.getMessage(),
                            "MCP catalog sync failed",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
