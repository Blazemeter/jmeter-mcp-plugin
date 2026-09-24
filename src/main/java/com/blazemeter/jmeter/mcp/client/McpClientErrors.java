package com.blazemeter.jmeter.mcp.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps SDK initialize failures onto a user-facing error.
 *
 * <p>MCP v2 ({@code 2026-07-28}) is not supported yet. HTTP strict servers typically return
 * JSON-RPC {@code -32022} with {@code supported: ["2026-07-28"]}. STDIO strict servers may
 * only expose {@code Unsupported protocol version: 2024-11-05} on {@code getMessage()}.
 *
 * <p>HTTP 401/403 becomes {@link McpAuthorizationException}. SSE 404/405/timeouts are left
 * unchanged: those also happen on misconfigured v1 servers, so they must not be reported as
 * MCP v2.
 */
public final class McpClientErrors {

  private static final Logger LOG = LoggerFactory.getLogger(McpClientErrors.class);

  private static final String MCP_V2_PROTOCOL = "2026-07-28";
  private static final String UNSUPPORTED_PROTOCOL_VERSION = "Unsupported protocol version";
  private static final String JSON_RPC_UNSUPPORTED_VERSION_CODE = "-32022";

  private McpClientErrors() {
  }

  /**
   * @param error initialize or connect failure (may be wrapped)
   * @return {@code true} when the failure is an MCP v2 protocol-version rejection
   */
  public static boolean isUnsupportedMcpV2(Throwable error) {
    String blob = collect(error);
    return blob.contains(MCP_V2_PROTOCOL)
        || blob.contains(JSON_RPC_UNSUPPORTED_VERSION_CODE)
        || blob.contains(UNSUPPORTED_PROTOCOL_VERSION);
  }

  /**
   * Rewrites MCP v2 initialize failures into {@link McpProtocolNotSupportedException} and HTTP
   * 401/403 into {@link McpAuthorizationException}. Other errors are returned unchanged
   * (wrapped in {@link RuntimeException} if needed).
   *
   * @param error initialize or connect failure
   * @return a user-facing exception when MCP v2 is detected; otherwise {@code error}
   */
  public static RuntimeException explainInitialize(Throwable error) {
    if (error instanceof McpProtocolNotSupportedException already) {
      return already;
    }
    if (error instanceof McpAuthorizationException already) {
      return already;
    }
    if (isUnsupportedMcpV2(error)) {
      LOG.warn("MCP protocol 2026-07-28 (MCP v2) is not supported yet");
      return new McpProtocolNotSupportedException(error);
    }
    if (isAuthorizationFailure(error)) {
      LOG.warn("MCP HTTP authorization failed (401/403)");
      return new McpAuthorizationException(error);
    }
    if (error instanceof RuntimeException runtime) {
      return runtime;
    }
    return new RuntimeException(error);
  }

  /**
   * @param explained result of {@link #explainInitialize(Throwable)}
   * @param http {@code true} when the GUI connect is SSE or Streamable HTTP
   * @return dialog title for a failed Connect / Start Now
   */
  public static String dialogTitle(Throwable explained, boolean http) {
    if (explained instanceof McpProtocolNotSupportedException) {
      return "MCP v2 not supported";
    }
    if (explained instanceof McpAuthorizationException) {
      return "Authorization failed";
    }
    return http ? "Connect failed" : "Start Now failed";
  }

  static boolean isAuthorizationFailure(Throwable error) {
    Throwable current = error;
    while (current != null) {
      if (current.getClass().getName().endsWith("McpHttpClientTransportAuthorizationException")) {
        return true;
      }
      String message = current.getMessage();
      if (message != null && message.contains("Authorization error when sending")) {
        return true;
      }
      for (Throwable suppressed : current.getSuppressed()) {
        if (isAuthorizationFailure(suppressed)) {
          return true;
        }
      }
      current = current.getCause();
    }
    return false;
  }

  private static String collect(Throwable error) {
    StringBuilder blob = new StringBuilder();
    Throwable current = error;
    while (current != null) {
      blob.append(current).append('\n');
      if (current.getMessage() != null) {
        blob.append(current.getMessage()).append('\n');
      }
      for (Throwable suppressed : current.getSuppressed()) {
        blob.append(collect(suppressed));
      }
      current = current.getCause();
    }
    return blob.toString();
  }
}
