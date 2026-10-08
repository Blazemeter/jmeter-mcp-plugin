package com.blazemeter.jmeter.mcp.config.gui;

import static org.assertj.swing.fixture.Containers.showInFrame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.blazemeter.jmeter.mcp.JMeterTestUtils;
import com.blazemeter.jmeter.mcp.SwingTestRunner;
import com.blazemeter.jmeter.mcp.client.TransportType;
import com.blazemeter.jmeter.mcp.config.McpClientConfig;
import java.util.List;
import org.apache.jmeter.testelement.TestElement;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SwingTestRunner.class)
public class McpClientConfigGuiIT {

  private FrameFixture frame;
  private McpClientConfigGui gui;

  @BeforeClass
  public static void setupJmeter() {
    JMeterTestUtils.setupJmeterEnv();
  }

  @Before
  public void setUp() {
    gui = new McpClientConfigGui();
    frame = showInFrame(gui);
  }

  @After
  public void tearDown() {
    if (frame != null) {
      frame.cleanUp();
    }
  }

  @Test
  public void shouldSetStdioDefaultsWhenClearGui() {
    gui.clearGui();
    frame.textBox("mcpClientConfig.name").requireText("mcpClient");
    frame.comboBox("mcpClientConfig.transport").requireSelection(TransportType.STDIO.name());
    frame.textBox("mcpClientConfig.stdioCommand").requireText("");
    frame.textBox("mcpClientConfig.stdioArgs").requireText("");
    frame.textBox("mcpClientConfig.stdioEnv").requireText("");
    frame.checkBox("mcpClientConfig.connectOnStartup").requireNotSelected();
  }

  @Test
  public void shouldLoadSseTransportFieldsWhenConfigure() {
    McpClientConfig config = new McpClientConfig();
    config.setProperty(McpClientConfig.NAME, "remote");
    config.setProperty(McpClientConfig.TRANSPORT, TransportType.SSE.name());
    config.setProperty(McpClientConfig.SERVER_URL, "http://example:8080");
    config.setProperty(McpClientConfig.ENDPOINT, "/sse");
    config.setProperty(McpClientConfig.CLIENT_NAME, "gui-client");
    config.setProperty(McpClientConfig.CLIENT_VERSION, "2.0");
    config.setProperty(McpClientConfig.REQUEST_TIMEOUT_MS, 11_000L);
    config.setProperty(McpClientConfig.INIT_TIMEOUT_MS, 9_000L);
    config.setProperty(McpClientConfig.CONNECT_ON_STARTUP, true);

    gui.configure(config);

    frame.textBox("mcpClientConfig.name").requireText("remote");
    frame.comboBox("mcpClientConfig.transport").requireSelection(TransportType.SSE.name());
    frame.textBox("mcpClientConfig.serverUrl").requireText("http://example:8080");
    frame.textBox("mcpClientConfig.endpoint").requireText("/sse");
    frame.textBox("mcpClientConfig.clientName").requireText("gui-client");
    frame.textBox("mcpClientConfig.clientVersion").requireText("2.0");
    frame.textBox("mcpClientConfig.requestTimeout").requireText("11000");
    frame.textBox("mcpClientConfig.initTimeout").requireText("9000");
    frame.checkBox("mcpClientConfig.connectOnStartup").requireSelected();
  }

  @Test
  public void shouldLoadStdioTransportFieldsWhenConfigure() {
    McpClientConfig config = new McpClientConfig();
    config.setProperty(McpClientConfig.TRANSPORT, TransportType.STDIO.name());
    config.setProperty(McpClientConfig.STDIO_COMMAND, "node");
    config.setProperty(McpClientConfig.STDIO_ARGS, "--mcp");
    config.setProperty(McpClientConfig.STDIO_ENV, "A=1");

    gui.configure(config);

    frame.comboBox("mcpClientConfig.transport").requireSelection(TransportType.STDIO.name());
    frame.textBox("mcpClientConfig.stdioCommand").requireText("node");
    frame.textBox("mcpClientConfig.stdioArgs").requireText("--mcp");
    frame.textBox("mcpClientConfig.stdioEnv").requireText("A=1");
  }

