package com.blazemeter.jmeter.mcp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.blazemeter.jmeter.mcp.JMeterTestUtils;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jorphan.collections.HashTree;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HeaderManagerLookupTest {

  @BeforeAll
  static void jmeterEnv() {
    JMeterTestUtils.setupJmeterEnv();
  }

  @Test
  void shouldFindHeaderManagerByModuleControllerStylePath() {
    HeaderManager direct = manager("Auth Headers", "Authorization", "Bearer token");
    HeaderManager nested = manager("Other", "X-Trace", "1");
    JMeterTreeNode root = node("Root");
    JMeterTreeNode plan = node("Test Plan");
    JMeterTreeNode group = node("Thread Group");
    root.add(plan);
    plan.add(nodeFor(direct));
    plan.add(group);
    group.add(nodeFor(nested));

    HeaderManager found = HeaderManagerLookup.findInTree(
        root, List.of("Root", "Test Plan", "Auth Headers"));
    assertEquals(direct, found);
    assertEquals(
        "Authorization=Bearer token", HeaderManagerLookup.toHeaderLines(found));
    assertEquals(
        nested,
        HeaderManagerLookup.findInTree(
            root, List.of("Root", "Test Plan", "Thread Group", "Other")));
    assertNull(HeaderManagerLookup.findInTree(root, List.of("Root", "Test Plan")));
  }

  @Test
  void shouldListManagersAndSkipHiddenRootInDisplayPath() {
    JMeterTreeNode root = node("Root");
    JMeterTreeNode plan = node("Test Plan");
    JMeterTreeNode managerNode = nodeFor(manager("HTTP Header Manager", "A", "1"));
    root.add(plan);
    plan.add(managerNode);

    assertEquals(List.of(managerNode), HeaderManagerLookup.listHeaderManagers(root));
    assertEquals(
        List.of("Root", "Test Plan", "HTTP Header Manager"),
        HeaderManagerLookup.pathOf(managerNode));
    assertEquals(
        "Test Plan > HTTP Header Manager", HeaderManagerLookup.displayPath(managerNode));
  }

  @Test
  void shouldCopyLegacyLinesAndIgnoreComments() {
    HeaderManager manager = new HeaderManager();
    HeaderManagerLookup.addHeaderLines(
        manager, "# comment\nAuthorization=Bearer token\n\nconfirmation-mode=DISABLE\nno-equals");
    assertEquals(
        "Authorization=Bearer token\nconfirmation-mode=DISABLE",
        HeaderManagerLookup.toHeaderLines(manager));
  }

  @Test
  void shouldMatchEngineTreePathThatOmitsTheHiddenRoot() {
    HeaderManager manager = manager("HTTP Header Manager", "Authorization", "Bearer token");
    HashTree tree = planContaining(manager);
    assertEquals(
        manager,
        HeaderManagerLookup.findInHashTree(
            tree, List.of("Root", "Test Plan", "HTTP Header Manager")));
    assertNull(
        HeaderManagerLookup.findInHashTree(tree, List.of("Root", "Test Plan", "Other")));
  }

  @Test
  void shouldReturnEmptyLinesWhenReferencedManagerIsDisabled() throws Exception {
    HeaderManager manager = manager("HTTP Header Manager", "Authorization", "Bearer token");
    manager.setEnabled(false);
    withEngineTree(planContaining(manager), () -> assertEquals(
        Optional.of(""),
        HeaderManagerLookup.findHeaderLines(
            List.of("Root", "Test Plan", "HTTP Header Manager"))));
  }

  private static void withEngineTree(HashTree tree, Runnable action) throws Exception {
    Field testField = StandardJMeterEngine.class.getDeclaredField("test");
    testField.setAccessible(true);
    StandardJMeterEngine engine = JMeterContextService.getContext().getEngine();
    Object previous = testField.get(engine);
    testField.set(engine, tree);
    try {
      action.run();
    } finally {
      testField.set(engine, previous);
    }
  }

  private static HashTree planContaining(HeaderManager manager) {
    TestElement plan = new ConfigTestElement();
    plan.setName("Test Plan");
    HashTree root = new HashTree();
    root.add(plan).add(manager);
    return root;
  }

  private static HeaderManager manager(String elementName, String headerName, String value) {
    HeaderManager manager = new HeaderManager();
    manager.setName(elementName);
    manager.add(new Header(headerName, value));
    return manager;
  }

  private static JMeterTreeNode node(String name) {
    TestElement element = new ConfigTestElement();
    element.setName(name);
    return new JMeterTreeNode(element, null);
  }

  private static JMeterTreeNode nodeFor(HeaderManager manager) {
    return new JMeterTreeNode(manager, null);
  }
}
