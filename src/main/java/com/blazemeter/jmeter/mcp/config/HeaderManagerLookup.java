package com.blazemeter.jmeter.mcp.config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.tree.TreeNode;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jorphan.collections.HashTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves an HTTP Header Manager referenced the same way a Module Controller references its
 * target: a path of element names from the test-plan tree root.
 *
 * <p>While a test is running the lookup uses the engine tree, so substituted header values on the
 * compiled plan are the ones that are sent. Otherwise it uses the open test plan in the GUI.
 */
public final class HeaderManagerLookup {

  private static final Logger LOG = LoggerFactory.getLogger(HeaderManagerLookup.class);

  private HeaderManagerLookup() {

  }

  /**
   * Header lines for {@code path}, or empty when the path does not identify a manager.
   * A present empty string means the manager was found and has no headers, or it is disabled.
   */
  public static Optional<String> findHeaderLines(List<String> path) {
    HeaderManager manager = findManager(path);
    if (manager == null) {
      return Optional.empty();
    }
    if (!manager.isEnabled()) {
      LOG.warn("Referenced HTTP Header Manager '{}' is disabled", manager.getName());
      return Optional.of("");
    }
    return Optional.of(toHeaderLines(manager));
  }

  public static List<JMeterTreeNode> listHeaderManagers(JMeterTreeNode root) {
    List<JMeterTreeNode> found = new ArrayList<>();
    if (root != null) {
      collect(root, found);
    }
    return found;
  }

  /** Names from the tree root through {@code node}, matching Module Controller node paths. */
  public static List<String> pathOf(JMeterTreeNode node) {
    TreeNode[] nodes = node.getPath();
    List<String> path = new ArrayList<>(nodes.length);
    for (TreeNode treeNode : nodes) {
      if (treeNode instanceof JMeterTreeNode jmeterNode) {
        String name = jmeterNode.getName();
        path.add(name == null ? "" : name);
      }
    }
    return path;
  }

  /** Path shown in the combo, without the hidden tree root. */
  public static String displayPath(JMeterTreeNode node) {
    List<String> path = pathOf(node);
    int start = path.size() > 1 ? 1 : 0;
    return String.join(" > ", path.subList(start, path.size()));
  }

  public static String toHeaderLines(HeaderManager manager) {
    StringBuilder lines = new StringBuilder();
    for (int i = 0; i < manager.size(); i++) {
      Header header = manager.get(i);
      if (header == null) {
        continue;
      }
      String name = header.getName() == null ? "" : header.getName().trim();
      if (name.isEmpty()) {
        continue;
      }
      if (lines.length() > 0) {
        lines.append('\n');
      }
      String value = header.getValue() == null ? "" : header.getValue().trim();
      lines.append(name).append('=').append(value);
    }
    return lines.toString();
  }

  /**
   * Copies {@code KEY=value} lines into {@code manager}, skipping blanks and {@code #} comments.
   */
  public static void addHeaderLines(HeaderManager manager, String lines) {
    if (manager == null || lines == null || lines.isBlank()) {
      return;
    }
    for (String line : lines.split("\\r?\\n")) {
      String trimmed = line.trim();
      if (trimmed.isEmpty() || trimmed.startsWith("#")) {
        continue;
      }
      int eq = trimmed.indexOf('=');
      if (eq <= 0) {
        continue;
      }
      String name = trimmed.substring(0, eq).trim();
      String value = trimmed.substring(eq + 1).trim();
      if (!name.isEmpty()) {
        manager.add(new Header(name, value));
      }
    }
  }

  static HeaderManager findInTree(JMeterTreeNode root, List<String> path) {
    if (root == null || path == null || path.size() < 2) {
      return null;
    }
    JMeterTreeNode node = traverse(root, path, 1);
    if (node == null || !(node.getTestElement() instanceof HeaderManager manager)) {
      return null;
    }
    return manager;
  }

  static HeaderManager findInHashTree(HashTree tree, List<String> path) {
    if (tree == null || path == null || path.isEmpty()) {
      return null;
    }
    return walk(tree, new ArrayList<>(), path);
  }

