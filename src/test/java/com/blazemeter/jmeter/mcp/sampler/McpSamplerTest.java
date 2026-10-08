package com.blazemeter.jmeter.mcp.sampler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.blazemeter.jmeter.mcp.client.McpClientRegistry;
import com.blazemeter.jmeter.mcp.client.McpClientSettings;
import com.blazemeter.jmeter.mcp.client.McpProtocolNotSupportedException;
import com.blazemeter.jmeter.mcp.client.TransportType;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.apache.jmeter.samplers.SampleResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class McpSamplerTest {

  private static final String CLIENT = "sampler-exception-test";

  @AfterEach
  void tearDown() {
    McpClientRegistry.getInstance().remove(CLIENT);
  }

  @Test
  void shouldMarkSampleFailureWhenClientConfigIsMissing() {
    McpSampler sampler = new McpSampler();
    sampler.setProperty(McpSampler.CONFIG_NAME, "missing-config");
    sampler.setProperty(McpSampler.OPERATION, McpOperation.PING.name());

    SampleResult result = sampler.sample(null);

    assertFalse(result.isSuccessful());
    assertEquals("IllegalStateException", result.getResponseCode());
    assertTrue(result.getResponseMessage().contains("missing-config"));
    assertTrue(result.getResponseMessage().contains("MCP Client Config"));
  }

  @Test
  void shouldReportMcpV2NotSupportedWhenInitializeRejectsProtocol() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    byte[] body =
        ("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32022,"
                + "\"message\":\"Unsupported protocol version: 2025-11-25\","
                + "\"data\":{\"supported\":[\"2026-07-28\"],"
                + "\"requested\":\"2025-11-25\"}},\"id\":\"1\"}")
            .getBytes(StandardCharsets.UTF_8);
    server.createContext(
        "/mcp",
        exchange -> {
          exchange.getRequestBody().readAllBytes();
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(400, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();
    try {
      McpClientSettings settings = new McpClientSettings();
      settings.setName(CLIENT);
      settings.setTransport(TransportType.STREAMABLE_HTTP);
      settings.setServerUrl("http://127.0.0.1:" + server.getAddress().getPort());
      settings.setEndpoint("/mcp");
      settings.setRequestTimeoutMillis(3_000L);
      settings.setInitializationTimeoutMillis(3_000L);
      McpClientRegistry.getInstance().registerDeferred(CLIENT, settings);

      McpSampler sampler = new McpSampler();
      sampler.setProperty(McpSampler.CONFIG_NAME, CLIENT);
      sampler.setProperty(McpSampler.OPERATION, McpOperation.PING.name());

      SampleResult result = sampler.sample(null);

      assertFalse(result.isSuccessful());
      assertEquals(
          McpProtocolNotSupportedException.class.getSimpleName(), result.getResponseCode());
      assertEquals(McpProtocolNotSupportedException.USER_MESSAGE, result.getResponseMessage());
    } finally {
      server.stop(0);
    }
  }

  @Test
  void shouldShowConfiguredHeadersAsRequestHeaders() {
    McpClientSettings settings = new McpClientSettings();
    settings.setName(CLIENT);
    settings.setTransport(TransportType.STDIO);
    settings.setStdioCommand("   ");
    settings.setRequestHeaders(
        "Authorization=Bearer token\n# ignored\nX-MCP-Client=jmeter-mcp-plugin");
    McpClientRegistry.getInstance().registerDeferred(CLIENT, settings);

    McpSampler sampler = new McpSampler();
    sampler.setProperty(McpSampler.CONFIG_NAME, CLIENT);
    sampler.setProperty(McpSampler.OPERATION, McpOperation.PING.name());

    SampleResult result = sampler.sample(null);

    assertEquals(
        "Authorization: Bearer token\nX-MCP-Client: jmeter-mcp-plugin",
        result.getRequestHeaders());
  }

  @Test
  void shouldMarkSampleFailureWhenClientSettingsAreInvalid() {
    McpClientSettings settings = new McpClientSettings();
    settings.setName(CLIENT);
    settings.setTransport(TransportType.STDIO);
    settings.setStdioCommand("   ");
    McpClientRegistry.getInstance().registerDeferred(CLIENT, settings);

    McpSampler sampler = new McpSampler();
    sampler.setProperty(McpSampler.CONFIG_NAME, CLIENT);
    sampler.setProperty(McpSampler.OPERATION, McpOperation.PING.name());

    SampleResult result = sampler.sample(null);

    assertFalse(result.isSuccessful());
    assertTrue(
        result.getResponseDataAsString().contains("command")
            || result.getResponseMessage().contains("command"));
  }

  @Test
  void shouldRejectBlankToolNameWhenRequiredIsCalled() {
    McpSampler sampler = new McpSampler();
    sampler.setProperty(McpSampler.TOOL_NAME, "  ");

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> sampler.required(McpSampler.TOOL_NAME, "Tool Name"));
    assertTrue(ex.getMessage().contains("Tool Name"));
    assertTrue(ex.getMessage().contains("required"));
  }

  @Test
  void shouldRejectInvalidJsonWhenParseArguments() {
    McpSampler sampler = new McpSampler();
    sampler.setProperty(McpSampler.ARGUMENTS_JSON, "{not-valid-json");

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, sampler::parseArguments);
    assertTrue(ex.getMessage().contains("JSON object"));
  }

  @Test
  void shouldReturnEmptyMapWhenArgumentsJsonIsBlank() {
    McpSampler sampler = new McpSampler();
    sampler.setProperty(McpSampler.ARGUMENTS_JSON, "");

    assertTrue(sampler.parseArguments().isEmpty());
  }

  @Test
  void shouldRejectJsonArrayWhenParseArguments() {
    McpSampler sampler = new McpSampler();
    sampler.setProperty(McpSampler.ARGUMENTS_JSON, "[1, 2]");

    assertThrows(IllegalArgumentException.class, sampler::parseArguments);
  }
}
