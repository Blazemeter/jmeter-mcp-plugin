package com.blazemeter.jmeter.mcp.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * Reads tool, resource, and prompt identifiers from a connected MCP client for
 * GUI catalog sync.
 */
public final class McpCatalogExtractor {

    private McpCatalogExtractor() {
    }

    public static List<String> toolNames(McpSyncClient client) {
        TreeSet<String> names = new TreeSet<>();
        String cursor = null;
        do {
            McpSchema.ListToolsResult result = cursor == null
                    ? client.listTools()
                    : client.listTools(cursor);
            if (result.tools() != null) {
                for (McpSchema.Tool tool : result.tools()) {
                    if (tool.name() != null && !tool.name().isBlank()) {
                        names.add(tool.name());
                    }
                }
            }
            cursor = blankToNull(result.nextCursor());
        } while (cursor != null);
        return Collections.unmodifiableList(new ArrayList<>(names));
    }

    public static List<String> resourceUris(McpSyncClient client) {
        TreeSet<String> uris = new TreeSet<>();
        String cursor = null;
        do {
            McpSchema.ListResourcesResult result = cursor == null
                    ? client.listResources()
                    : client.listResources(cursor);
            if (result.resources() != null) {
                for (McpSchema.Resource resource : result.resources()) {
                    if (resource.uri() != null && !resource.uri().isBlank()) {
                        uris.add(resource.uri());
                    }
                }
            }
            cursor = blankToNull(result.nextCursor());
        } while (cursor != null);
        return Collections.unmodifiableList(new ArrayList<>(uris));
    }

    public static List<String> promptNames(McpSyncClient client) {
        TreeSet<String> names = new TreeSet<>();
        String cursor = null;
        do {
            McpSchema.ListPromptsResult result = cursor == null
                    ? client.listPrompts()
                    : client.listPrompts(cursor);
            if (result.prompts() != null) {
                for (McpSchema.Prompt prompt : result.prompts()) {
                    if (prompt.name() != null && !prompt.name().isBlank()) {
                        names.add(prompt.name());
                    }
                }
            }
            cursor = blankToNull(result.nextCursor());
        } while (cursor != null);
        return Collections.unmodifiableList(new ArrayList<>(names));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
