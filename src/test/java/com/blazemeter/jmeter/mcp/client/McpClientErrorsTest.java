package com.blazemeter.jmeter.mcp.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class McpClientErrorsTest {

  @Test
  void shouldDetectHttpStrictV2InitializeFailure() {
    RuntimeException http =
        wrapInitialize(
            "Bad Request. Status code:400, response-event:AggregateResponseEvent["
                + "data={\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32022,"
                + "\"message\":\"Unsupported protocol version: 2025-11-25\","
                + "\"data\":{\"supported\":[\"2026-07-28\"],"
                + "\"requested\":\"2025-11-25\"}}}]");

    assertTrue(McpClientErrors.isUnsupportedMcpV2(http));
    RuntimeException explained = McpClientErrors.explainInitialize(http);
    assertInstanceOf(McpProtocolNotSupportedException.class, explained);
    assertTrue(explained.getMessage().contains("2026-07-28"));
    assertTrue(explained.getMessage().contains("not support yet"));
    assertEquals("MCP v2 not supported", McpClientErrors.dialogTitle(explained, true));
  }

  @Test
  void shouldDetectStdioStrictV2WhenMessageOmitsSupportedVersion() {
    RuntimeException stdio =
        wrapInitialize("Unsupported protocol version: 2024-11-05");

    assertTrue(McpClientErrors.isUnsupportedMcpV2(stdio));
    assertInstanceOf(
        McpProtocolNotSupportedException.class, McpClientErrors.explainInitialize(stdio));
  }

  @Test
  void shouldDetectJsonRpcCodeWithoutProtocolDateInMessage() {
    RuntimeException error =
        wrapInitialize("JSONRPCError[code=-32022, message=Unsupported protocol version]");

    assertTrue(McpClientErrors.isUnsupportedMcpV2(error));
  }

  @Test
  void shouldNotTreatSse404AsMcpV2() {
    RuntimeException sse =
        wrapInitialize("Invalid SSE response. Status code: 404 Line: <!DOCTYPE html>");

    assertFalse(McpClientErrors.isUnsupportedMcpV2(sse));
    assertSame(sse, McpClientErrors.explainInitialize(sse));
  }

  @Test
  void shouldNotTreatSse405MethodNotAllowedAsMcpV2() {
    RuntimeException sse =
        wrapInitialize(
            "Invalid SSE response. Status code: 405 Line: "
                + "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32000,"
                + "\"message\":\"Method not allowed.\"},\"id\":null}");

    assertFalse(McpClientErrors.isUnsupportedMcpV2(sse));
    assertSame(sse, McpClientErrors.explainInitialize(sse));
  }

  @Test
  void shouldNotRewriteGenericInitializeTimeout() {
    RuntimeException timeout =
        wrapInitialize(
            "Did not observe any item or terminal signal within 8000ms in 'map'");

    assertFalse(McpClientErrors.isUnsupportedMcpV2(timeout));
    assertSame(timeout, McpClientErrors.explainInitialize(timeout));
  }

  @Test
  void shouldNotRewriteInvalidStdioCommand() {
    IllegalArgumentException missingCommand =
        new IllegalArgumentException(
            "MCP STDIO transport requires a command (e.g. 'npx' or '/usr/local/bin/node')");

    assertFalse(McpClientErrors.isUnsupportedMcpV2(missingCommand));
    assertSame(missingCommand, McpClientErrors.explainInitialize(missingCommand));
  }

  @Test
  void shouldReturnSameInstanceWhenAlreadyWrapped() {
    McpProtocolNotSupportedException already =
        new McpProtocolNotSupportedException(new RuntimeException("cause"));

    assertSame(already, McpClientErrors.explainInitialize(already));
    assertEquals(McpProtocolNotSupportedException.USER_MESSAGE, already.getMessage());
  }

  @Test
  void shouldWrapCheckedExceptionWhenNotMcpV2() {
    Exception checked = new Exception("connect failed");
    RuntimeException explained = McpClientErrors.explainInitialize(checked);
    assertEquals("connect failed", explained.getCause().getMessage());
  }

  private static RuntimeException wrapInitialize(String causeMessage) {
    RuntimeException cause = new RuntimeException(causeMessage);
    RuntimeException outer =
        new RuntimeException("Client failed to initialize by explicit API call", cause);
    outer.addSuppressed(new Exception("#block terminated with an error"));
    return outer;
  }
}
