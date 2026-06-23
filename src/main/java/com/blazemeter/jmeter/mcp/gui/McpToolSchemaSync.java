package com.blazemeter.jmeter.mcp.gui;

import com.blazemeter.jmeter.mcp.client.McpClientRegistry;
import com.blazemeter.jmeter.mcp.client.McpPreviewClients;
import com.blazemeter.jmeter.mcp.client.McpToolSchemaLoader;
import com.blazemeter.jmeter.mcp.client.McpToolSchemaLoader.LoadedSchema;
import io.modelcontextprotocol.client.McpSyncClient;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;

/**
 * Loads a tool's {@code inputSchema} from a connected preview client for the sampler GUI.
 */
public final class McpToolSchemaSync {

  private McpToolSchemaSync() {
  }

  static LoadedSchema loadFromConnectedClient(McpSyncClient client, String configName,
                        String toolName) {
    try {
      return McpToolSchemaLoader.load(
          McpPreviewClients.requireConnected(configName, client), toolName);
    } catch (java.io.IOException ex) {
      throw new IllegalStateException(
          "Could not read input schema for tool '" + toolName + "'", ex);
    }
  }

  public static void loadAsync(String configName, String toolName,
                SchemaLoadListener listener) {
    new SwingWorker<LoadedSchema, Void>() {
      @Override
      protected LoadedSchema doInBackground() {
        return McpClientRegistry.getInstance().withConnectedClient(configName,
            client -> loadFromConnectedClient(client, configName, toolName));
      }

      @Override
      protected void done() {
        try {
          listener.onSuccess(get());
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          listener.onFailure(ex);
        } catch (ExecutionException ex) {
          Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
          listener.onFailure(cause);
        }
      }
    }.execute();
  }

  @FunctionalInterface
  public interface SchemaLoadListener {
    void onSuccess(LoadedSchema schema);

    default void onFailure(Throwable error) {
      // optional
    }
  }
}
