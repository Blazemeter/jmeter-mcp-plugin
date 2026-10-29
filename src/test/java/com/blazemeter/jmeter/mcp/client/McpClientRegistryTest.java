package com.blazemeter.jmeter.mcp.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.blazemeter.jmeter.mcp.JMeterTestUtils;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
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

      assertThrows(IllegalArgumentException.class, () -> registry.getOrConnect(CLIENT));
      assertSame(settings, registry.getSettings(CLIENT));
      assertEquals("${mcp_cmd}", settings.getStdioCommand());
      assertEquals("Authorization=Bearer ${mcp_bearer}", settings.getRequestHeaders());
      assertEquals("   ", registry.getResolvedSettings(CLIENT).getStdioCommand());
      assertEquals(
          "Authorization=Bearer token-row-1",
          registry.getResolvedSettings(CLIENT).getRequestHeaders());

      vars.put("mcp_cmd", " \t ");
      vars.put("mcp_bearer", "token-row-2");
      assertThrows(IllegalArgumentException.class, () -> registry.getOrConnect(CLIENT));
      assertSame(settings, registry.getSettings(CLIENT));
      assertEquals("${mcp_cmd}", settings.getStdioCommand());
      assertEquals(" \t ", registry.getResolvedSettings(CLIENT).getStdioCommand());
      assertEquals(
          "Authorization=Bearer token-row-2",
          registry.getResolvedSettings(CLIENT).getRequestHeaders());
    } finally {
      JMeterContextService.getContext().setVariables(previous);
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
