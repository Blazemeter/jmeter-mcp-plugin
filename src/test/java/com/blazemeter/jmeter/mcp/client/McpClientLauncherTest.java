package com.blazemeter.jmeter.mcp.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class McpClientLauncherTest {

  @Test
  void portFromServerUrlUsesExplicitPort() {
    assertEquals(8080, McpClientLauncher.portFromServerUrl("http://localhost:8080/mcp", 3001));
  }

  @Test
  void portFromServerUrlDefaultsForHttp() {
    assertEquals(80, McpClientLauncher.portFromServerUrl("http://localhost/mcp", 3001));
  }

  @Test
  void portFromServerUrlFallsBackWhenBlank() {
    assertEquals(3001, McpClientLauncher.portFromServerUrl("", 3001));
  }

  @Test
  void resolveReadyEndpointPrefersConfiguredPort() {
    McpClientSettings settings = new McpClientSettings();
    settings.setServerUrl("http://localhost:9999");
    settings.setServerReadyHost("127.0.0.1");
    settings.setServerReadyPort(3001);

    McpClientLauncher.ReadyEndpoint ready = McpClientLauncher.resolveReadyEndpoint(settings);
    assertEquals("127.0.0.1", ready.host());
    assertEquals(3001, ready.port());
  }
}
