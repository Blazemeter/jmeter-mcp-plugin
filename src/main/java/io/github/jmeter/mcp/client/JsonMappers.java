package io.github.jmeter.mcp.client;

import java.util.Iterator;
import java.util.ServiceLoader;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.McpJsonMapperSupplier;

/**
 * Lazily resolves a shared {@link McpJsonMapper} via the SDK's
 * {@link McpJsonMapperSupplier} {@code ServiceLoader} contract.
 *
 * <p>The shaded plugin JAR ships the {@code mcp-json-jackson3} service
 * registration, so {@link #getDefault()} returns a Jackson 3-backed mapper at
 * runtime without any additional configuration.
 */
public final class JsonMappers {

    private static volatile McpJsonMapper defaultMapper;

    private JsonMappers() {
    }

    public static McpJsonMapper getDefault() {
        McpJsonMapper local = defaultMapper;
        if (local == null) {
            synchronized (JsonMappers.class) {
                local = defaultMapper;
                if (local == null) {
                    local = resolve();
                    defaultMapper = local;
                }
            }
        }
        return local;
    }

    private static McpJsonMapper resolve() {
        ServiceLoader<McpJsonMapperSupplier> loader =
                ServiceLoader.load(McpJsonMapperSupplier.class,
                        JsonMappers.class.getClassLoader());
        Iterator<McpJsonMapperSupplier> it = loader.iterator();
        if (it.hasNext()) {
            McpJsonMapper mapper = it.next().get();
            if (mapper != null) {
                return mapper;
            }
        }
        throw new IllegalStateException(
                "No McpJsonMapper implementation found on the classpath. "
                        + "The 'jmeter-mcp-plugin' shaded JAR is expected to "
                        + "ship 'mcp-json-jackson3'; check that it was bundled "
                        + "by the shade plugin's ServicesResourceTransformer.");
    }
}
