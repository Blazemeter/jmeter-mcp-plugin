package com.blazemeter.jmeter.mcp;

import static org.assertj.swing.junit.runner.Formatter.testNameFrom;

import java.awt.Toolkit;
import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import org.assertj.core.util.Files;
import org.assertj.swing.junit.runner.FailureScreenshotTaker;
import org.junit.rules.MethodRule;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/**
 * JUnit 4 runner for AssertJ Swing GUI integration tests. Captures screenshots on failure under
 * {@code target/failsafe-reports/failed-gui-tests}. Bootstraps the Cacio virtual AWT toolkit so
 * tests render to an in-memory screen instead of opening real windows or grabbing the mouse.
 */
public class SwingTestRunner extends BlockJUnit4ClassRunner {

  static {
    // Must run before any AWT/Swing field initializers below. On Java 17+, awt.toolkit
    // system properties are ignored; CacioExtension installs CTCToolkit via ByteBuddy.
    CacioTestSupport.installVirtualToolkit();
  }

  private static final FailureScreenshotTaker SCREENSHOT_TAKER =
      new FailureScreenshotTaker(buildGuiScreenshotsFolder());

  private static File buildGuiScreenshotsFolder() {
    File folder = Paths.get("target", "failsafe-reports", "failed-gui-tests").toFile();
    Files.delete(folder);
    folder.mkdirs();
    return folder;
  }

  public SwingTestRunner(Class<?> klass) throws InitializationError {
    super(klass);
    ensureCacioToolkit();
  }

  private static void ensureCacioToolkit() throws InitializationError {
    String toolkit = Toolkit.getDefaultToolkit().getClass().getName();
    if (!toolkit.contains("cacio")) {
      throw new InitializationError(
          "Expected Cacio virtual AWT toolkit for GUI ITs, got: "
              + toolkit
              + ". Ensure cacio-tta is on the test classpath and Failsafe applies cacio.jvm.args.");
    }
  }

  @Override
  protected Statement methodInvoker(FrameworkMethod method, Object test) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          method.invokeExplosively(test);
        } catch (Throwable t) {
          SCREENSHOT_TAKER.saveScreenshot(
              testNameFrom(method.getDeclaringClass(), method.getMethod()));
          throw t;
        }
      }
    };
  }

  @Override
  protected List<MethodRule> rules(Object target) {
    return super.rules(target);
  }
}
