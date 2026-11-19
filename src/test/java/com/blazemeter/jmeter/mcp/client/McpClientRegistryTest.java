package com.blazemeter.jmeter.mcp.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.blazemeter.jmeter.mcp.JMeterTestUtils;
import com.blazemeter.jmeter.mcp.config.McpClientConfig;
import com.blazemeter.jmeter.mcp.config.McpClientConfigTest;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.util.JMeterUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

class McpClientRegistryTest {

  private static final String CLIENT = "exception-test-client";

  @AfterEach
  void tearDown() {
    McpClientRegistry.getInstance().remove(CLIENT);
    McpClientRegistry.getInstance().remove("registry-stdio-client");
  }

  @Test
  void shouldRejectBlankNameWhenRegisterDeferred() {
    McpClientSettings settings = new McpClientSettings();
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> McpClientRegistry.getInstance().registerDeferred("  ", settings));
    assertTrue(ex.getMessage().contains("name must not be empty"));
  }

  @Test
  void shouldRejectNullNameWhenConnectOnStartup() {
    McpClientSettings settings = new McpClientSettings();
    assertThrows(
        IllegalArgumentException.class,
        () -> McpClientRegistry.getInstance().connectOnStartup(null, settings));
  }

  @Test
  void shouldReturnNullWhenGetOrConnectForUnregisteredClient() {
    assertNull(McpClientRegistry.getInstance().getOrConnect("unregistered-client"));
  }

  @Test
  void shouldReturnNullWhenGetOrConnectWithBlankName() {
    assertNull(McpClientRegistry.getInstance().getOrConnect("   "));
  }

  @Test
  void shouldKeepTemplatesWhenConnectFailsAndResolveAgainOnRetry() {
    JMeterTestUtils.setupJmeterEnv();
    JMeterVariables previous = JMeterContextService.getContext().getVariables();
    JMeterVariables vars = new JMeterVariables();
    vars.put("mcp_cmd", "   ");
    vars.put("mcp_bearer", "token-row-1");
    JMeterContextService.getContext().setVariables(vars);
    try {
      McpClientSettings settings = new McpClientSettings();
      settings.setName(CLIENT);
      settings.setTransport(TransportType.STDIO);
      settings.setStdioCommand("${mcp_cmd}");
      settings.setRequestHeaders("Authorization=Bearer ${mcp_bearer}");
      McpClientRegistry registry = McpClientRegistry.getInstance();
      registry.registerDeferred(CLIENT, settings);

      McpClientSettings[] attempt = new McpClientSettings[1];
      assertThrows(
          IllegalArgumentException.class,
          () -> registry.withClient(CLIENT, client -> client, null, attempt));
      assertSame(settings, registry.getSettings(CLIENT));
      assertEquals("${mcp_cmd}", settings.getStdioCommand());
      assertEquals("Authorization=Bearer ${mcp_bearer}", settings.getRequestHeaders());
      assertEquals("   ", registry.getResolvedSettings(CLIENT).getStdioCommand());
      assertEquals(
          "Authorization=Bearer token-row-1",
          registry.getResolvedSettings(CLIENT).getRequestHeaders());
      assertEquals(
          "Authorization=Bearer token-row-1", attempt[0].getRequestHeaders());

      vars.put("mcp_cmd", " \t ");
      vars.put("mcp_bearer", "token-row-2");
      assertThrows(IllegalArgumentException.class, () -> registry.getOrConnect(CLIENT));
      assertSame(settings, registry.getSettings(CLIENT));
      assertEquals("${mcp_cmd}", settings.getStdioCommand());
      assertEquals(" \t ", registry.getResolvedSettings(CLIENT).getStdioCommand());
      assertEquals(
          "Authorization=Bearer token-row-2",
          registry.getResolvedSettings(CLIENT).getRequestHeaders());
      assertEquals(
          "Authorization=Bearer token-row-1", attempt[0].getRequestHeaders());
    } finally {
      JMeterContextService.getContext().setVariables(previous);
    }
  }

  @Test
  void shouldKeepLiteralPlaceholderWhenResolvedValueContainsVariableSyntax() {
    JMeterTestUtils.setupJmeterEnv();
    JMeterVariables previous = JMeterContextService.getContext().getVariables();
    JMeterVariables vars = new JMeterVariables();
    vars.put("other", "secret");
    vars.put("mcp_token", "literal-${other}");
    JMeterContextService.getContext().setVariables(vars);
    try {
      McpClientSettings settings = new McpClientSettings();
      settings.setName(CLIENT);
      settings.setTransport(TransportType.STREAMABLE_HTTP);
      settings.setServerUrl("http://127.0.0.1:9");
      settings.setRequestHeaders("Authorization=Bearer ${mcp_token}");
      McpClientRegistry registry = McpClientRegistry.getInstance();
      registry.registerDeferred(CLIENT, settings);

      McpAuthorizationException ex =
          assertThrows(McpAuthorizationException.class, () -> registry.getOrConnect(CLIENT));
      assertTrue(ex.getMessage().contains("literal-${other}"));
      assertEquals(
          "Authorization=Bearer ${mcp_token}", registry.getSettings(CLIENT).getRequestHeaders());
      assertEquals(
          "Authorization=Bearer literal-${other}",
          registry.getResolvedSettings(CLIENT).getRequestHeaders());
    } finally {
      JMeterContextService.getContext().setVariables(previous);
    }
  }

  @Test
  void shouldExpandRegisteredPropertyOnceWhenConnecting() throws Exception {
    JMeterTestUtils.setupJmeterEnv();
    McpClientConfigTest.registerPropertyFunction();
    String urlKey = "mcp.url.once";
    String headerKey = "mcp.header.once";
    JMeterVariables previous = JMeterContextService.getContext().getVariables();
    JMeterVariables vars = new JMeterVariables();
    vars.put("other", "secret");
    JMeterContextService.getContext().setVariables(vars);
    JMeterUtils.setProperty(urlKey, "http://127.0.0.1:9");
    JMeterUtils.setProperty(headerKey, "literal-${other}");
    try {
      String urlExpr = "${__P(" + urlKey + ")}";
      String headerExpr = "X-Token=${__P(" + headerKey + ")}";
      McpClientConfig config = new McpClientConfig();
      config.setProperty(McpClientConfig.NAME, CLIENT);
      config.setProperty(McpClientConfig.TRANSPORT, TransportType.STREAMABLE_HTTP.name());
      config.setProperty(McpClientConfig.SERVER_URL, urlExpr);
      config.setProperty(McpClientConfig.REQUEST_HEADERS, headerExpr);

      McpClientSettings settings = config.toSettings();
      assertEquals(urlExpr, settings.getServerUrl());
      assertEquals(headerExpr, settings.getRequestHeaders());

      McpClientRegistry registry = McpClientRegistry.getInstance();
      registry.registerDeferred(CLIENT, settings);
      assertThrows(RuntimeException.class, () -> registry.getOrConnect(CLIENT));
      assertEquals(urlExpr, registry.getSettings(CLIENT).getServerUrl());
      assertEquals("http://127.0.0.1:9", registry.getResolvedSettings(CLIENT).getServerUrl());
      assertEquals(
          "X-Token=literal-${other}", registry.getResolvedSettings(CLIENT).getRequestHeaders());
    } finally {
      JMeterContextService.getContext().setVariables(previous);
      JMeterUtils.getJMeterProperties().remove(urlKey);
      JMeterUtils.getJMeterProperties().remove(headerKey);
    }
  }

  @Test
  void shouldPropagateInvalidStdioErrorWhenGetOrConnect() {
    McpClientSettings settings = new McpClientSettings();
    settings.setName(CLIENT);
    settings.setTransport(TransportType.STDIO);
    settings.setStdioCommand("");
    McpClientRegistry.getInstance().registerDeferred(CLIENT, settings);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> McpClientRegistry.getInstance().getOrConnect(CLIENT));
    assertTrue(ex.getMessage().contains("command"));
  }

  @Test
  void shouldIgnoreBlankNameWhenRemove() {
    McpClientRegistry.getInstance().remove(null);
    McpClientRegistry.getInstance().remove("  ");
  }

  @Test
  void shouldClearDeferredRegistrationWhenRemoveBeforeConnect() {
    McpClientSettings settings = new McpClientSettings();
    settings.setName(CLIENT);
    settings.setTransport(TransportType.SSE);
    settings.setServerUrl("http://127.0.0.1:1");
    McpClientRegistry registry = McpClientRegistry.getInstance();
    registry.registerDeferred(CLIENT, settings);
    registry.remove(CLIENT);
    assertNull(registry.getOrConnect(CLIENT));
  }

  @Test
  void shouldDeferStartupConnectWhenThreadVariableIsUnresolved() throws Exception {
    JMeterTestUtils.setupJmeterEnv();
    McpClientSettings settings = new McpClientSettings();
    settings.setName(CLIENT);
    settings.setTransport(TransportType.STDIO);
    settings.setStdioCommand("${mcp_cmd}");
    settings.setStdioEnv("TOKEN=${mcp_token}");
    McpClientRegistry registry = McpClientRegistry.getInstance();
    registry.connectOnStartup(CLIENT, settings);

    ExecutionException startup =
        assertThrows(
            ExecutionException.class,
            () -> registry.pendingConnect(CLIENT).get(10, TimeUnit.SECONDS));
    assertTrue(startup.getCause() instanceof IllegalStateException);
    assertTrue(startup.getCause().getMessage().contains("unresolved"));
    assertEquals("${mcp_cmd}", registry.getSettings(CLIENT).getStdioCommand());
    assertNull(registry.getResolvedSettings(CLIENT));

    JMeterVariables previous = JMeterContextService.getContext().getVariables();
    JMeterVariables vars = new JMeterVariables();
    vars.put("mcp_cmd", "   ");
    vars.put("mcp_token", "row-1");
    JMeterContextService.getContext().setVariables(vars);
    try {
      IllegalArgumentException retry =
          assertThrows(IllegalArgumentException.class, () -> registry.getOrConnect(CLIENT));
      assertTrue(retry.getMessage().contains("command"));
      assertEquals("${mcp_cmd}", registry.getSettings(CLIENT).getStdioCommand());
      assertEquals("   ", registry.getResolvedSettings(CLIENT).getStdioCommand());
      assertEquals("TOKEN=row-1", registry.getResolvedSettings(CLIENT).getStdioEnv());
    } finally {
      JMeterContextService.getContext().setVariables(previous);
    }
  }

  @Test
  void shouldConnectOnStartupWhenSettingsHaveNoPlaceholder() throws Exception {
    McpClientSettings settings = new McpClientSettings();
    settings.setName(CLIENT);
    settings.setTransport(TransportType.STDIO);
    settings.setStdioCommand("   ");
    McpClientRegistry registry = McpClientRegistry.getInstance();
    registry.connectOnStartup(CLIENT, settings);

    ExecutionException startup =
        assertThrows(
            ExecutionException.class,
            () -> registry.pendingConnect(CLIENT).get(10, TimeUnit.SECONDS));
    assertTrue(startup.getCause() instanceof IllegalArgumentException);
    assertTrue(startup.getCause().getMessage().contains("command"));
    assertEquals("   ", registry.getResolvedSettings(CLIENT).getStdioCommand());
  }

  @Test
  void shouldRetryConnectWhenBackgroundStartupFailed() {
    McpClientSettings invalid = new McpClientSettings();
    invalid.setName(CLIENT);
    invalid.setTransport(TransportType.STDIO);
    invalid.setStdioCommand("");
    McpClientRegistry registry = McpClientRegistry.getInstance();
    registry.connectOnStartup(CLIENT, invalid);

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> registry.getOrConnect(CLIENT));
    assertTrue(ex.getMessage().contains("command"));
  }

  @Test
  void shouldClearHttpDeferredClientsWhenCloseBeforeServerStop() {
    McpClientSettings sse = new McpClientSettings();
    sse.setName(CLIENT);
    sse.setTransport(TransportType.SSE);
    sse.setServerUrl("http://127.0.0.1:1");

    McpClientSettings stdio = new McpClientSettings();
    stdio.setName("registry-stdio-client");
    stdio.setTransport(TransportType.STDIO);
    stdio.setStdioCommand("");

    McpClientRegistry registry = McpClientRegistry.getInstance();
    registry.registerDeferred(CLIENT, sse);
    registry.registerDeferred("registry-stdio-client", stdio);

    registry.closeHttpClientsBeforeServerStop();

    assertNull(registry.getOrConnect(CLIENT));
    // STDIO registration must survive — only HTTP transports are closed for server stop.
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> registry.getOrConnect("registry-stdio-client"));
    assertTrue(ex.getMessage().contains("command"));
  }

  @Test
  @EnabledIf("com.blazemeter.jmeter.mcp.client.McpClientTestFixtures#isNpxAvailable")
  void shouldConnectStdioClientWhenRegisterDeferredAndNpxAvailable() {
    String name = "registry-stdio-client";
    McpClientRegistry registry = McpClientRegistry.getInstance();
    registry.registerDeferred(name, McpClientTestFixtures.stdioServerEverythingSettings(name));

    var first = registry.getOrConnect(name);
    var second = registry.getOrConnect(name);
    assertNotNull(first);
    assertSame(first, second);
    assertNotNull(first.ping());

    registry.remove(name);
    assertNull(registry.getOrConnect(name));
  }

  @Test
  @EnabledIf("com.blazemeter.jmeter.mcp.client.McpClientTestFixtures#isNpxAvailable")
  void shouldConnectInBackgroundWhenConnectOnStartupAndNpxAvailable() {
    String name = "registry-stdio-client";
    McpClientRegistry registry = McpClientRegistry.getInstance();
    registry.connectOnStartup(name, McpClientTestFixtures.stdioServerEverythingSettings(name));

    var client = registry.getOrConnect(name);
    assertNotNull(client);
    assertNotNull(client.ping());
  }
}
