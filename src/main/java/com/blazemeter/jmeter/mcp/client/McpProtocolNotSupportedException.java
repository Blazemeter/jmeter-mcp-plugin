package com.blazemeter.jmeter.mcp.client;

/**
 * Thrown when the MCP server only speaks protocol {@code 2026-07-28} (MCP v2), which this plugin
 * does not support yet.
 */
public final class McpProtocolNotSupportedException extends RuntimeException {

  public static final String USER_MESSAGE =
      "This MCP server requires protocol 2026-07-28 (MCP v2), "
          + "which this plugin does not support yet.\n\n"
          + "The plugin speaks 2025-era MCP (initialize handshake). "
          + "Use a dual-mode/legacy server, or wait for a plugin release with v2 support.";

  public McpProtocolNotSupportedException(Throwable cause) {
    super(USER_MESSAGE, cause);
  }
}