  private static HeaderManager findManager(List<String> path) {
    if (path == null || path.isEmpty()) {
      return null;
    }
    if (isTestRunning()) {
      return findInHashTree(runningTestTree(), path);
    }
    JMeterTreeNode root = guiRoot();
    if (root != null) {
      return findInTree(root, path);
    }
    return findInHashTree(runningTestTree(), path);
  }

  private static JMeterTreeNode traverse(JMeterTreeNode node, List<String> path, int level) {
    if (node == null || path.size() <= level) {
      return null;
    }
    String expected = path.get(level);
    for (int i = 0; i < node.getChildCount(); i++) {
      TreeNode childNode = node.getChildAt(i);
      if (!(childNode instanceof JMeterTreeNode child) || !expected.equals(child.getName())) {
        continue;
      }
      if (path.size() == level + 1) {
        return child;
      }
      JMeterTreeNode found = traverse(child, path, level + 1);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  private static HeaderManager walk(HashTree tree, List<String> ancestors, List<String> path) {
    for (Object item : tree.list()) {
      TestElement element = elementOf(item);
      if (element == null) {
        continue;
      }
      List<String> next = new ArrayList<>(ancestors);
      String name = element.getName();
      next.add(name == null ? "" : name);
      if (element instanceof HeaderManager manager && pathMatches(path, next)) {
        return manager;
      }
      HashTree children = tree.getTree(item);
      if (children != null) {
        HeaderManager found = walk(children, next, path);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static TestElement elementOf(Object item) {
    if (item instanceof JMeterTreeNode node) {
      return node.getTestElement();
    }
    if (item instanceof TestElement element) {
      return element;
    }
    return null;
  }

  /**
   * GUI paths include the hidden tree root. The engine HashTree starts at the test plan, so the
   * stored path is either equal to the engine path or one name longer.
   */
  static boolean pathMatches(List<String> stored, List<String> actual) {
    if (actual.isEmpty() || stored.size() < actual.size()) {
      return false;
    }
    if (stored.size() != actual.size() && stored.size() != actual.size() + 1) {
      return false;
    }
    int offset = stored.size() - actual.size();
    for (int i = 0; i < actual.size(); i++) {
      if (!stored.get(offset + i).equals(actual.get(i))) {
        return false;
      }
    }
    return true;
  }

  private static void collect(JMeterTreeNode node, List<JMeterTreeNode> found) {
    for (int i = 0; i < node.getChildCount(); i++) {
      TreeNode childNode = node.getChildAt(i);
      if (!(childNode instanceof JMeterTreeNode child)) {
        continue;
      }
      if (child.getTestElement() instanceof HeaderManager) {
        found.add(child);
      }
      collect(child, found);
    }
  }

  private static JMeterTreeNode guiRoot() {
    GuiPackage guiPackage = GuiPackage.getInstance();
    if (guiPackage == null || guiPackage.getTreeModel() == null) {
      return null;
    }
    Object root = guiPackage.getTreeModel().getRoot();
    if (root instanceof JMeterTreeNode node) {
      return node;
    }
    return null;
  }

  private static boolean isTestRunning() {
    StandardJMeterEngine engine = currentEngine();
    return engine != null && engine.isActive();
  }

  private static StandardJMeterEngine currentEngine() {
    if (JMeterContextService.getContext() == null) {
      return null;
    }
    return JMeterContextService.getContext().getEngine();
  }

  /**
   * The running plan is private on {@link StandardJMeterEngine}. Non-GUI runs have no
   * {@link GuiPackage} tree, so this is how a config element finds a sibling header manager.
   */
  private static HashTree runningTestTree() {
    StandardJMeterEngine engine = currentEngine();
    if (engine == null) {
      return null;
    }
    try {
      Field field = StandardJMeterEngine.class.getDeclaredField("test");
      field.setAccessible(true);
      Object value = field.get(engine);
      if (value instanceof HashTree tree) {
        return tree;
      }
    } catch (ReflectiveOperationException ex) {
      LOG.debug("Could not read the running test tree: {}", ex.toString());
    }
    return null;
  }
}
