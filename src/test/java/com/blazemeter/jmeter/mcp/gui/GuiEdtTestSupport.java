package com.blazemeter.jmeter.mcp.gui;

import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

/**
 * Runs assertions on the Swing EDT and waits for async GUI work to finish.
 */
final class GuiEdtTestSupport {

    private GuiEdtTestSupport() {
    }

    static void onEdtVoid(EdtRunnable action) throws Exception {
        onEdt(() -> {
            action.run();
            return null;
        });
    }

    static <T> T onEdt(EdtSupplier<T> action) throws Exception {
        final Object[] holder = new Object[1];
        final Exception[] error = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                holder[0] = action.get();
            } catch (Exception ex) {
                error[0] = ex;
            }
        });
        if (error[0] != null) {
            throw error[0];
        }
        @SuppressWarnings("unchecked")
        T value = (T) holder[0];
        return value;
    }

    static void awaitCondition(EdtBooleanSupplier condition, long timeoutSeconds) throws Exception {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds);
        while (System.currentTimeMillis() < deadline) {
            onEdtVoid(() -> { });
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Condition was not met within " + timeoutSeconds + "s");
    }

    @FunctionalInterface
    interface EdtBooleanSupplier {
        boolean getAsBoolean() throws Exception;
    }

    @FunctionalInterface
    interface EdtSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    interface EdtRunnable {
        void run() throws Exception;
    }
}
