package com.blazemeter.jmeter.mcp.gui;

import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.blazemeter.jmeter.mcp.gui.responsive.ResponsiveSizing;

/**
 * Factory for multi-line environment variable editors (KEY=value per line).
 */
public final class EnvVarsField {

    public static final int ROWS = 10;

    public static final int COLS = 40;

    /** Minimum scroll-pane height when JMeter's {@code makeScrollPane} shrinks the area. */
    public static final int MIN_SCROLL_HEIGHT_PX = 160;

    private EnvVarsField() {
    }

    public static JTextArea newTextArea() {
        JTextArea area = new JTextArea(ROWS, COLS);
        area.setLineWrap(false);
        area.putClientProperty(ResponsiveSizing.ENV_VARS_AREA, Boolean.TRUE);
        return area;
    }

    /**
     * JMeter {@link org.apache.jmeter.gui.AbstractJMeterGuiComponent#makeScrollPane} sets
     * preferred size to the text area minimum (often a single line). Apply a row-based height
     * after {@code makeScrollPane(area)}.
     */
    public static void applyScrollPaneSize(JScrollPane scroll, JTextArea area) {
        int height = preferredScrollHeight(area);
        scroll.setPreferredSize(new Dimension(0, height));
        scroll.setMinimumSize(new Dimension(0, height));
    }

    static int preferredScrollHeight(JTextArea area) {
        int lineHeight = area.getFontMetrics(area.getFont()).getHeight();
        int rows = area.getRows() > 0 ? area.getRows() : ROWS;
        Insets margin = area.getMargin();
        int fromRows = rows * lineHeight + margin.top + margin.bottom + 12;
        return Math.max(MIN_SCROLL_HEIGHT_PX, fromRows);
    }
}
