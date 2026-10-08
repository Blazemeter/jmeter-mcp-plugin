package com.blazemeter.jmeter.mcp.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.blazemeter.jmeter.mcp.server.McpServerProcessManager;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

class McpClientFactoryTest {

  @AfterEach
  void stopManagedServer() {
    McpServerProcessManager.getInstance().stop();
  }

  @Test
  void shouldSplitSimpleArgsWhenArgStringHasUnquotedTokens() {
    assertEquals(
        List.of("-y", "@modelcontextprotocol/server-everything", "dir"),
        McpClientFactory.splitArgs("-y @modelcontextprotocol/server-everything dir"));
  }

  @Test
  void shouldSplitQuotedArgsWhenArgStringContainsDoubleQuotes() {
    assertEquals(
        List.of("--message", "hello world", "--other", "v"),
        McpClientFactory.splitArgs("--message \"hello world\" --other v"));
  }

  @Test
  void shouldReturnEmptyListWhenArgsAreBlankOrNull() {
    assertTrue(McpClientFactory.splitArgs("   ").isEmpty());
    assertTrue(McpClientFactory.splitArgs(null).isEmpty());
  }

  @Test
  void shouldTrimTrailingWhitespaceWhenArgTokenHasTrailingSpaces() {
    assertEquals(List.of("--mcp"), McpClientFactory.splitArgs("--mcp "));
  }

  @Test
  void shouldParseEnvLinesWhenInputContainsCommentsAndAssignments() {
    Map<String, String> env = McpClientFactory.parseEnv("FOO=bar\n# comment\nBAZ = qux\n\nEMPTY=");
    assertEquals(3, env.size());
    assertEquals("bar", env.get("FOO"));
    assertEquals("qux", env.get("BAZ"));
    assertEquals("", env.get("EMPTY"));
  }

  @Test
  void shouldReturnEmptyMapWhenEnvInputIsBlank() {
    assertTrue(McpClientFactory.parseEnv(null).isEmpty());
    assertTrue(McpClientFactory.parseEnv("  \n").isEmpty());
  }

  @Test
  void shouldSkipInvalidLinesWhenEnvInputHasMalformedEntries() {
    Map<String, String> env = McpClientFactory.parseEnv("no-equals\n=empty-key\nKEY=value");
    assertEquals(1, env.size());
    assertEquals("value", env.get("KEY"));
  }

  @Test
  void shouldSplitSingleQuotedArgsWhenArgStringContainsSingleQuotes() {
    assertEquals(List.of("arg", "two words"), McpClientFactory.splitArgs("arg 'two words'"));
  }

  @Test
  void shouldBuildStdioTransportWhenCommandArgsAndEnvAreSet() {
    McpClientSettings settings = new McpClientSettings();
    settings.setTransport(TransportType.STDIO);
    settings.setStdioCommand("npx");
    settings.setStdioArgs("-y pkg");
    settings.setStdioEnv("NODE_OPTIONS=--no-warnings");

    assertInstanceOf(
        StdioClientTransport.class, McpClientFactoryTestSupport.buildTransport(settings));
  }

  @Test
  void shouldBuildStdioTransportWhenCommandHasSurroundingWhitespace() {
    McpClientSettings settings = new McpClientSettings();
    settings.setTransport(TransportType.STDIO);
    settings.setStdioCommand("  node  ");

    assertInstanceOf(
        StdioClientTransport.class, McpClientFactoryTestSupport.buildTransport(settings));
  }

  @Test
  void shouldBuildSseTransportWhenServerUrlAndEndpointAreSet() {
    McpClientSettings settings = new McpClientSettings();
    settings.setTransport(TransportType.SSE);
    settings.setServerUrl("http://127.0.0.1:8080");
    settings.setEndpoint("/custom/sse");

    assertInstanceOf(
        HttpClientSseClientTransport.class, McpClientFactoryTestSupport.buildTransport(settings));
  }

  @Test
  void shouldBuildSseTransportWhenOnlyServerUrlIsSet() {
    McpClientSettings settings = new McpClientSettings();
    settings.setTransport(TransportType.SSE);
    settings.setServerUrl("http://127.0.0.1:8080");

    assertInstanceOf(
        HttpClientSseClientTransport.class, McpClientFactoryTestSupport.buildTransport(settings));
  }

  @Test
  void shouldBuildStreamableHttpTransportWhenServerUrlAndEndpointAreSet() {
    McpClientSettings settings = new McpClientSettings();
    settings.setTransport(TransportType.STREAMABLE_HTTP);
    settings.setServerUrl("http://127.0.0.1:8080");
    settings.setEndpoint("/mcp");
    settings.setRequestHeaders("Authorization=Bearer test-token");

    assertInstanceOf(
        HttpClientStreamableHttpTransport.class,
        McpClientFactoryTestSupport.buildTransport(settings));
  }

  @Test
  @EnabledIf("com.blazemeter.jmeter.mcp.client.McpClientTestFixtures#isNpxAvailable")
  void shouldInitializeStdioClientWhenNpxServerEverythingIsAvailable() {
    McpClientSettings settings =
        McpClientTestFixtures.stdioServerEverythingSettings("factory-stdio");
    settings.setClientName("");
    settings.setClientVersion("  ");

    try (var client = McpClientFactory.buildAndInitialize(settings)) {
      assertNotNull(client.ping());
    }
  }

