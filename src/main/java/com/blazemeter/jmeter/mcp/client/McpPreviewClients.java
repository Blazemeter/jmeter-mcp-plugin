package com.blazemeter.jmeter.mcp.client;

import io.modelcontextprotocol.client.McpSyncClient;

/**
 * Helpers for GUI preview connections started via Start Now on MCP Client Config.
 */
public final class McpPreviewClients {

    private McpPreviewClients() {
    }

    public static McpSyncClient requireConnected(String configName, McpSyncClient client) {
        if (client == null) {
            throw new IllegalStateException(
                    "No connected MCP client for '" + configName + "'. "
                            + "Use Start Now on bzm - MCP Client Config first.");
        }
        return client;
    }
}
