package com.blazemeter.jmeter.mcp.gui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Shared {@link GridBagLayout} helpers for plugin configuration GUIs.
 */
public final class GridBagForm {

    private GridBagForm() {
    }

    public static GridBagConstraints horizontalRowConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 4, 2, 4);
        c.weightx = 1;
        return c;
    }

    public static void addLabelAndField(JPanel panel, GridBagConstraints c, int row,
                                        String label, Component field) {
        c.gridy = row;
        c.gridx = 0;
        c.weightx = 0;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        panel.add(field, c);
    }

    /**
     * Label top-aligned for a tall field (e.g. env scroll pane).
     */
    public static void addLabelAndMultilineField(JPanel panel, GridBagConstraints c, int row,
                                                 String label, Component field) {
        c.gridy = row;
        c.gridx = 0;
        c.weightx = 0;
        c.weighty = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, c);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
    }

    public static long parseLong(String text, long fallback) {
        if (text == null) {
            return fallback;
        }
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
