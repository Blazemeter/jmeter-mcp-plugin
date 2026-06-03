package com.blazemeter.jmeter.mcp.client;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * Loads a tool's {@code inputSchema} from a connected MCP client (GUI and tests).
 */
public final class McpToolSchemaLoader {

    public record LoadedSchema(Map<String, Object> schemaMap, String prettySchemaJson) {
    }

    private McpToolSchemaLoader() {
    }

    public static LoadedSchema load(McpSyncClient client, String toolName) throws IOException {
        return load(McpCatalogExtractor.listToolsPageSource(client), toolName);
    }

    public static LoadedSchema load(Function<String, McpSchema.ListToolsResult> listToolsPage,
                                    String toolName) throws IOException {
        McpSchema.Tool tool = McpCatalogExtractor.findTool(listToolsPage, toolName)
                .orElseThrow(() -> new IllegalStateException(
                        "Tool '" + toolName + "' was not found on the MCP server"));
        Map<String, Object> schemaMap = ToolArgumentsSchemaSupport.toSchemaMap(tool.inputSchema());
        String pretty = ToolArgumentsSchemaSupport.toPrettySchemaJson(tool.inputSchema());
        return new LoadedSchema(schemaMap, pretty);
    }
}
