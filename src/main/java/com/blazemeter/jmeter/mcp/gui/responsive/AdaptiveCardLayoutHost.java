package com.blazemeter.jmeter.mcp.gui.responsive;

import static java.awt.Component.LEFT_ALIGNMENT;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * {@link java.awt.CardLayout} reserves vertical space for the tallest card. This host ties the card
 * panel height to the <em>visible</em> card and adds vertical glue so extra space sits below the
 * form (same idea as HTTP/2 {@code AdaptiveTabbedPaneHeightHost}).
 */
public final class AdaptiveCardLayoutHost extends JPanel {

  private static final long serialVersionUID = 1L;

  private static final int MIN_CONTENT_HEIGHT_PX = 80;

  private final JPanel cardPanel;

  public AdaptiveCardLayoutHost(JPanel cardPanel) {
    this.cardPanel = cardPanel;
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setAlignmentX(LEFT_ALIGNMENT);
    cardPanel.setAlignmentX(LEFT_ALIGNMENT);
    add(cardPanel);
    add(Box.createVerticalGlue());
    sync();
  }

  public JPanel getCardPanel() {
    return cardPanel;
  }

  /** Call after {@link java.awt.CardLayout#show} so height matches the new card. */
  public void afterCardShown() {
    sync();
    SwingUtilities.invokeLater(
        () -> {
          sync();
          invalidate();
          revalidate();
          repaint();
          revalidateAncestors(this);
        });
  }

  public void sync() {
    cardPanel.setPreferredSize(null);
    cardPanel.setMinimumSize(null);
    cardPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

    Component visible = findVisibleCard();
    int selH = visible != null ? visible.getPreferredSize().height : 0;
    if (selH == 0) {
      return;
    }
    int h = Math.max(selH, MIN_CONTENT_HEIGHT_PX);
    Dimension panePref = cardPanel.getPreferredSize();
    int w = panePref.width > 0 ? panePref.width : 0;
    cardPanel.setPreferredSize(new Dimension(w, h));
    cardPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
    cardPanel.setMinimumSize(new Dimension(0, h));
  }

  @Override
  public Dimension getPreferredSize() {
    if (isPreferredSizeSet()) {
      return super.getPreferredSize();
    }
    Insets in = getInsets();
    Dimension cp = cardPanel.getPreferredSize();
    int width = cp.width + in.left + in.right;
    Container parent = getParent();
    if (parent != null) {
      int parentWidth = parent.getWidth();
      if (parentWidth > 0) {
        width = Math.max(width, parentWidth);
      }
    }
    return new Dimension(width, cp.height + in.top + in.bottom);
  }

  @Override
  public Dimension getMaximumSize() {
    Dimension pref = getPreferredSize();
    return new Dimension(Integer.MAX_VALUE, pref.height);
  }

  private Component findVisibleCard() {
    for (Component child : cardPanel.getComponents()) {
      if (child.isVisible()) {
        return child;
      }
    }
    return null;
  }

  public static void revalidateAncestors(JComponent component) {
    for (Container parent = component.getParent(); parent != null; parent = parent.getParent()) {
      parent.invalidate();
      if (parent instanceof JComponent) {
        ((JComponent) parent).revalidate();
      }
    }
  }
}
