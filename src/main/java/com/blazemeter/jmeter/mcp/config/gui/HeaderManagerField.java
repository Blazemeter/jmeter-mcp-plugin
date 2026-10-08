package com.blazemeter.jmeter.mcp.config.gui;

import com.blazemeter.jmeter.mcp.config.HeaderManagerLookup;
import com.blazemeter.jmeter.mcp.config.McpClientConfig;
import java.awt.BorderLayout;
import java.awt.event.HierarchyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.TreePath;
import org.apache.jmeter.exceptions.IllegalUserActionException;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.testelement.TestElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Per-config header manager control. The combo selects one manager for this MCP Client Config, and
 * Create adds a new manager next to this config and associates only this config with it.
 */
public final class HeaderManagerField extends JPanel {

  private static final long serialVersionUID = 1L;

  private static final Logger LOG = LoggerFactory.getLogger(HeaderManagerField.class);

  private static final String HEADER_PANEL =
      "org.apache.jmeter.protocol.http.gui.HeaderPanel";

  private final JComboBox<HeaderManagerChoice> combo = new JComboBox<>();
  private final JButton createButton = new JButton("Create Header Manager");
  private final JLabel missingLabel =
      new JLabel("The referenced header manager is not in the test plan.");

  private final Supplier<String> configName;

  private List<String> selectedPath = List.of();
  private String legacy = "";
  private boolean refreshing;

  public HeaderManagerField(Supplier<String> configName) {
    super(new BorderLayout(0, 2));
    this.configName = configName;
    combo.setName("mcpClientConfig.headerManager");
    combo.setPrototypeDisplayValue(HeaderManagerChoice.placeholder());
    combo.setToolTipText(
        "Header manager for this MCP Client Config. Other configs keep their own selection.");
    createButton.setName("mcpClientConfig.createHeaderManager");
    createButton.setToolTipText(
        "Create a header manager next to this config and associate it with this config only.");
    missingLabel.setName("mcpClientConfig.headerManagerMissing");
    missingLabel.setVisible(false);

    JPanel controls = new JPanel(new BorderLayout(4, 0));
    controls.add(combo, BorderLayout.CENTER);
    controls.add(createButton, BorderLayout.WEST);
    add(controls, BorderLayout.CENTER);
    add(missingLabel, BorderLayout.SOUTH);

    combo.addActionListener(event -> onSelectionChanged());
    createButton.addActionListener(event -> createHeaderManager());
    addHierarchyListener(this::refreshWhenShown);
    rebuild();
  }

  public void loadFrom(McpClientConfig config) {
    legacy = config.getPropertyAsString(McpClientConfig.REQUEST_HEADERS, "");
    setSelectedPath(config.getHeaderManagerPath());
  }

  public void applyTo(McpClientConfig config) {
    List<String> path = getSelectedPath();
    config.setHeaderManagerPath(path);
    if (path.isEmpty()) {
      config.setProperty(McpClientConfig.REQUEST_HEADERS, legacy);
    } else {
      config.setProperty(McpClientConfig.REQUEST_HEADERS, "");
    }
  }

  public void clear() {
    legacy = "";
    setSelectedPath(List.of());
  }

  public List<String> getSelectedPath() {
    return List.copyOf(selectedPath);
  }

  public void setSelectedPath(List<String> path) {
    selectedPath = path == null ? List.of() : List.copyOf(path);
    rebuild();
  }

  private void onSelectionChanged() {
    if (refreshing) {
      return;
    }
    HeaderManagerChoice choice = (HeaderManagerChoice) combo.getSelectedItem();
    if (choice == null || choice.node == null) {
      selectedPath = List.of();
    } else {
      selectedPath = choice.path();
    }
    missingLabel.setVisible(false);
  }

