package com.blazemeter.jmeter.mcp.client;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpCatalogExtractorTest {

    private static final McpSchema.JsonSchema SAMPLE_SCHEMA = new McpSchema.JsonSchema(
            "object",
            Map.of("x", Map.of("type", "string")),
            List.of("x"),
            false,
            null,
            null);

    @Test
    void shouldReturnEmptyWhenToolNameIsBlank() {
        Function<String, McpSchema.ListToolsResult> pages =
                cursor -> page(tool("echo", SAMPLE_SCHEMA));

        assertTrue(McpCatalogExtractor.findTool(pages, null).isEmpty());
        assertTrue(McpCatalogExtractor.findTool(pages, "  ").isEmpty());
    }

    @Test
    void shouldFindToolOnFirstPageWhenNameMatches() {
        McpSchema.Tool echo = tool("echo", SAMPLE_SCHEMA);

        Optional<McpSchema.Tool> found = McpCatalogExtractor.findTool(
                cursor -> page(echo), "echo");

        assertTrue(found.isPresent());
        assertEquals("echo", found.get().name());
    }

    @Test
    void shouldTrimToolNameWhenFinding() {
        McpSchema.Tool echo = tool("echo", SAMPLE_SCHEMA);

        Optional<McpSchema.Tool> found = McpCatalogExtractor.findTool(
                cursor -> page(echo), "  echo  ");

        assertTrue(found.isPresent());
    }

    @Test
    void shouldFindToolOnSecondPageWhenPaginated() {
        McpSchema.Tool target = tool("target", SAMPLE_SCHEMA);
        Function<String, McpSchema.ListToolsResult> pages = cursor -> {
            if (cursor == null) {
                return new McpSchema.ListToolsResult(
                        List.of(tool("other", SAMPLE_SCHEMA)), "page-2", null);
            }
            return new McpSchema.ListToolsResult(List.of(target), null, null);
        };

        Optional<McpSchema.Tool> found = McpCatalogExtractor.findTool(pages, "target");

        assertTrue(found.isPresent());
        assertEquals("target", found.get().name());
    }

    @Test
    void shouldReturnEmptyWhenToolNotListed() {
        Optional<McpSchema.Tool> found = McpCatalogExtractor.findTool(
                cursor -> page(tool("alpha", SAMPLE_SCHEMA)), "missing");

        assertFalse(found.isPresent());
    }

    @Test
    void shouldSkipNullToolsListWhenPageHasNoTools() {
        McpSchema.ListToolsResult empty = new McpSchema.ListToolsResult(null, null, null);

        assertFalse(McpCatalogExtractor.findTool(cursor -> empty, "any").isPresent());
    }

    @Test
    void shouldCollectToolNamesAcrossPagesAndSkipBlank() {
        McpSchema.Tool valid = tool("beta", SAMPLE_SCHEMA);
        McpSchema.Tool blank = new McpSchema.Tool("  ", null, null, SAMPLE_SCHEMA, null, null, null);
        var pages = new java.util.HashMap<String, McpSchema.ListToolsResult>();
        pages.put(null, new McpSchema.ListToolsResult(
                List.of(tool("alpha", SAMPLE_SCHEMA), blank), "p2", null));
        pages.put("p2", new McpSchema.ListToolsResult(List.of(valid), null, null));

        List<String> names = McpCatalogExtractor.toolNames(pages::get);

        assertEquals(List.of("alpha", "beta"), names);
    }

    @Test
    void shouldCollectResourceUrisAcrossPages() {
        McpSchema.Resource r1 =
                new McpSchema.Resource("file:///a", "a", null, null, null, null, null, null);
        McpSchema.Resource r2 =
                new McpSchema.Resource("file:///b", "b", null, null, null, null, null, null);
        var pages = new java.util.HashMap<String, McpSchema.ListResourcesResult>();
        pages.put(null, new McpSchema.ListResourcesResult(List.of(r1), "next", null));
        pages.put("next", new McpSchema.ListResourcesResult(List.of(r2), null, null));

        List<String> uris = McpCatalogExtractor.resourceUris(pages::get);

        assertEquals(List.of("file:///a", "file:///b"), uris);
    }

    @Test
    void shouldCollectPromptNamesAndSkipBlank() {
        McpSchema.Prompt p1 = new McpSchema.Prompt("greet", "Greeting", "Hi", List.of());
        McpSchema.Prompt blank = new McpSchema.Prompt(" ", null, null, List.of());
        McpSchema.ListPromptsResult page = new McpSchema.ListPromptsResult(List.of(p1, blank), null, null);

        List<String> names = McpCatalogExtractor.promptNames(cursor -> page);

        assertEquals(List.of("greet"), names);
    }

    private static McpSchema.ListToolsResult page(McpSchema.Tool... tools) {
        return new McpSchema.ListToolsResult(List.of(tools), null, null);
    }

    private static McpSchema.Tool tool(String name, McpSchema.JsonSchema inputSchema) {
        return new McpSchema.Tool(name, null, null, inputSchema, null, null, null);
    }
}
