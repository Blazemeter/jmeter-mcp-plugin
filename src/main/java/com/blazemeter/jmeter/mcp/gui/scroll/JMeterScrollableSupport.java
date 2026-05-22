package com.blazemeter.jmeter.mcp.gui.scroll;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/**
 * Default {@link Scrollable} behavior for JMeter property panels embedded in a
 * {@link javax.swing.JScrollPane}. Matches BlazeMeter HTTP/2 plugin GUIs.
 */
public final class JMeterScrollableSupport {

    private JMeterScrollableSupport() {
    }

    public static Dimension preferredViewportSize(JComponent component) {
        return component.getPreferredSize();
    }

    public static int scrollableUnitIncrement(Rectangle visibleRect, int orientation) {
        return orientation == SwingConstants.VERTICAL
                ? Math.max(1, visibleRect.height / 10)
                : Math.max(1, visibleRect.width / 10);
    }

    public static int scrollableBlockIncrement(Rectangle visibleRect, int orientation) {
        return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
    }

    public static boolean tracksViewportWidth() {
        return true;
    }

    public static boolean tracksViewportHeight() {
        return false;
    }
}