  private void refreshWhenShown(HierarchyEvent event) {
    if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0 || !isShowing()) {
      return;
    }
    rebuild();
  }

  private void rebuild() {
    if (refreshing) {
      return;
    }
    refreshing = true;
    try {
      List<JMeterTreeNode> managers = currentManagers();
      combo.removeAllItems();
      combo.setVisible(true);
      createButton.setVisible(true);
      combo.addItem(HeaderManagerChoice.none());
      int selectedIndex = 0;
      for (JMeterTreeNode node : managers) {
        HeaderManagerChoice choice = HeaderManagerChoice.of(node);
        combo.addItem(choice);
        if (choice.path().equals(selectedPath)) {
          selectedIndex = combo.getItemCount() - 1;
        }
      }
      combo.setSelectedIndex(selectedIndex);
      missingLabel.setVisible(!selectedPath.isEmpty() && !containsPath(managers, selectedPath));
      revalidate();
    } finally {
      refreshing = false;
    }
  }

  private void createHeaderManager() {
    GuiPackage guiPackage = GuiPackage.getInstance();
    if (guiPackage == null) {
      showError("Open this config in the test plan to create a header manager.");
      return;
    }
    JMeterTreeNode current = guiPackage.getCurrentNode();
    if (current == null || !(current.getParent() instanceof JMeterTreeNode parent)) {
      showError("Place MCP Client Config in the test plan, then create the header manager.");
      return;
    }
    try {
      HeaderManager manager = newHeaderManager(guiPackage, configName.get());
      JMeterTreeNode created = guiPackage.getTreeModel().addComponent(manager, parent);
      placeBeside(guiPackage, parent, current, created);
      guiPackage.setDirty(true);
      reveal(guiPackage, parent, created);
      selectedPath = HeaderManagerLookup.pathOf(created);
      legacy = "";
      rebuild();
      if (current.getTestElement() instanceof McpClientConfig config) {
        applyTo(config);
      }
    } catch (IllegalUserActionException | RuntimeException ex) {
      LOG.warn("Could not create HTTP Header Manager: {}", ex.toString());
      showError(messageOrDefault(ex));
    }
  }

  private HeaderManager newHeaderManager(GuiPackage guiPackage, String ownerName) {
    TestElement created = guiPackage.createTestElement(HEADER_PANEL);
    if (!(created instanceof HeaderManager manager)) {
      throw new IllegalStateException("JMeter did not create an HTTP Header Manager.");
    }
    HeaderManagerLookup.addHeaderLines(manager, legacy);
    manager.setName(managerNameFor(ownerName));
    return manager;
  }

  static String managerNameFor(String ownerName) {
    String owner = ownerName == null ? "" : ownerName.trim();
    if (owner.isEmpty()) {
      return "HTTP Header Manager";
    }
    return "HTTP Header Manager - " + owner;
  }

  /** Puts the new manager directly after this config, still under the same parent. */
  private static void placeBeside(
      GuiPackage guiPackage,
      JMeterTreeNode parent,
      JMeterTreeNode current,
      JMeterTreeNode created) {
    int index = parent.getIndex(current);
    if (index < 0 || parent.getIndex(created) == index + 1) {
      return;
    }
    guiPackage.getTreeModel().removeNodeFromParent(created);
    int insertAt = Math.min(index + 1, parent.getChildCount());
    guiPackage.getTreeModel().insertNodeInto(created, parent, insertAt);
  }

  private static void reveal(
      GuiPackage guiPackage, JMeterTreeNode parent, JMeterTreeNode created) {
    if (guiPackage.getTreeListener() == null || guiPackage.getTreeListener().getJTree() == null) {
      return;
    }
    JTree tree = guiPackage.getTreeListener().getJTree();
    tree.expandPath(new TreePath(parent.getPath()));
    tree.scrollPathToVisible(new TreePath(created.getPath()));
  }

  private void showError(String message) {
    JOptionPane.showMessageDialog(this, message, "Header Manager", JOptionPane.ERROR_MESSAGE);
  }

  private static String messageOrDefault(Exception ex) {
    String message = ex.getMessage();
    if (message == null || message.isBlank()) {
      return "Could not create the header manager.";
    }
    return message;
  }

  private static List<JMeterTreeNode> currentManagers() {
    GuiPackage guiPackage = GuiPackage.getInstance();
    if (guiPackage == null || guiPackage.getTreeModel() == null) {
      return List.of();
    }
    Object root = guiPackage.getTreeModel().getRoot();
    if (!(root instanceof JMeterTreeNode rootNode)) {
      return List.of();
    }
    return HeaderManagerLookup.listHeaderManagers(rootNode);
  }

  private static boolean containsPath(List<JMeterTreeNode> managers, List<String> path) {
    for (JMeterTreeNode node : managers) {
      if (HeaderManagerLookup.pathOf(node).equals(path)) {
        return true;
      }
    }
    return false;
  }

  private static final class HeaderManagerChoice {

    private final JMeterTreeNode node;
    private final String label;

    private HeaderManagerChoice(JMeterTreeNode node, String label) {
      this.node = node;
      this.label = label;
    }

    private static HeaderManagerChoice none() {
      return new HeaderManagerChoice(null, "(none)");
    }

    private static HeaderManagerChoice placeholder() {
      return new HeaderManagerChoice(null, "Test Plan > Thread Group > HTTP Header Manager");
    }

    private static HeaderManagerChoice of(JMeterTreeNode node) {
      return new HeaderManagerChoice(node, HeaderManagerLookup.displayPath(node));
    }

    private List<String> path() {
      if (node == null) {
        return List.of();
      }
      return new ArrayList<>(HeaderManagerLookup.pathOf(node));
    }

    @Override
    public String toString() {
      return label;
    }
  }
}
