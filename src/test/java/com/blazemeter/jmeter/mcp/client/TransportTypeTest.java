package com.blazemeter.jmeter.mcp.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TransportTypeTest {

    @Test
    void shouldDefaultToStdioWhenFromStringIsBlank() {
        assertEquals(TransportType.STDIO, TransportType.fromString(null));
        assertEquals(TransportType.STDIO, TransportType.fromString(""));
        assertEquals(TransportType.STDIO, TransportType.fromString("   "));
    }

    @Test
    void shouldParseTransportWhenFromStringUsesHyphensOrUnderscores() {
        assertEquals(TransportType.STDIO, TransportType.fromString("stdio"));
        assertEquals(TransportType.SSE, TransportType.fromString("SSE"));
        assertEquals(TransportType.STREAMABLE_HTTP,
                TransportType.fromString("streamable-http"));
    }

    @Test
    void shouldRejectUnknownTransportWhenFromString() {
        assertThrows(IllegalArgumentException.class,
                () -> TransportType.fromString("websocket"));
    }
}
