package com.blazemeter.jmeter.mcp.server.gui;

import static org.assertj.swing.fixture.Containers.showInFrame;
import static org.junit.Assert.assertEquals;

import com.blazemeter.jmeter.mcp.JMeterTestUtils;
import com.blazemeter.jmeter.mcp.SwingTestRunner;
import com.blazemeter.jmeter.mcp.server.McpServerProcess;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SwingTestRunner.class)
public class McpServerProcessGuiIT {

  private FrameFixture frame;
  private McpServerProcessGui gui;

  @BeforeClass
  public static void setupJmeter() {
    JMeterTestUtils.setupJmeterEnv();
  }

  @Before
  public void setUp() {
    gui = new McpServerProcessGui();
    frame = showInFrame(gui);
  }

  @After
  public void tearDown() {
    if (frame != null) {
      frame.cleanUp();
    }
  }

  @Test
  public void shouldSetLaunchDefaultsWhenClearGui() {
    gui.clearGui();
    frame.textBox("mcpServerProcess.command").requireText("npx");
    frame
        .textBox("mcpServerProcess.args")
        .requireText("-y @modelcontextprotocol/server-everything sse");
    frame.textBox("mcpServerProcess.env").requireText("");
    frame.textBox("mcpServerProcess.readyHost").requireText("localhost");
    frame.textBox("mcpServerProcess.readyPort").requireText("3001");
    frame.textBox("mcpServerProcess.startupWait").requireText("60000");
  }

  @Test
  public void shouldLoadServerProcessPropertiesWhenConfigure() {
    McpServerProcess process = new McpServerProcess();
    process.setProperty(McpServerProcess.COMMAND, "node");
    process.setProperty(McpServerProcess.ARGS, "server.js");
    process.setProperty(McpServerProcess.ENV, "PORT=3001");
    process.setProperty(McpServerProcess.READY_HOST, "127.0.0.1");
    process.setProperty(McpServerProcess.READY_PORT, 4000L);
    process.setProperty(McpServerProcess.STARTUP_WAIT_MS, 15_000L);

    gui.configure(process);

    frame.textBox("mcpServerProcess.command").requireText("node");
    frame.textBox("mcpServerProcess.args").requireText("server.js");
    frame.textBox("mcpServerProcess.env").requireText("PORT=3001");
    frame.textBox("mcpServerProcess.readyHost").requireText("127.0.0.1");
    frame.textBox("mcpServerProcess.readyPort").requireText("4000");
    frame.textBox("mcpServerProcess.startupWait").requireText("15000");
  }

  @Test
  public void shouldPersistEditedFieldsWhenModifyTestElement() {
    gui.clearGui();
    frame.textBox("mcpServerProcess.command").setText("python");
    frame.textBox("mcpServerProcess.args").setText("-m mcp");
    frame.textBox("mcpServerProcess.env").setText("DEBUG=1");
    frame.textBox("mcpServerProcess.readyHost").setText("0.0.0.0");
    frame.textBox("mcpServerProcess.readyPort").setText("8080");
    frame.textBox("mcpServerProcess.startupWait").setText("12000");

    McpServerProcess saved = (McpServerProcess) gui.createTestElement();
    assertEquals("python", saved.getPropertyAsString(McpServerProcess.COMMAND));
    assertEquals("-m mcp", saved.getPropertyAsString(McpServerProcess.ARGS));
    assertEquals("DEBUG=1", saved.getPropertyAsString(McpServerProcess.ENV));
    assertEquals("0.0.0.0", saved.getPropertyAsString(McpServerProcess.READY_HOST));
    assertEquals(8080L, saved.getPropertyAsLong(McpServerProcess.READY_PORT));
    assertEquals(12_000L, saved.getPropertyAsLong(McpServerProcess.STARTUP_WAIT_MS));
  }

  @Test
  public void shouldFallBackToDefaultsWhenInvalidNumericFields() {
    gui.clearGui();
    frame.textBox("mcpServerProcess.readyPort").setText("bad");
    frame.textBox("mcpServerProcess.startupWait").setText("nope");

    McpServerProcess saved = (McpServerProcess) gui.createTestElement();
    assertEquals(3001L, saved.getPropertyAsLong(McpServerProcess.READY_PORT));
    assertEquals(60_000L, saved.getPropertyAsLong(McpServerProcess.STARTUP_WAIT_MS));
  }
}
