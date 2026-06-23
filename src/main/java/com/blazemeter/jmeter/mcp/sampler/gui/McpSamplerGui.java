package com.blazemeter.jmeter.mcp.sampler.gui;

import com.blazemeter.jmeter.commons.BlazemeterLabsLogo;
import com.blazemeter.jmeter.mcp.gui.EditableCatalogField;
import com.blazemeter.jmeter.mcp.gui.GridBagForm;
import com.blazemeter.jmeter.mcp.gui.McpSamplerCatalogSync;
import com.blazemeter.jmeter.mcp.gui.PluginGuiConstants;
import com.blazemeter.jmeter.mcp.gui.ToolArgumentsEditor;
import com.blazemeter.jmeter.mcp.gui.responsive.AdaptiveCardLayoutHost;
import com.blazemeter.jmeter.mcp.gui.responsive.ResponsiveSizing;
import com.blazemeter.jmeter.mcp.gui.scroll.JMeterScrollableSupport;
import com.blazemeter.jmeter.mcp.sampler.McpOperation;
import com.blazemeter.jmeter.mcp.sampler.McpSampler;
import com.blazemeter.jmeter.mcp.util.Strings;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import org.apache.jmeter.samplers.gui.AbstractSamplerGui;
import org.apache.jmeter.testelement.TestElement;

/**
 * Swing GUI for {@link McpSampler}. Shows only the input fields relevant to
 * the currently selected {@link McpOperation}.
 */
public class McpSamplerGui extends AbstractSamplerGui implements Scrollable {

  private static final long serialVersionUID = 1L;

  private static final String CARD_EMPTY = "empty";
  private static final String CARD_TOOL = "tool";
  private static final String CARD_RESOURCE = "resource";
  private static final String CARD_PROMPT = "prompt";

  private final JTextField configNameField = new JTextField("mcpClient", 20);
  private final JComboBox<McpOperation> operationCombo =
      new JComboBox<>(McpOperation.values());

  private final EditableCatalogField toolNameField =
      new EditableCatalogField(25, "Sync");
  private final EditableCatalogField resourceUriField =
      new EditableCatalogField(35, "Sync");
  private final EditableCatalogField promptNameField =
      new EditableCatalogField(25, "Sync");

  private final ToolArgumentsEditor toolArgumentsEditor = new ToolArgumentsEditor();

  private final CardLayout cardLayout = new CardLayout();
  private final JPanel cards = new JPanel(cardLayout);
  private AdaptiveCardLayoutHost operationCardHost;
  private JPanel centerPanel;

  public McpSamplerGui() {
    super();
    init();
  }

  @Override
  public String getStaticLabel() {
    return "bzm - MCP Sampler";
  }

  @Override
  public String getLabelResource() {
    return "mcp_sampler_title";
  }

  private void init() {
    setLayout(new BorderLayout(0, 5));
    setBorder(makeBorder());
    add(makeTitlePanel(), BorderLayout.NORTH);

    centerPanel = new JPanel(new BorderLayout(0, 5));
    centerPanel.add(buildHeader(), BorderLayout.NORTH);
    centerPanel.add(buildCards(), BorderLayout.CENTER);
    centerPanel.add(toolArgumentsEditor, BorderLayout.SOUTH);
    add(centerPanel, BorderLayout.CENTER);
    add(new BlazemeterLabsLogo(PluginGuiConstants.PLUGIN_REPOSITORY_URL), BorderLayout.PAGE_END);

    ResponsiveSizing.applyTree(this);

    toolArgumentsEditor.setConfigNameSupplier(() -> configNameField.getText());
    toolArgumentsEditor.setToolNameSupplier(toolNameField::getText);

    operationCombo.addActionListener(e -> updateCard());
    operationCombo.setSelectedItem(McpOperation.PING);
    toolNameField.getSyncButton().addActionListener(
        e -> syncCatalog(McpOperation.CALL_TOOL, toolNameField));
    resourceUriField.getSyncButton().addActionListener(
        e -> syncCatalog(McpOperation.READ_RESOURCE, resourceUriField));
    promptNameField.getSyncButton().addActionListener(
        e -> syncCatalog(McpOperation.GET_PROMPT, promptNameField));
    updateCard();
    assignComponentNames();
  }

  private void assignComponentNames() {
    configNameField.setName("mcpSampler.configName");
    operationCombo.setName("mcpSampler.operation");
    toolNameField.setName("mcpSampler.toolName");
    resourceUriField.setName("mcpSampler.resourceUri");
    promptNameField.setName("mcpSampler.promptName");
  }

  private void syncCatalog(McpOperation operation, EditableCatalogField field) {
    String configName = Strings.trimToDefault(configNameField.getText(), "mcpClient");
    McpSamplerCatalogSync.syncAsync(configName, operation, field, this::updateCard);
  }

  private JPanel buildHeader() {
    JPanel p = new JPanel(new GridBagLayout());
    p.setBorder(BorderFactory.createTitledBorder("MCP Operation"));
    GridBagConstraints c = GridBagForm.horizontalRowConstraints();
    GridBagForm.addLabelAndField(p, c, 0, "Client Config (Variable Name):", configNameField);
    GridBagForm.addLabelAndField(p, c, 1, "Operation:", operationCombo);
    return p;
  }

