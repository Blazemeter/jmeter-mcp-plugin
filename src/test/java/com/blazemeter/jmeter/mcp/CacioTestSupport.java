package com.blazemeter.jmeter.mcp;

/**
 * Installs Cacio's in-memory AWT toolkit for Swing tests.
 *
 * <p>On Java 17+, {@code awt.toolkit} / {@code java.awt.graphicsenv} system properties are ignored.
 * Loading {@code CacioExtension} runs its static initializer, which uses ByteBuddy to inject
 * {@code CTCToolkit} so AssertJ Swing does not open real windows or steal mouse focus.
 */
public final class CacioTestSupport {

  private static volatile boolean installed;

  private CacioTestSupport() {
  }

  public static void installVirtualToolkit() {
    if (installed) {
      return;
    }
    synchronized (CacioTestSupport.class) {
      if (installed) {
        return;
      }
      try {
        Class.forName("com.github.caciocavallosilano.cacio.ctc.junit.CacioExtension");
        installed = true;
      } catch (ClassNotFoundException | ExceptionInInitializerError | NoClassDefFoundError ex) {
        throw new IllegalStateException(
            "Failed to install Cacio virtual AWT toolkit for Swing tests", ex);
      }
    }
  }
}
