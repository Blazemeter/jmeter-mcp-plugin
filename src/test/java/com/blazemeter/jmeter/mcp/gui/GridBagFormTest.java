package com.blazemeter.jmeter.mcp.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GridBagFormTest {

    @Test
    void parseLongReturnsFallbackForNullOrInvalid() {
        assertEquals(30_000L, GridBagForm.parseLong(null, 30_000L));
        assertEquals(30_000L, GridBagForm.parseLong("  ", 30_000L));
        assertEquals(30_000L, GridBagForm.parseLong("not-a-number", 30_000L));
    }

    @Test
    void parseLongTrimsAndParsesValidValue() {
        assertEquals(42L, GridBagForm.parseLong("  42  ", 0L));
    }
}
