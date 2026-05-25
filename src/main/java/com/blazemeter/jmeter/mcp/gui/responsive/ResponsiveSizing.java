package com.blazemeter.jmeter.mcp.gui.responsive;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.LayoutManager;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Caps column-based preferred widths and clears rigid horizontal minimum sizes so
 * plugin forms reflow when JMeter's property panel is narrowed.
 */
public final class ResponsiveSizing {

    /** Client property: do not cap columns on env-var {@link javax.swing.JTextArea}s. */
    public static final String ENV_VARS_AREA = "mcp.envVarsArea";

    public static final int MAX_TEXTFIELD_COLS = 26;

    public static final int MAX_TEXTAREA_COLS = 36;

    private ResponsiveSizing() {
    }

    /**
     * Visits {@code root} and descendants. Children are processed first so parent
     * {@link FlowLayout} sizes reflect updated text fields.
     */
    public static void applyTree(Component root) {
        if (root instanceof Container) {
            for (Component child : ((Container) root).getComponents()) {
                applyTree(child);
            }
        }

        if (root instanceof JTextField) {
            JTextField tf = (JTextField) root;
            if (tf.getColumns() > MAX_TEXTFIELD_COLS) {
                tf.setColumns(MAX_TEXTFIELD_COLS);
            }
            relaxHorizontalMinimumWidth(tf);
        } else if (root instanceof JTextArea) {
            JTextArea ta = (JTextArea) root;
            if (!Boolean.TRUE.equals(ta.getClientProperty(ENV_VARS_AREA))
                    && ta.getColumns() > MAX_TEXTAREA_COLS) {
                ta.setColumns(MAX_TEXTAREA_COLS);
            }
            relaxHorizontalMinimumWidth(ta);
        } else if (root instanceof JComboBox) {
            relaxHorizontalMinimumWidth((JComboBox<?>) root);
        } else if (root instanceof JPanel) {
            JPanel jp = (JPanel) root;
            LayoutManager lm = jp.getLayout();
            if (lm instanceof FlowLayout) {
                Dimension min = jp.getMinimumSize();
                jp.setMinimumSize(new Dimension(0, min.height));
            }
        }

        if (root instanceof JComponent) {
            String className = root.getClass().getName();
            if (className.endsWith("JLabeledTextField") || className.endsWith("JLabeledChoice")) {
                relaxHorizontalMinimumWidth((JComponent) root);
            }
        }
    }

    private static void relaxHorizontalMinimumWidth(JComponent component) {
        Dimension min = component.getMinimumSize();
        component.setMinimumSize(new Dimension(0, min.height));
    }
}
