package com.blazemeter.jmeter.mcp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.blazemeter.jmeter.mcp.client.McpClientSettings;
import com.blazemeter.jmeter.mcp.client.TransportType;
import org.junit.jupiter.api.Test;

class McpClientConfigTest {

  @Test
  void shouldMapAllPropertiesWhenToSettings() {
    McpClientConfig config = new McpClientConfig();
    config.setProperty(McpClientConfig.NAME, "clientA");
    config.setProperty(McpClientConfig.TRANSPORT, TransportType.SSE.name());
    config.setProperty(McpClientConfig.SERVER_URL, "http://localhost:9090");
    config.setProperty(McpClientConfig.ENDPOINT, "/events");
    config.setProperty(McpClientConfig.STDIO_COMMAND, "npx");
    config.setProperty(McpClientConfig.STDIO_ARGS, "-y pkg");
    config.setProperty(McpClientConfig.STDIO_ENV, "FOO=bar");
    config.setProperty(McpClientConfig.CLIENT_NAME, "test-client");
    config.setProperty(McpClientConfig.CLIENT_VERSION, "9.9");
    config.setProperty(McpClientConfig.REQUEST_TIMEOUT_MS, 12_000L);
    config.setProperty(McpClientConfig.INIT_TIMEOUT_MS, 8_000L);
    config.setProperty(McpClientConfig.CONNECT_ON_STARTUP, true);

    McpClientSettings settings = config.toSettings();
    assertEquals("clientA", settings.getName());
    assertEquals(TransportType.SSE, settings.getTransport());
    assertEquals("http://localhost:9090", settings.getServerUrl());
    assertEquals("/events", settings.getEndpoint());
    assertEquals("npx", settings.getStdioCommand());
    assertEquals("-y pkg", settings.getStdioArgs());
    assertEquals("FOO=bar", settings.getStdioEnv());
    assertEquals("test-client", settings.getClientName());
    assertEquals("9.9", settings.getClientVersion());
    assertEquals(12_000L, settings.getRequestTimeoutMillis());
    assertEquals(8_000L, settings.getInitializationTimeoutMillis());
    assertTrue(settings.isConnectOnStartup());
  }

  @Test
  void shouldUseDefaultsWhenToSettingsWithEmptyConfig() {
    McpClientConfig config = new McpClientConfig();
    McpClientSettings settings = config.toSettings();
    assertEquals("mcpClient", settings.getName());
    assertEquals(TransportType.STDIO, settings.getTransport());
    assertEquals("jmeter-mcp-plugin", settings.getClientName());
    assertEquals("0.1.0", settings.getClientVersion());
    assertEquals(30_000L, settings.getRequestTimeoutMillis());
    assertEquals(30_000L, settings.getInitializationTimeoutMillis());
    assertFalse(settings.isConnectOnStartup());
  }
}