  @Test
  public void shouldPersistStreamableHttpFieldsWhenModifyTestElement() {
    McpClientConfig initial = new McpClientConfig();
    initial.setProperty(McpClientConfig.NAME, "mcpClient");
    initial.setProperty(McpClientConfig.TRANSPORT, TransportType.STREAMABLE_HTTP.name());
    initial.setProperty(McpClientConfig.SERVER_URL, "http://localhost:8080");
    initial.setProperty(McpClientConfig.ENDPOINT, "");
    initial.setProperty(McpClientConfig.CLIENT_NAME, "jmeter-mcp-plugin");
    initial.setProperty(McpClientConfig.CLIENT_VERSION, "0.1.0");
    initial.setProperty(McpClientConfig.REQUEST_TIMEOUT_MS, 30_000L);
    initial.setProperty(McpClientConfig.INIT_TIMEOUT_MS, 30_000L);
    initial.setProperty(McpClientConfig.CONNECT_ON_STARTUP, true);
    gui.configure(initial);

    frame.comboBox("mcpClientConfig.transport").requireSelection(TransportType.STREAMABLE_HTTP.name());
    frame.textBox("mcpClientConfig.serverUrl").requireVisible();
    frame.textBox("mcpClientConfig.name").setText("edited");
    frame.textBox("mcpClientConfig.serverUrl").setText("http://mcp.local");
    frame.textBox("mcpClientConfig.endpoint").setText("/custom");
    frame.textBox("mcpClientConfig.clientName").setText("name");
    frame.textBox("mcpClientConfig.clientVersion").setText("3.1");
    frame.textBox("mcpClientConfig.requestTimeout").setText("45000");
    frame.textBox("mcpClientConfig.initTimeout").setText("5000");

    McpClientConfig saved = new McpClientConfig();
    gui.modifyTestElement(saved);

    assertEquals("edited", saved.getPropertyAsString(McpClientConfig.NAME));
    assertEquals(
        TransportType.STREAMABLE_HTTP.name(), saved.getPropertyAsString(McpClientConfig.TRANSPORT));
    assertEquals("http://mcp.local", saved.getPropertyAsString(McpClientConfig.SERVER_URL));
    assertEquals("/custom", saved.getPropertyAsString(McpClientConfig.ENDPOINT));
    assertEquals("name", saved.getPropertyAsString(McpClientConfig.CLIENT_NAME));
    assertEquals("3.1", saved.getPropertyAsString(McpClientConfig.CLIENT_VERSION));
    assertEquals(45_000L, saved.getPropertyAsLong(McpClientConfig.REQUEST_TIMEOUT_MS));
    assertEquals(5_000L, saved.getPropertyAsLong(McpClientConfig.INIT_TIMEOUT_MS));
    assertTrue(saved.getPropertyAsBoolean(McpClientConfig.CONNECT_ON_STARTUP));
  }

  @Test
  public void shouldOfferSelectAndCreateHeaderManagerOnEachConfig() {
    gui.clearGui();
    frame.comboBox("mcpClientConfig.headerManager").requireSelection("(none)");
    frame.button("mcpClientConfig.createHeaderManager").requireVisible();
  }

  @Test
  public void shouldKeepLegacyHeadersWhenNoHeaderManagerIsSelected() {
    McpClientConfig config = new McpClientConfig();
    config.setProperty(McpClientConfig.TRANSPORT, TransportType.SSE.name());
    config.setProperty(McpClientConfig.REQUEST_HEADERS, "Authorization=Bearer token");
    gui.configure(config);

    frame.comboBox("mcpClientConfig.transport").requireSelection(TransportType.SSE.name());
    frame.button("mcpClientConfig.createHeaderManager").requireVisible();

    McpClientConfig saved = new McpClientConfig();
    gui.modifyTestElement(saved);
    assertEquals(
        "Authorization=Bearer token",
        saved.getPropertyAsString(McpClientConfig.REQUEST_HEADERS));
    assertEquals(List.of(), saved.getHeaderManagerPath());
  }

  @Test
  public void shouldFallBackToDefaultTimeoutsWhenInvalidNumericInput() {
    gui.clearGui();
    frame.textBox("mcpClientConfig.requestTimeout").setText("oops");
    frame.textBox("mcpClientConfig.initTimeout").setText("also-bad");

    McpClientConfig saved = (McpClientConfig) gui.createTestElement();
    assertEquals(30_000L, saved.getPropertyAsLong(McpClientConfig.REQUEST_TIMEOUT_MS));
    assertEquals(30_000L, saved.getPropertyAsLong(McpClientConfig.INIT_TIMEOUT_MS));
  }
}
