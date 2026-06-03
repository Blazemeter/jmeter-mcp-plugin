package com.blazemeter.jmeter.mcp.client;

import java.util.Iterator;
import java.util.ServiceLoader;

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
        ServiceLoader<JsonSchemaValidatorSupplier> loader =
                ServiceLoader.load(JsonSchemaValidatorSupplier.class,
                        JsonSchemaValidators.class.getClassLoader());
        Iterator<JsonSchemaValidatorSupplier> it = loader.iterator();
        if (it.hasNext()) {
            JsonSchemaValidator validator = it.next().get();
            if (validator != null) {
                return validator;
            }
        }
        throw new IllegalStateException(
                "No JsonSchemaValidator implementation found on the classpath. "
                        + "The shaded plugin JAR is expected to ship 'mcp-json-jackson3'.");
    }
}
