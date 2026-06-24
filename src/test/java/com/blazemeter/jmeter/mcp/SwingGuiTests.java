package com.blazemeter.jmeter.mcp;

import java.awt.Component;
import java.util.function.BooleanSupplier;
import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;
import org.assertj.swing.core.ComponentFinder;
import org.assertj.swing.exception.ComponentLookupException;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JOptionPaneFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.assertj.swing.timing.Timeout;

/** AssertJ Swing helpers for GUI integration tests (cacio / xvfb-safe). */
public final class SwingGuiTests {

  private static final Timeout DEFAULT_TIMEOUT = Timeout.timeout(10_000);

  private SwingGuiTests() {}

  public static void waitForEdt(FrameFixture frame) {
    frame.robot().waitForIdle();
  }

  public static void selectComboItem(FrameFixture frame, String comboName, Object item) {
    waitForEdt(frame);
    JComboBox<?> combo = findCombo(frame, comboName);
    combo.setSelectedItem(item);
    waitForEdt(frame);
    waitUntil(frame, "combo '" + comboName + "' selected", () -> item.equals(combo.getSelectedItem()));
  }

  public static void setEditableComboText(FrameFixture frame, String comboName, String text) {
    waitForEdt(frame);
    @SuppressWarnings("unchecked")
    JComboBox<String> combo = (JComboBox<String>) findCombo(frame, comboName);
    combo.setEditable(true);
    if (combo.getEditor() != null) {
      combo.getEditor().setItem(text);
    } else {
      combo.setSelectedItem(text);
      if (!text.equals(String.valueOf(combo.getSelectedItem()))) {
        combo.addItem(text);
        combo.setSelectedItem(text);
      }
    }
    waitForEdt(frame);
  }

  public static void setTextByName(FrameFixture frame, String name, String text) {
    waitForEdt(frame);
    Component component = findComponent(frame, name, Component.class);
    if (!(component instanceof JTextComponent field)) {
      throw new IllegalStateException("Not a text component: " + name);
    }
    field.setText(text);
    waitForEdt(frame);
  }

  public static JOptionPaneFixture waitForOptionPane(FrameFixture frame) {
    waitUntil(frame, "option pane present", () -> isOptionPaneShowing(frame));
    return new JOptionPaneFixture(
        frame.robot(),
        frame.robot().finder().findByType(javax.swing.JOptionPane.class, false));
  }

  public static void waitUntil(FrameFixture frame, String description, BooleanSupplier condition) {
    waitForEdt(frame);
    Pause.pause(
        new Condition(description) {
          @Override
          public boolean test() {
            return condition.getAsBoolean();
          }
        },
        DEFAULT_TIMEOUT);
  }

  public static boolean isOptionPaneShowing(FrameFixture frame) {
    try {
      frame.robot().finder().findByType(javax.swing.JOptionPane.class, false);
      return true;
    } catch (ComponentLookupException ignored) {
      return false;
    }
  }

  private static JComboBox<?> findCombo(FrameFixture frame, String comboName) {
    return findComponent(frame, comboName, JComboBox.class);
  }

  private static <T extends Component> T findComponent(FrameFixture frame, String name, Class<T> type) {
    ComponentFinder finder = frame.robot().finder();
    try {
      return finder.findByName(name, type, true);
    } catch (ComponentLookupException ignored) {
      return finder.findByName(name, type, false);
    }
  }
}
