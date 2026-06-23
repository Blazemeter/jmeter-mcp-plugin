package com.blazemeter.jmeter.mcp.sampler.gui;

import static org.assertj.swing.fixture.Containers.showInFrame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.blazemeter.jmeter.mcp.JMeterTestUtils;
import com.blazemeter.jmeter.mcp.SwingTestRunner;
import com.blazemeter.jmeter.mcp.sampler.McpOperation;
import com.blazemeter.jmeter.mcp.sampler.McpSampler;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SwingTestRunner.class)
public class McpSamplerGuiIT {

  private FrameFixture frame;
  private McpSamplerGui gui;

  @BeforeClass
  public static void setupJmeter() {
    JMeterTestUtils.setupJmeterEnv();
  }

  @Before
  public void setUp() {
    gui = new McpSamplerGui();
    frame = showInFrame(gui);
  }

  @After
  public void tearDown() {
    if (frame != null) {
      frame.cleanUp();
    }
  }

  @Test
  public void shouldResetHeaderFieldsWhenClearGui() {
    gui.clearGui();
    frame.textBox("mcpSampler.configName").requireText("mcpClient");
    frame.comboBox("mcpSampler.operation").requireSelection(McpOperation.PING.name());
  }

  @Test
  public void shouldLoadCallToolFieldsWhenConfigure() {
    McpSampler sampler = new McpSampler();
    sampler.setProperty(McpSampler.CONFIG_NAME, "shared");
    sampler.setProperty(McpSampler.OPERATION, McpOperation.CALL_TOOL.name());
    sampler.setProperty(McpSampler.TOOL_NAME, "echo");
    sampler.setProperty(McpSampler.ARGUMENTS_JSON, "{\"x\":1}");

    gui.configure(sampler);

    frame.textBox("mcpSampler.configName").requireText("shared");
    frame.comboBox("mcpSampler.operation").requireSelection(McpOperation.CALL_TOOL.name());
    frame.comboBox("mcpSampler.toolName").requireSelection("echo");
    frame.textBox("mcpSampler.arguments").requireText("{\"x\":1}");
  }

  @Test
  public void shouldPersistCallToolSettingsWhenModifyTestElement() {
    gui.clearGui();
    frame.textBox("mcpSampler.configName").setText("clientB");
    frame.comboBox("mcpSampler.operation").selectItem(McpOperation.CALL_TOOL.name());
    frame.comboBox("mcpSampler.toolName").enterText("add");
    frame.textBox("mcpSampler.arguments").setText("{\"a\":1}");

    McpSampler saved = (McpSampler) gui.createTestElement();
    assertEquals("clientB", saved.getPropertyAsString(McpSampler.CONFIG_NAME));
    assertEquals(McpOperation.CALL_TOOL.name(), saved.getPropertyAsString(McpSampler.OPERATION));
    assertEquals("add", saved.getPropertyAsString(McpSampler.TOOL_NAME));
    assertEquals("{\"a\":1}", saved.getPropertyAsString(McpSampler.ARGUMENTS_JSON));
  }

  @Test
  public void shouldShowSchemaActionsWhenCallToolSelected() {
    gui.clearGui();
    frame.comboBox("mcpSampler.operation").selectItem(McpOperation.CALL_TOOL.name());

    frame.button("mcpSampler.loadSchema").requireVisible();
    frame.button("mcpSampler.generateSample").requireVisible();
    frame.button("mcpSampler.validateArguments").requireVisible();
    frame.textBox("mcpSampler.arguments").requireVisible();
  }

  @Test
  public void shouldHideSchemaActionsWhenPingSelected() {
    gui.clearGui();
    frame.comboBox("mcpSampler.operation").selectItem(McpOperation.PING.name());

    try {
      frame.button("mcpSampler.loadSchema");
      org.junit.Assert.fail("Load schema should not be visible for PING");
    } catch (org.assertj.swing.exception.ComponentLookupException expected) {
      // expected
    }
  }

  @Test
  public void shouldPersistReadResourceSettingsWhenModifyTestElement() {
    gui.clearGui();
    frame.comboBox("mcpSampler.operation").selectItem(McpOperation.READ_RESOURCE.name());
    frame.comboBox("mcpSampler.resourceUri").enterText("resource://docs");

    McpSampler saved = (McpSampler) gui.createTestElement();
    assertEquals(McpOperation.READ_RESOURCE.name(), saved.getPropertyAsString(McpSampler.OPERATION));
    assertEquals("resource://docs", saved.getPropertyAsString(McpSampler.RESOURCE_URI));
  }
}
