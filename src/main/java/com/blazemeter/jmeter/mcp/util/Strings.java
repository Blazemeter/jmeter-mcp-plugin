package com.blazemeter.jmeter.mcp.util;

/** Small string helpers shared by config elements, factories, and GUIs. */
public final class Strings {

  private Strings() {

  }

  public static String blankToDefault(String value, String fallback) {
    return (value == null || value.isBlank()) ? fallback : value;
  }

  public static String trimToDefault(String value, String fallback) {
    if (value == null) {
      return fallback;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? fallback : trimmed;
  }
}