  @Test
  @EnabledIf("com.blazemeter.jmeter.mcp.client.McpClientTestFixtures#isNpxAvailable")
  void shouldInitializeSseClientWhenHttpServerEverythingIsRunning() {
    int port = McpClientTestFixtures.findAvailablePort();
    startHttpServer("sse", port);
    McpClientSettings settings =
        McpClientTestFixtures.sseServerEverythingSettings("factory-sse", port);

    try (var client = McpClientFactory.buildAndInitialize(settings)) {
      assertNotNull(client.ping());
    }
  }

  @Test
  @EnabledIf("com.blazemeter.jmeter.mcp.client.McpClientTestFixtures#isNpxAvailable")
  void shouldInitializeStreamableHttpClientWhenHttpServerEverythingIsRunning() {
    int port = McpClientTestFixtures.findAvailablePort();
    startHttpServer("streamableHttp", port);
    McpClientSettings settings =
        McpClientTestFixtures.streamableHttpServerEverythingSettings(
            "factory-streamable", port);

    try (var client = McpClientFactory.buildAndInitialize(settings)) {
      assertNotNull(client.ping());
    }
  }

  private static void startHttpServer(String mode, int port) {
    McpServerProcessManager.getInstance()
        .start(
            "npx",
            McpClientTestFixtures.SERVER_EVERYTHING_ARGS + " " + mode,
            "PORT=" + port,
            "127.0.0.1",
            port,
            McpClientTestFixtures.LIVE_SERVER_TIMEOUT_MS);
  }

  @Test
  void shouldRejectNullSettingsWhenBuildAndInitialize() {
    assertThrows(NullPointerException.class, () -> McpClientFactory.buildAndInitialize(null));
  }

  @Test
  void shouldRejectBlankCommandWhenStdioCommandIsWhitespaceOnly() {
    McpClientSettings s = new McpClientSettings();
    s.setTransport(TransportType.STDIO);
    s.setStdioCommand("   ");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> McpClientFactory.buildAndInitialize(s));
    assertTrue(ex.getMessage().contains("command"));
  }

  @Test
  void shouldRejectNullCommandWhenStdioCommandIsMissing() {
    McpClientSettings s = new McpClientSettings();
    s.setTransport(TransportType.STDIO);
    s.setStdioCommand(null);
    assertThrows(IllegalArgumentException.class, () -> McpClientFactory.buildAndInitialize(s));
  }

  @Test
  void shouldRejectMissingServerUrlWhenSseServerUrlIsBlank() {
    McpClientSettings s = new McpClientSettings();
    s.setTransport(TransportType.SSE);
    s.setServerUrl("  ");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> McpClientFactory.buildAndInitialize(s));
    assertTrue(ex.getMessage().contains("SSE"));
    assertTrue(ex.getMessage().contains("server URL"));
  }

  @Test
  void shouldRejectMissingServerUrlWhenStreamableHttpServerUrlIsNull() {
    McpClientSettings s = new McpClientSettings();
    s.setTransport(TransportType.STREAMABLE_HTTP);
    s.setServerUrl(null);
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> McpClientFactory.buildAndInitialize(s));
    assertTrue(ex.getMessage().contains("Streamable HTTP"));
    assertTrue(ex.getMessage().contains("server URL"));
  }

  @Test
  void shouldRejectEmptyBearerWhenAuthorizationHeaderHasNoToken() {
    McpClientSettings s = streamableHttpWithHeaders("Authorization=Bearer ");
    McpAuthorizationException ex =
        assertThrows(
            McpAuthorizationException.class,
            () -> McpClientFactoryTestSupport.buildTransport(s));
    assertTrue(ex.getMessage().contains("empty"));
  }

  @Test
  void shouldRejectUnresolvedVariableWhenAuthorizationHeaderContainsJmeterFunction() {
    McpClientSettings s =
        streamableHttpWithHeaders("Authorization=Bearer ${MCP_BEARER}");
    McpAuthorizationException ex =
        assertThrows(
            McpAuthorizationException.class,
            () -> McpClientFactoryTestSupport.buildTransport(s));
    assertTrue(ex.getMessage().contains("${MCP_BEARER}"));
  }

  @Test
  void shouldBuildStreamableHttpWhenAuthorizationHeaderHasToken() {
    McpClientSettings s =
        streamableHttpWithHeaders(
            "Authorization=Bearer id:secret\nconfirmation-mode=DISABLE");
    assertInstanceOf(
        HttpClientStreamableHttpTransport.class,
        McpClientFactoryTestSupport.buildTransport(s));
  }

  private static McpClientSettings streamableHttpWithHeaders(String headers) {
    McpClientSettings s = new McpClientSettings();
    s.setTransport(TransportType.STREAMABLE_HTTP);
    s.setServerUrl("http://127.0.0.1:8080");
    s.setRequestHeaders(headers);
    return s;
  }
}
