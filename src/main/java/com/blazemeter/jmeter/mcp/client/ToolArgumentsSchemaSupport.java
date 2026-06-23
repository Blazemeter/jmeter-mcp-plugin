package com.blazemeter.jmeter.mcp.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * Converts MCP tool {@link McpSchema.JsonSchema} to maps, generates starter argument
 * JSON, and validates user input against the schema.
 */
public final class ToolArgumentsSchemaSupport {

    private ToolArgumentsSchemaSupport() {
    }

    public record ValidationResult(boolean valid, String message) {
    }

    public static Map<String, Object> toSchemaMap(McpSchema.JsonSchema schema) throws IOException {
        if (schema == null) {
            return Map.of("type", "object");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = JsonMappers.getDefault().convertValue(schema, Map.class);
        return map;
    }

    public static String toPrettySchemaJson(McpSchema.JsonSchema schema) throws IOException {
        return JsonMappers.writeValueAsPrettyString(toSchemaMap(schema));
    }

    @SuppressWarnings("unchecked")
    public static String generateSampleJson(Map<String, Object> schema) throws IOException {
        Object sample = generateValue(schema);
        return JsonMappers.writeValueAsPrettyString(sample);
    }

    public static ValidationResult validateArgumentsJson(String argumentsJson,
                                                         Map<String, Object> schema) {
        if (schema == null || schema.isEmpty()) {
            return new ValidationResult(false, "No input schema loaded");
        }
        String raw = argumentsJson == null ? "" : argumentsJson.trim();
        if (raw.isEmpty()) {
            raw = "{}";
        }
        try {
            Object parsed = JsonMappers.getDefault().readValue(raw, Object.class);
            if (!(parsed instanceof Map)) {
                return new ValidationResult(false, "Arguments must be a JSON object");
            }
            JsonSchemaValidator.ValidationResponse response =
                    JsonSchemaValidators.getDefault().validate(schema, parsed);
            if (response.valid()) {
                return new ValidationResult(true, "Arguments match the tool input schema");
            }
            return new ValidationResult(false,
                    response.errorMessage() != null ? response.errorMessage() : "Validation failed");
        } catch (IOException | RuntimeException ex) {
            return new ValidationResult(false, "Invalid JSON: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    static Object generateValue(Map<String, Object> schema) {
        if (schema == null) {
            return Map.of();
        }
        Object enumValues = schema.get("enum");
        if (enumValues instanceof List<?> list && !list.isEmpty()) {
            return list.get(0);
        }
        String type = stringProp(schema, "type");
        if ("object".equals(type) || schema.containsKey("properties")) {
            return generateObject(schema);
        }
        return switch (type) {
            case "string" -> "";
            case "integer", "number" -> 0;
            case "boolean" -> false;
            case "array" -> generateArray(schema);
            case "null" -> null;
            default -> Map.of();
        };
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> generateObject(Map<String, Object> schema) {
        Object propertiesObj = schema.get("properties");
        if (!(propertiesObj instanceof Map<?, ?> properties) || properties.isEmpty()) {
            return Map.of();
        }
        Set<String> keys = keysToPopulate(schema, properties);
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : keys) {
            Object propSchema = properties.get(key);
            if (propSchema instanceof Map<?, ?> propMap) {
                result.put(key, generateValue((Map<String, Object>) propMap));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> generateArray(Map<String, Object> schema) {
        Object items = schema.get("items");
        if (items instanceof Map<?, ?> itemSchema) {
            return List.of(generateValue((Map<String, Object>) itemSchema));
        }
        return List.of();
    }

    private static Set<String> keysToPopulate(Map<String, Object> schema, Map<?, ?> properties) {
        List<String> required = requiredList(schema);
        if (!required.isEmpty()) {
            return new LinkedHashSet<>(required);
        }
        Set<String> keys = new LinkedHashSet<>();
        for (Object key : properties.keySet()) {
            if (key != null) {
                keys.add(key.toString());
            }
        }
        return keys;
    }

    @SuppressWarnings("unchecked")
    private static List<String> requiredList(Map<String, Object> schema) {
        Object required = schema.get("required");
        if (!(required instanceof List<?> list)) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (Object item : list) {
            if (item != null) {
                names.add(item.toString());
            }
        }
        return names;
    }

    private static String stringProp(Map<String, Object> schema, String key) {
        Object value = schema.get(key);
        return value == null ? "" : value.toString();
    }
}
