package com.blazemeter.jmeter.mcp.client;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.McpJsonMapperSupplier;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import java.io.IOException;
import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Lazily resolves a shared {@link McpJsonMapper} via the SDK's {@link McpJsonMapperSupplier} {@code
 * ServiceLoader} contract.
 *
 * <p>The shaded plugin JAR ships the {@code mcp-json-jackson3} service registration, so {@link
 * #getDefault()} returns a Jackson 3-backed mapper at runtime without any additional configuration.
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

  /**
   * Serializes {@code value} as indented JSON for human-readable JMeter response bodies. Falls back
   * to compact JSON if the mapper is not Jackson.
   */
  public static String writeValueAsPrettyString(Object value) throws IOException {
    if (value == null) {
      return "null";
    }
    McpJsonMapper mapper = getDefault();
    if (mapper instanceof JacksonMcpJsonMapper jacksonMapper) {
      return jacksonMapper
          .getJsonMapper()
          .writerWithDefaultPrettyPrinter()
          .writeValueAsString(value);
    }
    return mapper.writeValueAsString(value);
  }

  private static McpJsonMapper resolve() {
    ServiceLoader<McpJsonMapperSupplier> loader =
        ServiceLoader.load(McpJsonMapperSupplier.class, JsonMappers.class.getClassLoader());
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
