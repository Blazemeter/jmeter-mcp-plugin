package com.blazemeter.jmeter.mcp;

import kg.apc.emulators.EmulatorJmeterEngine;
import kg.apc.emulators.EmulatorThreadMonitor;
import kg.apc.emulators.TestJMeterUtils;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterThread;
import org.apache.jmeter.threads.JMeterThreadMonitor;
import org.apache.jmeter.threads.ListenerNotifier;
import org.apache.jorphan.collections.HashTree;

/**
 * Bootstraps a minimal JMeter runtime for GUI and config-element tests.
 */
public final class JMeterTestUtils {

    private static boolean jmeterEnvInitialized;

    private JMeterTestUtils() {
    }

    public static void setupJmeterEnv() {
        if (!jmeterEnvInitialized) {
            jmeterEnvInitialized = true;
            TestJMeterUtils.createJmeterEnv();
            StandardJMeterEngine engine = new EmulatorJmeterEngine();
            JMeterThreadMonitor monitor = new EmulatorThreadMonitor();
            JMeterContextService.getContext().setEngine(engine);
            HashTree tree = new HashTree();
            tree.add(new LoopController());
            JMeterThread thread = new JMeterThread(tree, monitor, new ListenerNotifier());
            thread.setThreadName("test thread");
            JMeterContextService.getContext().setThread(thread);
        }
    }
}
