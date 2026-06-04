package com.blazemeter.jmeter.mcp.gui.responsive;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.CardLayout;
import java.awt.Dimension;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class AdaptiveCardLayoutHostTest {

  @Test
  void shouldUseVisibleCardHeightWhenSyncAfterSwitchingCards() throws Exception {
    CardLayout layout = new CardLayout();
    JPanel cards = new JPanel(layout);

    JPanel shortCard = new JPanel();
    shortCard.setPreferredSize(new Dimension(200, 40));
    JPanel tallCard = new JPanel();
    tallCard.setPreferredSize(new Dimension(200, 300));

    cards.add(shortCard, "short");
    cards.add(tallCard, "tall");

    AdaptiveCardLayoutHost host = new AdaptiveCardLayoutHost(cards);

    SwingUtilities.invokeAndWait(
        () -> {
          layout.show(cards, "tall");
          host.sync();
        });
    int tallHeight = host.getCardPanel().getPreferredSize().height;

    SwingUtilities.invokeAndWait(
        () -> {
          layout.show(cards, "short");
          host.sync();
        });
    int shortHeight = host.getCardPanel().getPreferredSize().height;

    assertTrue(
        shortHeight < tallHeight,
        "visible short card should reduce host height below tallest-card baseline");
  }
}
