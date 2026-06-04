package com.blazemeter.jmeter.mcp.server.gui;

import com.blazemeter.jmeter.commons.BlazemeterLabsLogo;
import com.blazemeter.jmeter.mcp.gui.EnvVarsField;
import com.blazemeter.jmeter.mcp.gui.GridBagForm;
import com.blazemeter.jmeter.mcp.gui.PluginGuiConstants;
import com.blazemeter.jmeter.mcp.gui.responsive.ResponsiveSizing;
import com.blazemeter.jmeter.mcp.gui.scroll.JMeterScrollableSupport;
import com.blazemeter.jmeter.mcp.server.McpServerProcess;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import org.apache.jmeter.config.gui.AbstractConfigGui;
import org.apache.jmeter.testelement.TestElement;

/** Swing GUI for {@link McpServerProcess}. */
public class McpServerProcessGui extends AbstractConfigGui implements Scrollable {

  private static final long serialVersionUID = 1L;

  private final JTextField commandField = new JTextField(20);
  private final JTextField argsField = new JTextField(40);
  private final JTextArea envArea = EnvVarsField.newTextArea();
  private final JTextField readyHostField = new JTextField(15);
  private final JTextField readyPortField = new JTextField(8);
  private final JTextField startupWaitField = new JTextField(8);

  public McpServerProcessGui() {
    init();
  }

  @Override
  public String getStaticLabel() {
    return "bzm - MCP Server Process";
  }

  @Override
  public String getLabelResource() {
    return "mcp_server_process_title";
  }

  private void init() {
    setLayout(new BorderLayout(0, 5));
    setBorder(makeBorder());
    add(makeTitlePanel(), BorderLayout.NORTH);

    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(BorderFactory.createTitledBorder("Server launch"));
    GridBagConstraints c = GridBagForm.horizontalRowConstraints();
    GridBagForm.addLabelAndField(panel, c, 0, "Command:", commandField);
    GridBagForm.addLabelAndField(panel, c, 1, "Args (space separated):", argsField);
    JScrollPane envScroll = makeScrollPane(envArea);
    EnvVarsField.applyScrollPaneSize(envScroll, envArea);
    GridBagForm.addLabelAndMultilineField(panel, c, 2, "Env (KEY=value per line):", envScroll);
    GridBagForm.addLabelAndField(panel, c, 3, "Ready host:", readyHostField);
    GridBagForm.addLabelAndField(panel, c, 4, "Ready port:", readyPortField);
    GridBagForm.addLabelAndField(panel, c, 5, "Startup wait (ms):", startupWaitField);
    add(panel, BorderLayout.CENTER);
    add(new BlazemeterLabsLogo(PluginGuiConstants.PLUGIN_REPOSITORY_URL), BorderLayout.PAGE_END);

    ResponsiveSizing.applyTree(this);

    clearGui();
    assignComponentNames();
  }

  private void assignComponentNames() {
    commandField.setName("mcpServerProcess.command");
    argsField.setName("mcpServerProcess.args");
    envArea.setName("mcpServerProcess.env");
    readyHostField.setName("mcpServerProcess.readyHost");
    readyPortField.setName("mcpServerProcess.readyPort");
    startupWaitField.setName("mcpServerProcess.startupWait");
  }

  @Override
  public Dimension getPreferredScrollableViewportSize() {
    return JMeterScrollableSupport.preferredViewportSize(this);
  }

  @Override
  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
    return JMeterScrollableSupport.scrollableUnitIncrement(visibleRect, orientation);
  }

  @Override
  public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
    return JMeterScrollableSupport.scrollableBlockIncrement(visibleRect, orientation);
  }

  @Override
  public boolean getScrollableTracksViewportWidth() {
    return JMeterScrollableSupport.tracksViewportWidth();
  }

  @Override
  public boolean getScrollableTracksViewportHeight() {
    return JMeterScrollableSupport.tracksViewportHeight();
  }

  @Override
  public TestElement createTestElement() {
    McpServerProcess element = new McpServerProcess();
    modifyTestElement(element);
    return element;
  }

  @Override
  public void modifyTestElement(TestElement element) {
    super.configureTestElement(element);
    if (!(element instanceof McpServerProcess)) {
      return;
    }
    McpServerProcess cfg = (McpServerProcess) element;
    cfg.setProperty(McpServerProcess.COMMAND, commandField.getText());
    cfg.setProperty(McpServerProcess.ARGS, argsField.getText());
    cfg.setProperty(McpServerProcess.ENV, envArea.getText());
    cfg.setProperty(McpServerProcess.READY_HOST, readyHostField.getText());
    cfg.setProperty(
        McpServerProcess.READY_PORT, GridBagForm.parseLong(readyPortField.getText(), 3001L));
    cfg.setProperty(
        McpServerProcess.STARTUP_WAIT_MS,
        GridBagForm.parseLong(startupWaitField.getText(), 60_000L));
  }

  @Override
  public void configure(TestElement element) {
    super.configure(element);
    if (!(element instanceof McpServerProcess)) {
      return;
    }
    McpServerProcess cfg = (McpServerProcess) element;
    commandField.setText(cfg.getPropertyAsString(McpServerProcess.COMMAND, ""));
    argsField.setText(cfg.getPropertyAsString(McpServerProcess.ARGS, ""));
    envArea.setText(cfg.getPropertyAsString(McpServerProcess.ENV, ""));
    readyHostField.setText(cfg.getPropertyAsString(McpServerProcess.READY_HOST, "localhost"));
    readyPortField.setText(
        String.valueOf(cfg.getPropertyAsLong(McpServerProcess.READY_PORT, 3001L)));
    startupWaitField.setText(
        String.valueOf(cfg.getPropertyAsLong(McpServerProcess.STARTUP_WAIT_MS, 60_000L)));
  }

  @Override
  public void clearGui() {
    super.clearGui();
    commandField.setText("npx");
    argsField.setText("-y @modelcontextprotocol/server-everything sse");
    envArea.setText("");
    readyHostField.setText("localhost");
    readyPortField.setText("3001");
    startupWaitField.setText("60000");
  }
}
