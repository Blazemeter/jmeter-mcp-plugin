package com.blazemeter.jmeter.mcp.client;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;

import com.helger.commons.annotation.VisibleForTesting;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * Reads tool, resource, and prompt identifiers from a connected MCP client for GUI catalog sync.
 */
public final class McpCatalogExtractor {

  private McpCatalogExtractor() {

    public static Function<String, McpSchema.ListToolsResult> listToolsPageSource(
            McpSyncClient client) {
        return pageSource(client::listTools, client::listTools);
    }

    public static Optional<McpSchema.Tool> findTool(McpSyncClient client, String toolName) {
        return findTool(listToolsPageSource(client), toolName);
    }

    @VisibleForTesting
    static Optional<McpSchema.Tool> findTool(
            Function<String, McpSchema.ListToolsResult> listToolsPage, String toolName) {
        if (toolName == null || toolName.isBlank()) {
            return Optional.empty();
        }
        String target = toolName.trim();
        Optional<McpSchema.Tool>[] found = new Optional[] {Optional.empty()};
        forEachPage(listToolsPage, McpSchema.ListToolsResult::nextCursor, result -> {
            if (found[0].isPresent() || result.tools() == null) {
                return;
            }
            for (McpSchema.Tool tool : result.tools()) {
                if (target.equals(tool.name())) {
                    found[0] = Optional.of(tool);
                    return;
                }
            }
        });
        return found[0];
    }

    public static List<String> toolNames(McpSyncClient client) {
        return toolNames(listToolsPageSource(client));
    }

    @VisibleForTesting
    static List<String> toolNames(Function<String, McpSchema.ListToolsResult> listToolsPage) {
        TreeSet<String> names = new TreeSet<>();
        forEachPage(listToolsPage, McpSchema.ListToolsResult::nextCursor, result -> {
            if (result.tools() == null) {
                return;
            }
            for (McpSchema.Tool tool : result.tools()) {
                if (tool.name() != null && !tool.name().isBlank()) {
                    names.add(tool.name());
                }
            }
        });
        return Collections.unmodifiableList(new ArrayList<>(names));
    }

    public static Function<String, McpSchema.ListResourcesResult> listResourcesPageSource(
            McpSyncClient client) {
        return pageSource(client::listResources, client::listResources);
    }

    public static List<String> resourceUris(McpSyncClient client) {
        return resourceUris(listResourcesPageSource(client));
    }

    @VisibleForTesting
    static List<String> resourceUris(
            Function<String, McpSchema.ListResourcesResult> listResourcesPage) {
        TreeSet<String> uris = new TreeSet<>();
        forEachPage(listResourcesPage, McpSchema.ListResourcesResult::nextCursor, result -> {
            if (result.resources() == null) {
                return;
            }
            for (McpSchema.Resource resource : result.resources()) {
                if (resource.uri() != null && !resource.uri().isBlank()) {
                    uris.add(resource.uri());
                }
            }
        });
        return Collections.unmodifiableList(new ArrayList<>(uris));
    }
    while (cursor != null);
    return Collections.unmodifiableList(new ArrayList<>(names));
  }

    public static Function<String, McpSchema.ListPromptsResult> listPromptsPageSource(
            McpSyncClient client) {
        return pageSource(client::listPrompts, client::listPrompts);
    }

    public static List<String> promptNames(McpSyncClient client) {
        return promptNames(listPromptsPageSource(client));
    }

    @VisibleForTesting
    static List<String> promptNames(
            Function<String, McpSchema.ListPromptsResult> listPromptsPage) {
        TreeSet<String> names = new TreeSet<>();
        forEachPage(listPromptsPage, McpSchema.ListPromptsResult::nextCursor, result -> {
            if (result.prompts() == null) {
                return;
            }
            for (McpSchema.Prompt prompt : result.prompts()) {
                if (prompt.name() != null && !prompt.name().isBlank()) {
                    names.add(prompt.name());
                }
            }
        });
        return Collections.unmodifiableList(new ArrayList<>(names));
    }
    while (cursor != null);
    return Collections.unmodifiableList(new ArrayList<>(uris));
  }

    private static <R> void forEachPage(Function<String, R> fetchPage,
                                          Function<R, String> nextCursor,
                                          Consumer<R> pageConsumer) {
        String cursor = null;
        do {
            R result = fetchPage.apply(cursor);
            pageConsumer.accept(result);
            cursor = blankToNull(nextCursor.apply(result));
        } while (cursor != null);
    }

    private static <R> Function<String, R> pageSource(
            java.util.function.Supplier<R> firstPage,
            Function<String, R> nextPage) {
        return cursor -> cursor == null ? firstPage.get() : nextPage.apply(cursor);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
    while (cursor != null);
    return Collections.unmodifiableList(new ArrayList<>(names));
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
