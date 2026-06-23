package com.blazemeter.jmeter.mcp.client;

import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.json.schema.JsonSchemaValidatorSupplier;

/**
 * Lazily resolves a shared {@link JsonSchemaValidator} via the SDK's
 * {@link JsonSchemaValidatorSupplier} {@code ServiceLoader} contract.
 */
public final class JsonSchemaValidators {

    private static volatile JsonSchemaValidator defaultValidator;

    private JsonSchemaValidators() {
    }

    public static JsonSchemaValidator getDefault() {
        JsonSchemaValidator local = defaultValidator;
        if (local == null) {
            synchronized (JsonSchemaValidators.class) {
                local = defaultValidator;
                if (local == null) {
                    local = resolve();
                    defaultValidator = local;
                }
            }
        }
        return local;
    }

    private static JsonSchemaValidator resolve() {
        return ServiceLoaderSupport.loadFirst(
                JsonSchemaValidatorSupplier.class,
                JsonSchemaValidators.class,
                JsonSchemaValidatorSupplier::get,
                "No JsonSchemaValidator implementation found on the classpath. "
                        + "The shaded plugin JAR is expected to ship 'mcp-json-jackson3'.");
    }
}
