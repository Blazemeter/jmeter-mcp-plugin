package com.blazemeter.jmeter.mcp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.blazemeter.jmeter.mcp.JMeterTestUtils;
import com.blazemeter.jmeter.mcp.client.McpClientSettings;
import com.blazemeter.jmeter.mcp.client.TransportType;
import java.lang.reflect.Field;
import java.util.List;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jorphan.collections.HashTree;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class McpClientConfigTest {

  @BeforeAll
  static void jmeterEnv() {
    JMeterTestUtils.setupJmeterEnv();
  }

  @Test
  void shouldLeaveLiteralHeadersUnchangedWhenToSettings() {
    McpClientConfig config = new McpClientConfig();
    config.setProperty(
        McpClientConfig.REQUEST_HEADERS,
        "Authorization=Bearer token\nconfirmation-mode=DISABLE");
    assertEquals(
        "Authorization=Bearer token\nconfirmation-mode=DISABLE",
        config.toSettings().getRequestHeaders());
  }

  @Test
  void shouldMapAllPropertiesWhenToSettings() {
    McpClientConfig config = new McpClientConfig();
    config.setProperty(McpClientConfig.NAME, "clientA");
    config.setProperty(McpClientConfig.TRANSPORT, TransportType.SSE.name());
    config.setProperty(McpClientConfig.SERVER_URL, "http://localhost:9090");
    config.setProperty(McpClientConfig.ENDPOINT, "/events");
    config.setProperty(McpClientConfig.REQUEST_HEADERS, "Authorization=Bearer token\nconfirmation-mode=DISABLE");
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
    assertEquals(
        "Authorization=Bearer token\nconfirmation-mode=DISABLE", settings.getRequestHeaders());
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
  void shouldRoundTripHeaderManagerPath() {
    McpClientConfig config = new McpClientConfig();
    config.setHeaderManagerPath(List.of("Test Plan", "Thread Group", "HTTP Header Manager"));
    assertEquals(
        List.of("Test Plan", "Thread Group", "HTTP Header Manager"),
        config.getHeaderManagerPath());
  }

  @Test
  void shouldUseHeaderManagerInsteadOfInlineHeadersWhenPathResolves() throws Exception {
    HeaderManager manager = new HeaderManager();
    manager.setName("HTTP Header Manager");
    manager.add(new Header("Authorization", "Bearer from-manager"));
    manager.add(new Header("confirmation-mode", "DISABLE"));
    HashTree tree = planContaining(manager);

    McpClientConfig config = new McpClientConfig();
    config.setProperty(McpClientConfig.NAME, "clientA");
    config.setProperty(
        McpClientConfig.REQUEST_HEADERS, "Authorization=Bearer stale");
    config.setHeaderManagerPath(List.of("Root", "Test Plan", "HTTP Header Manager"));

    Field testField = StandardJMeterEngine.class.getDeclaredField("test");
    testField.setAccessible(true);
    StandardJMeterEngine engine = JMeterContextService.getContext().getEngine();
    Object previous = testField.get(engine);
    testField.set(engine, tree);
    try {
      assertEquals(
          "Authorization=Bearer from-manager\nconfirmation-mode=DISABLE",
          config.toSettings().getRequestHeaders());
    } finally {
      testField.set(engine, previous);
    }
  }

  @Test
  void shouldSendNoHeadersWhenReferencedManagerIsMissing() throws Exception {
    McpClientConfig config = new McpClientConfig();
    config.setProperty(McpClientConfig.REQUEST_HEADERS, "Authorization=Bearer stale");
    config.setHeaderManagerPath(List.of("Root", "Test Plan", "Missing Manager"));

    Field testField = StandardJMeterEngine.class.getDeclaredField("test");
    testField.setAccessible(true);
    StandardJMeterEngine engine = JMeterContextService.getContext().getEngine();
    Object previous = testField.get(engine);
    testField.set(engine, new HashTree());
    try {
      assertEquals("", config.toSettings().getRequestHeaders());
    } finally {
      testField.set(engine, previous);
    }
  }

  private static HashTree planContaining(HeaderManager manager) {
    TestElement plan = new ConfigTestElement();
    plan.setName("Test Plan");
    HashTree root = new HashTree();
    root.add(plan).add(manager);
    return root;
  }

  @Test
  void shouldUseDefaultsWhenToSettingsWithEmptyConfig() {
    McpClientConfig config = new McpClientConfig();
    McpClientSettings settings = config.toSettings();
    assertEquals("mcpClient", settings.getName());
    assertEquals(TransportType.STDIO, settings.getTransport());
    assertEquals("", settings.getRequestHeaders());
    assertEquals("jmeter-mcp-plugin", settings.getClientName());
    assertEquals("0.1.0", settings.getClientVersion());
    assertEquals(30_000L, settings.getRequestTimeoutMillis());
    assertEquals(30_000L, settings.getInitializationTimeoutMillis());
    assertFalse(settings.isConnectOnStartup());
  }
}
