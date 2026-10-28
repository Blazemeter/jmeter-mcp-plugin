package com.blazemeter.jmeter.mcp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.blazemeter.jmeter.mcp.JMeterTestUtils;
import com.blazemeter.jmeter.mcp.client.McpClientSettings;
import com.blazemeter.jmeter.mcp.client.TransportType;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.engine.util.CompoundVariable;
import org.apache.jmeter.functions.AbstractFunction;
import org.apache.jmeter.functions.Function;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.util.JMeterUtils;
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

  /**
   * Unit tests do not ship {@code ApacheJMeter_functions}, so {@code __P} is registered here.
   * A full JMeter install loads the real function the same way.
   */
  @SuppressWarnings("unchecked")
  private static void registerPropertyFunction() throws Exception {
    Field field = CompoundVariable.class.getDeclaredField("functions");
    field.setAccessible(true);
    Map<String, Class<? extends Function>> functions =
        (Map<String, Class<? extends Function>>) field.get(null);
    functions.putIfAbsent("__P", TestPropertyFunction.class);
  }

  /** Stand-in for JMeter {@code ${__P(name,default)}}. */
  public static final class TestPropertyFunction extends AbstractFunction {

    private CompoundVariable name;
    private CompoundVariable fallback;

    @Override
    public String execute(SampleResult previousResult, Sampler currentSampler) {
      String propertyName = name.execute();
      String defaultValue = fallback == null ? propertyName : fallback.execute();
      return JMeterUtils.getPropDefault(propertyName, defaultValue);
    }

    @Override
    public void setParameters(Collection<CompoundVariable> parameters) {
      CompoundVariable[] values = parameters.toArray(new CompoundVariable[0]);
      name = values[0];
      fallback = values.length > 1 ? values[1] : null;
    }

    @Override
    public String getReferenceKey() {
      return "__P";
    }

    @Override
    public List<String> getArgumentDesc() {
      return List.of();
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
  void shouldResolvePropertyFunctionInServerUrlWhenToSettings() throws Exception {
    registerPropertyFunction();
    String urlKey = "mcp.url.unit";
    String endpointKey = "mcp.endpoint.unit";
    JMeterUtils.getJMeterProperties().remove(urlKey);
    JMeterUtils.getJMeterProperties().remove(endpointKey);
    try {
      McpClientConfig config = new McpClientConfig();
      config.setProperty(
          McpClientConfig.SERVER_URL,
          "${__P(" + urlKey + ",https://mcp.perfectomobile.com)}");
      config.setProperty(McpClientConfig.ENDPOINT, "${__P(" + endpointKey + ",/mcp)}");

      McpClientSettings defaults = config.toSettings();
      assertEquals("https://mcp.perfectomobile.com", defaults.getServerUrl());
      assertEquals("/mcp", defaults.getEndpoint());

      JMeterUtils.setProperty(urlKey, "https://mcp.example.test");
      assertEquals("https://mcp.example.test", config.toSettings().getServerUrl());
    } finally {
      JMeterUtils.getJMeterProperties().remove(urlKey);
      JMeterUtils.getJMeterProperties().remove(endpointKey);
    }
  }

  @Test
  void shouldUseDefaultsWhenToSettingsWithEmptyConfig() {
    McpClientConfig config = new McpClientConfig();
    McpClientSettings settings = config.toSettings();
    assertEquals("mcpClient", settings.getName());
    assertEquals(TransportType.STDIO, settings.getTransport());
    assertEquals("", settings.getRequestHeaders());
    assertEquals("jmeter-mcp-plugin", settings.getClientName());
    assertEquals("1.0.0", settings.getClientVersion());
    assertEquals(30_000L, settings.getRequestTimeoutMillis());
    assertEquals(30_000L, settings.getInitializationTimeoutMillis());
    assertFalse(settings.isConnectOnStartup());
  }
}
