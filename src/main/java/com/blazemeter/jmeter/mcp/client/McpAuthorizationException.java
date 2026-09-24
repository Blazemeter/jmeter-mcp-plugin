package com.blazemeter.jmeter.mcp.client;

/**
 * Thrown when MCP HTTP initialize fails due to missing, unresolved, or rejected credentials
 * (HTTP 401/403).
 */
public final class McpAuthorizationException extends RuntimeException {

  public static final String USER_MESSAGE =
      "MCP server returned HTTP 401/403 (authorization failed).\n\n"
          + "Set a valid Authorization header on the HTTP Header Manager referenced by "
          + "MCP Client Config, for example:\n"
          + "Authorization=Bearer <apiKeyId>:<apiKeySecret>\n\n"
          + "An empty token, an unresolved ${variable}, or wrong credentials all "
          + "produce this error. JMeter GUI does not pick up -J flags from a previous "
          + "CLI run; put the token in that header manager or in user.properties.";

  public McpAuthorizationException(String message) {
    super(message);
  }

  public McpAuthorizationException(Throwable cause) {
    super(USER_MESSAGE, cause);
  }
}
