package com.blazemeter.jmeter.mcp.sampler;

/**
 * Operations the {@code MCP Sampler} can perform against a connected MCP
 * server.
 */
public enum McpOperation {

    PING,
    LIST_TOOLS,
    CALL_TOOL,
    LIST_RESOURCES,
    READ_RESOURCE,
    LIST_PROMPTS,
    GET_PROMPT;

    public static McpOperation fromString(String value) {
        if (value == null || value.isBlank()) {
            return PING;
        }
        return McpOperation.valueOf(value.trim().toUpperCase().replace('-', '_'));
    }
}
