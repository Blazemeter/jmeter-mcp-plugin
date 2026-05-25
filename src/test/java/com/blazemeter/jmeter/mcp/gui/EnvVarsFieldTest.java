package com.blazemeter.jmeter.mcp.gui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JTextArea;

import org.junit.jupiter.api.Test;

class EnvVarsFieldTest {

    @Test
    void preferredScrollHeightIsAtLeastMinimum() {
        JTextArea area = EnvVarsField.newTextArea();
        assertTrue(EnvVarsField.preferredScrollHeight(area) >= EnvVarsField.MIN_SCROLL_HEIGHT_PX);
    }
}
