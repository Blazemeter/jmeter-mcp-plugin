package com.blazemeter.jmeter.mcp.sampler;

import com.blazemeter.jmeter.mcp.client.McpClientRegistry;
import com.blazemeter.jmeter.mcp.client.McpClientSettings;
import com.blazemeter.jmeter.mcp.client.TransportType;
import org.apache.jmeter.samplers.SampleResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpSamplerTest {

    private static final String CLIENT = "sampler-exception-test";

    @AfterEach
    void tearDown() {
        McpClientRegistry.getInstance().remove(CLIENT);
    }

    @Test
    void sampleMarksFailureWhenClientConfigMissing() {
        McpSampler sampler = new McpSampler();
        sampler.setProperty(McpSampler.CONFIG_NAME, "missing-config");
        sampler.setProperty(McpSampler.OPERATION, McpOperation.PING.name());

        SampleResult result = sampler.sample(null);

        assertFalse(result.isSuccessful());
        assertEquals("IllegalStateException", result.getResponseCode());
        assertTrue(result.getResponseMessage().contains("missing-config"));
        assertTrue(result.getResponseMessage().contains("MCP Client Config"));
    }

    @Test
    void sampleMarksFailureWhenClientSettingsInvalid() {
        McpClientSettings settings = new McpClientSettings();
        settings.setName(CLIENT);
        settings.setTransport(TransportType.STDIO);
        settings.setStdioCommand("   ");
        McpClientRegistry.getInstance().registerDeferred(CLIENT, settings);

        McpSampler sampler = new McpSampler();
        sampler.setProperty(McpSampler.CONFIG_NAME, CLIENT);
        sampler.setProperty(McpSampler.OPERATION, McpOperation.PING.name());

        SampleResult result = sampler.sample(null);

        assertFalse(result.isSuccessful());
        assertTrue(result.getResponseDataAsString().contains("command")
                || result.getResponseMessage().contains("command"));
    }

    @Test
    void requiredRejectsBlankToolName() {
        McpSampler sampler = new McpSampler();
        sampler.setProperty(McpSampler.TOOL_NAME, "  ");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> sampler.required(McpSampler.TOOL_NAME, "Tool Name"));
        assertTrue(ex.getMessage().contains("Tool Name"));
        assertTrue(ex.getMessage().contains("required"));
    }

    @Test
    void parseArgumentsRejectsInvalidJson() {
        McpSampler sampler = new McpSampler();
        sampler.setProperty(McpSampler.ARGUMENTS_JSON, "{not-valid-json");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                sampler::parseArguments);
        assertTrue(ex.getMessage().contains("JSON object"));
    }

    @Test
    void parseArgumentsAcceptsEmptyObject() {
        McpSampler sampler = new McpSampler();
        sampler.setProperty(McpSampler.ARGUMENTS_JSON, "");

        assertTrue(sampler.parseArguments().isEmpty());
    }

    @Test
    void parseArgumentsRejectsJsonArray() {
        McpSampler sampler = new McpSampler();
        sampler.setProperty(McpSampler.ARGUMENTS_JSON, "[1, 2]");

        assertThrows(IllegalArgumentException.class, sampler::parseArguments);
    }
}
