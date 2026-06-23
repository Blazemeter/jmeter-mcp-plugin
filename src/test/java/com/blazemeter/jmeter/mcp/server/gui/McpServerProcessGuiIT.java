package com.blazemeter.jmeter.mcp.server.gui;

import static org.assertj.swing.fixture.Containers.showInFrame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.blazemeter.jmeter.mcp.JMeterTestUtils;
import com.blazemeter.jmeter.mcp.SwingTestRunner;
import com.blazemeter.jmeter.mcp.server.McpServerProcess;
import com.blazemeter.jmeter.mcp.server.McpServerProcessManager;
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

    @After
    public void tearDown() {
        McpServerProcessManager.getInstance().stop();
        if (frame != null) {
            frame.cleanUp();
        }
    }

    @Test
    public void shouldSetLaunchDefaultsWhenClearGui() {
        gui.clearGui();
        frame.textBox("mcpServerProcess.command").requireText("npx");
        frame.textBox("mcpServerProcess.args")
                .requireText("-y @modelcontextprotocol/server-everything sse");
        frame.textBox("mcpServerProcess.env").requireText("");
        frame.textBox("mcpServerProcess.readyHost").requireText("localhost");
        frame.textBox("mcpServerProcess.readyPort").requireText("3001");
        frame.textBox("mcpServerProcess.startupWait").requireText("60000");
        frame.button("mcpServerProcess.start").requireText("Start");
        frame.button("mcpServerProcess.stop").requireDisabled();
    }

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

    @Test
    public void shouldInvokeServerControlStartWithFormSettings() {
        StubMcpServerControl control = new StubMcpServerControl();
        showGuiWithControl(control);
        gui.clearGui();
        frame.textBox("mcpServerProcess.command").setText("node");
        frame.textBox("mcpServerProcess.args").setText("server.js");
        frame.textBox("mcpServerProcess.env").setText("PORT=3001");
        frame.textBox("mcpServerProcess.readyHost").setText("127.0.0.1");
        frame.textBox("mcpServerProcess.readyPort").setText("4000");
        frame.textBox("mcpServerProcess.startupWait").setText("15000");

        frame.button("mcpServerProcess.start").click();
        frame.robot().waitForIdle();

        assertNotNull(control.lastStart);
        assertEquals("node", control.lastStart.command());
        assertEquals("server.js", control.lastStart.args());
        assertEquals("PORT=3001", control.lastStart.env());
        assertEquals("127.0.0.1", control.lastStart.readyHost());
        assertEquals(4000, control.lastStart.readyPort());
        assertEquals(15_000L, control.lastStart.startupWaitMs());
        frame.label("mcpServerProcess.status")
                .requireText("Running at 127.0.0.1:4000 (pid 42, reused on test run)");
        frame.button("mcpServerProcess.stop").requireEnabled();
    }

    @Test
    public void shouldInvokeServerControlStopWhenStopClicked() {
        StubMcpServerControl control = new StubMcpServerControl();
        control.managedPid = 99L;
        showGuiWithControl(control);

        frame.button("mcpServerProcess.stop").click();
        frame.robot().waitForIdle();

        assertTrue(control.stopCalled);
        frame.label("mcpServerProcess.status").requireText("Not running");
        frame.button("mcpServerProcess.stop").requireDisabled();
    }

    @Test
    public void shouldShowReachableStatusWhenPortOpenButNotManaged() {
        StubMcpServerControl control = new StubMcpServerControl();
        control.portOpen = true;
        showGuiWithControl(control);

        frame.label("mcpServerProcess.status")
                .requireText("Reachable at localhost:3001 (not managed here)");
        frame.button("mcpServerProcess.stop").requireDisabled();
    }

    @Test
    public void shouldShowNotRunningWhenStartFails() {
        StubMcpServerControl control = new StubMcpServerControl();
        control.startFailure = new RuntimeException("boom");
        showGuiWithControl(control);

        frame.button("mcpServerProcess.start").click();
        frame.robot().waitForIdle();
        frame.optionPane().requireMessage("boom");
        frame.optionPane().okButton().click();
        frame.label("mcpServerProcess.status").requireText("Not running");
    }

    private void showGuiWithControl(StubMcpServerControl control) {
        if (frame != null) {
            frame.cleanUp();
        }
        gui = new McpServerProcessGui(control);
        frame = showInFrame(gui);
    }
}
