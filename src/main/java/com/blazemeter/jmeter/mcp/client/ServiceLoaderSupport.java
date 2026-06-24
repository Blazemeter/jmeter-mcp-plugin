package com.blazemeter.jmeter.mcp.client;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.function.Function;

/**
 * Resolves SPI implementations from {@link ServiceLoader}.
 */
final class ServiceLoaderSupport {

  private ServiceLoaderSupport() {
  }

  static <S, T> T loadFirst(Class<S> supplierType, Class<?> anchor,
               Function<S, T> extract, String notFoundMessage) {
    ServiceLoader<S> loader = ServiceLoader.load(supplierType, anchor.getClassLoader());
    Iterator<S> it = loader.iterator();
    if (it.hasNext()) {
      T value = extract.apply(it.next());
      if (value != null) {
        return value;
      }
    }
    throw new IllegalStateException(notFoundMessage);
  }
}
