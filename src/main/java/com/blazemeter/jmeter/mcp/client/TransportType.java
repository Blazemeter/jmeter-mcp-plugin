package com.blazemeter.jmeter.mcp.client;

/**
 * Supported MCP client transport layers.
 *
 * <p>All three transports are implemented by the official MCP Java SDK
 * (module {@code io.modelcontextprotocol.sdk:mcp}) without any external
 * web framework.
 */
public enum TransportType {

    /** Subprocess-based transport using stdin/stdout (e.g. local Node tools). */
    STDIO,

    /** Legacy HTTP + Server-Sent Events transport. */
    SSE,

    /** MCP "Streamable HTTP" transport (2025-03-26 spec). */
    STREAMABLE_HTTP;

    public static TransportType fromString(String value) {
        if (value == null || value.isBlank()) {
            return STDIO;
        }
        String normalized = value.trim().toUpperCase().replace('-', '_');
        for (TransportType t : values()) {
            if (t.name().equals(normalized)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown MCP transport type: " + value);
    }
}
