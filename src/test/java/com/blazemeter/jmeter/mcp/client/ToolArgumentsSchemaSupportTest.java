package com.blazemeter.jmeter.mcp.client;

import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolArgumentsSchemaSupportTest {

    @Test
    void shouldDefaultToObjectSchemaWhenJsonSchemaIsNull() throws Exception {
        Map<String, Object> map = ToolArgumentsSchemaSupport.toSchemaMap(null);
        assertEquals("object", map.get("type"));
        assertTrue(ToolArgumentsSchemaSupport.toPrettySchemaJson(null).contains("object"));
    }

    @Test
    void shouldGenerateRequiredPropertiesWhenSchemaListsRequiredFields() throws Exception {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "name", Map.of("type", "string"),
                        "count", Map.of("type", "integer")),
                "required", List.of("name"));

        String json = ToolArgumentsSchemaSupport.generateSampleJson(schema);
        assertTrue(json.contains("\"name\""));
        assertFalse(json.contains("\"count\""));
    }

    @Test
    void shouldGenerateAllPropertiesWhenNoRequiredList() throws Exception {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "a", Map.of("type", "string"),
                        "b", Map.of("type", "boolean")));

        String json = ToolArgumentsSchemaSupport.generateSampleJson(schema);
        assertTrue(json.contains("\"a\""));
        assertTrue(json.contains("\"b\""));
    }

    @Test
    void shouldUseFirstEnumValueWhenPropertyHasEnum() throws Exception {
        Map<String, Object> schema = Map.of("enum", List.of("red", "blue"));
        Object value = ToolArgumentsSchemaSupport.generateValue(schema);
        assertEquals("red", value);
    }

    @Test
    void shouldGenerateTypedScalarsAndArrays() throws Exception {
        assertEquals("", ToolArgumentsSchemaSupport.generateValue(Map.of("type", "string")));
        assertEquals(0, ToolArgumentsSchemaSupport.generateValue(Map.of("type", "integer")));
        assertEquals(false, ToolArgumentsSchemaSupport.generateValue(Map.of("type", "boolean")));
        assertNull(ToolArgumentsSchemaSupport.generateValue(Map.of("type", "null")));

        @SuppressWarnings("unchecked")
        List<Object> array = (List<Object>) ToolArgumentsSchemaSupport.generateValue(Map.of(
                "type", "array",
                "items", Map.of("type", "string")));
        assertEquals(1, array.size());
        assertEquals("", array.get(0));
    }

    @Test
    void shouldReturnEmptyObjectWhenSchemaHasNoProperties() {
        Object value = ToolArgumentsSchemaSupport.generateValue(Map.of("type", "object"));
        assertEquals(Map.of(), value);
    }

    @Test
    void shouldValidateArgumentsWhenJsonMatchesInputSchema() throws Exception {
        McpSchema.JsonSchema schema = new McpSchema.JsonSchema(
                "object",
                Map.of("message", Map.of("type", "string")),
                List.of("message"),
                false,
                null,
                null);
        Map<String, Object> schemaMap = ToolArgumentsSchemaSupport.toSchemaMap(schema);

        var valid = ToolArgumentsSchemaSupport.validateArgumentsJson(
                "{\"message\":\"hello\"}", schemaMap);
        assertTrue(valid.valid());

        var invalid = ToolArgumentsSchemaSupport.validateArgumentsJson(
                "{}", schemaMap);
        assertFalse(invalid.valid());
    }

    @Test
    void shouldTreatBlankArgumentsAsEmptyObjectWhenValidating() throws Exception {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of("opt", Map.of("type", "string")));

        var result = ToolArgumentsSchemaSupport.validateArgumentsJson("   ", schema);
        assertTrue(result.valid());
    }

    @Test
    void shouldRejectWhenSchemaNotLoaded() {
        var result = ToolArgumentsSchemaSupport.validateArgumentsJson("{}", null);
        assertFalse(result.valid());
        assertTrue(result.message().contains("No input schema"));
    }

    @Test
    void shouldRejectNonObjectArgumentsWhenValidating() {
        var result = ToolArgumentsSchemaSupport.validateArgumentsJson(
                "[]", Map.of("type", "object"));
        assertFalse(result.valid());
        assertTrue(result.message().contains("JSON object"));
    }

    @Test
    void shouldRejectMalformedJsonWhenValidating() {
        var result = ToolArgumentsSchemaSupport.validateArgumentsJson(
                "{not json}", Map.of("type", "object"));
        assertFalse(result.valid());
        assertTrue(result.message().contains("Invalid JSON"));
    }

    @Test
    void shouldGenerateNumberTypeWhenSchemaSpecifiesNumber() {
        assertEquals(0, ToolArgumentsSchemaSupport.generateValue(Map.of("type", "number")));
    }

    @Test
    void shouldRejectWhenSchemaMapIsEmpty() {
        var result = ToolArgumentsSchemaSupport.validateArgumentsJson("{}", Map.of());
        assertFalse(result.valid());
        assertTrue(result.message().contains("No input schema"));
    }

    @Test
    void shouldGenerateNestedObjectWhenPropertiesPresentWithoutExplicitType() throws Exception {
        Map<String, Object> schema = Map.of(
                "properties", Map.of("id", Map.of("type", "integer")));

        String json = ToolArgumentsSchemaSupport.generateSampleJson(schema);
        assertTrue(json.contains("\"id\""));
    }

    @Test
    void shouldGenerateEmptyArrayWhenItemsSchemaMissing() {
        @SuppressWarnings("unchecked")
        List<Object> array = (List<Object>) ToolArgumentsSchemaSupport.generateValue(
                Map.of("type", "array"));
        assertTrue(array.isEmpty());
    }
}