  private AdaptiveCardLayoutHost buildCards() {
    cards.setBorder(BorderFactory.createTitledBorder("Operation Parameters"));

    cards.add(infoCard("No parameters required for this operation."), CARD_EMPTY);

    JPanel tool = new JPanel(new GridBagLayout());
    GridBagConstraints tc = GridBagForm.horizontalRowConstraints();
    GridBagForm.addLabelAndField(tool, tc, 0, "Tool Name:", toolNameField);
    cards.add(tool, CARD_TOOL);

    JPanel resource = new JPanel(new GridBagLayout());
    GridBagConstraints rc = GridBagForm.horizontalRowConstraints();
    GridBagForm.addLabelAndField(resource, rc, 0, "Resource URI:", resourceUriField);
    cards.add(resource, CARD_RESOURCE);

    JPanel prompt = new JPanel(new GridBagLayout());
    GridBagConstraints pc = GridBagForm.horizontalRowConstraints();
    GridBagForm.addLabelAndField(prompt, pc, 0, "Prompt Name:", promptNameField);
    cards.add(prompt, CARD_PROMPT);

    operationCardHost = new AdaptiveCardLayoutHost(cards);
    return operationCardHost;
  }

  private JPanel infoCard(String text) {
    JPanel p = new JPanel(new BorderLayout());
    JTextArea area = new JTextArea(text);
    area.setEditable(false);
    area.setOpaque(false);
    p.add(area, BorderLayout.CENTER);
    return p;
  }

  private void updateCard() {
    McpOperation op = (McpOperation) operationCombo.getSelectedItem();
    if (op == null) {
      cardLayout.show(cards, CARD_EMPTY);
    } else {
      switch (op) {
        case CALL_TOOL:
          cardLayout.show(cards, CARD_TOOL);
          break;
        case READ_RESOURCE:
          cardLayout.show(cards, CARD_RESOURCE);
          break;
        case GET_PROMPT:
          cardLayout.show(cards, CARD_PROMPT);
          break;
        default:
          cardLayout.show(cards, CARD_EMPTY);
          break;
      }
    }
    if (operationCardHost != null) {
      operationCardHost.afterCardShown();
    }
    boolean callTool = op == McpOperation.CALL_TOOL;
    boolean usesArguments = callTool || op == McpOperation.GET_PROMPT;
    toolArgumentsEditor.setVisible(usesArguments);
    toolArgumentsEditor.setCallToolMode(callTool);
    centerPanel.revalidate();
    centerPanel.repaint();
    revalidate();
    repaint();
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
    McpSampler sampler = new McpSampler();
    modifyTestElement(sampler);
    return sampler;
  }

  @Override
  public void modifyTestElement(TestElement element) {
    super.configureTestElement(element);
    if (!(element instanceof McpSampler)) {
      return;
    }
    McpSampler sampler = (McpSampler) element;
    sampler.setProperty(McpSampler.CONFIG_NAME, configNameField.getText());
    McpOperation op = (McpOperation) operationCombo.getSelectedItem();
    sampler.setProperty(McpSampler.OPERATION,
        op != null ? op.name() : McpOperation.PING.name());
    sampler.setProperty(McpSampler.TOOL_NAME, toolNameField.getText().trim());
    sampler.setProperty(McpSampler.RESOURCE_URI, resourceUriField.getText().trim());
    sampler.setProperty(McpSampler.PROMPT_NAME, promptNameField.getText().trim());
    sampler.setProperty(McpSampler.ARGUMENTS_JSON, toolArgumentsEditor.getArgumentsText());
  }

  @Override
  public void configure(TestElement element) {
    super.configure(element);
    if (!(element instanceof McpSampler)) {
      return;
    }
    McpSampler sampler = (McpSampler) element;
    configNameField.setText(sampler.getPropertyAsString(McpSampler.CONFIG_NAME, "mcpClient"));
    operationCombo.setSelectedItem(McpOperation.fromString(
        sampler.getPropertyAsString(McpSampler.OPERATION, McpOperation.PING.name())));
    toolNameField.setText(sampler.getPropertyAsString(McpSampler.TOOL_NAME, "").trim());
    resourceUriField.setText(sampler.getPropertyAsString(McpSampler.RESOURCE_URI, "").trim());
    promptNameField.setText(sampler.getPropertyAsString(McpSampler.PROMPT_NAME, "").trim());
    toolArgumentsEditor.setArgumentsText(
        sampler.getPropertyAsString(McpSampler.ARGUMENTS_JSON, ""));
    updateCard();
  }

  @Override
  public void clearGui() {
    super.clearGui();
    configNameField.setText("mcpClient");
    operationCombo.setSelectedItem(McpOperation.PING);
    toolNameField.setChoices(java.util.List.of(), "");
    resourceUriField.setChoices(java.util.List.of(), "");
    promptNameField.setChoices(java.util.List.of(), "");
    toolArgumentsEditor.clear();
    updateCard();
  }
}
