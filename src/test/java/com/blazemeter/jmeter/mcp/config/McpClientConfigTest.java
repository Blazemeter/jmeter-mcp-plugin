package com.blazemeter.jmeter.mcp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.blazemeter.jmeter.mcp.JMeterTestUtils;
import com.blazemeter.jmeter.mcp.client.McpClientSettings;
import com.blazemeter.jmeter.mcp.client.TransportType;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
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
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class McpClientConfigTest {

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

  @Test
  void shouldApplyHeaderManagerWhenEngineThreadHasNoContextEngine() throws Exception {
    HeaderManager manager = new HeaderManager();
    manager.setName("HTTP Header Manager - Perfecto");
    manager.add(new Header("Authorization", "Bearer from-manager"));
    manager.add(new Header("Perfecto-Cloud-Name", "beta"));

    TestPlan plan = new TestPlan();
    plan.setName("Perfecto MCP User Flow");
    HashTree tree = new HashTree();
    tree.add(plan).add(manager);

    McpClientConfig config = new McpClientConfig();
    config.setProperty(McpClientConfig.NAME, "mcpClient");
    config.setHeaderManagerPath(List.of(
        "Test Plan", "Perfecto MCP User Flow", "HTTP Header Manager - Perfecto"));

    // Non-GUI testStarted() runs on StandardJMeterEngine's executor. That thread's
    // context has no engine and there is no GUI tree.
    StandardJMeterEngine previous = sharedEngine();
    StandardJMeterEngine engine = new StandardJMeterEngine();
    engine.configure(tree);
    AtomicReference<String> headers = new AtomicReference<>();
    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread engineThread = new Thread(() -> {
      try {
        headers.set(config.toSettings().getRequestHeaders());
      } catch (Throwable thrown) {
        failure.set(thrown);
      }
    }, "StandardJMeterEngine-test");
    engineThread.start();
    engineThread.join(10_000);
    try {
      assertFalse(engineThread.isAlive(), "engine thread did not finish");
      if (failure.get() != null) {
        throw new Exception(failure.get());
      }
      assertEquals(
          "Authorization=Bearer from-manager\nPerfecto-Cloud-Name=beta",
          headers.get());
    } finally {
      restoreSharedEngine(previous);
    }
  }

  private static StandardJMeterEngine sharedEngine() throws Exception {
    Field field = StandardJMeterEngine.class.getDeclaredField("engine");
    field.setAccessible(true);
    return (StandardJMeterEngine) field.get(null);
  }

  private static void restoreSharedEngine(StandardJMeterEngine engine) throws Exception {
    Field field = StandardJMeterEngine.class.getDeclaredField("engine");
    field.setAccessible(true);
    field.set(null, engine);
  }

  /**
   * Unit tests do not ship {@code ApacheJMeter_functions}, so {@code __P} is registered here.
   * A full JMeter install loads the real function the same way.
   */
  @SuppressWarnings("unchecked")
  public static void registerPropertyFunction() throws Exception {
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
  void shouldResolveCsvVariableWhenConnecting() {
    JMeterVariables previous = JMeterContextService.getContext().getVariables();
    JMeterVariables vars = new JMeterVariables();
    vars.put("mcp_bearer", "token-from-csv");
    vars.put("mcp_url", "https://mcp.perfectomobile.com");
    JMeterContextService.getContext().setVariables(vars);
    try {
      McpClientSettings settings = new McpClientSettings();
      settings.setServerUrl("${mcp_url}");
      settings.setRequestHeaders("Authorization=Bearer ${mcp_bearer}");
      McpClientSettings resolved = settings.withPlaceholdersResolved();
      assertEquals("${mcp_url}", settings.getServerUrl());
      assertEquals("Authorization=Bearer ${mcp_bearer}", settings.getRequestHeaders());
      assertEquals("https://mcp.perfectomobile.com", resolved.getServerUrl());
      assertEquals("Authorization=Bearer token-from-csv", resolved.getRequestHeaders());
    } finally {
      JMeterContextService.getContext().setVariables(previous);
    }
  }

  @Test
  void shouldThrowWhenPlaceholderFunctionFails() throws Exception {
    registerFunction("__explode", ExplodingFunction.class);
    try {
      McpClientSettings settings = new McpClientSettings();
      settings.setServerUrl("http://${__explode()}");
      IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, settings::withPlaceholdersResolved);
      assertTrue(ex.getMessage().contains("server URL"));
      assertFalse(ex.getMessage().contains("http://${__explode()}"));
      assertFalse(ex.getMessage().contains("placeholder exploded"));
      assertNull(ex.getCause());
      assertEquals("http://${__explode()}", settings.getServerUrl());
    } finally {
      unregisterFunction("__explode");
    }
  }

  @SuppressWarnings("unchecked")
  private static void registerFunction(String name, Class<? extends Function> type)
      throws Exception {
    functionMap().put(name, type);
  }

  private static void unregisterFunction(String name) throws Exception {
    functionMap().remove(name);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Class<? extends Function>> functionMap() throws Exception {
    Field field = CompoundVariable.class.getDeclaredField("functions");
    field.setAccessible(true);
    return (Map<String, Class<? extends Function>>) field.get(null);
  }

  /** Function whose execution fails so placeholder resolution must report that error. */
  public static final class ExplodingFunction implements Function {

    @Override
    public String execute(SampleResult previousResult, Sampler currentSampler) {
      throw new IllegalStateException("placeholder exploded");
    }

    @Override
    public void setParameters(Collection<CompoundVariable> parameters) {
      // no arguments
    }

    @Override
    public String getReferenceKey() {
      return "__explode";
    }

    @Override
    public List<String> getArgumentDesc() {
      return List.of();
    }
  }

  @Test
  void shouldKeepPropertyExpressionsWhenToSettings() {
    McpClientConfig config = new McpClientConfig();
    String url = "${__P(mcp.url.unit,https://mcp.perfectomobile.com)}";
    String endpoint = "${__P(mcp.endpoint.unit,/mcp)}";
    config.setProperty(McpClientConfig.SERVER_URL, url);
    config.setProperty(McpClientConfig.ENDPOINT, endpoint);

    McpClientSettings settings = config.toSettings();
    assertEquals(url, settings.getServerUrl());
    assertEquals(endpoint, settings.getEndpoint());
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
