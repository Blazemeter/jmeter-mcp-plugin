package com.blazemeter.jmeter.mcp.server.gui;

import java.awt.BorderLayout;
import java.util.Objects;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SwingWorker;

import com.blazemeter.jmeter.commons.BlazemeterLabsLogo;

import com.blazemeter.jmeter.mcp.gui.EnvVarsField;
import com.blazemeter.jmeter.mcp.gui.GridBagForm;
import com.blazemeter.jmeter.mcp.gui.PluginGuiConstants;
import com.blazemeter.jmeter.mcp.McpRuntimeCleanup;
import com.blazemeter.jmeter.mcp.gui.responsive.ResponsiveSizing;
import com.blazemeter.jmeter.mcp.gui.scroll.JMeterScrollableSupport;
import com.blazemeter.jmeter.mcp.server.DefaultMcpServerControl;
import com.blazemeter.jmeter.mcp.server.McpServerControl;
import com.blazemeter.jmeter.mcp.server.McpServerLaunchSettings;
import com.blazemeter.jmeter.mcp.server.McpServerProcess;
import com.blazemeter.jmeter.mcp.util.Strings;
import org.apache.jmeter.config.gui.AbstractConfigGui;
import org.apache.jmeter.testelement.TestElement;

/**
 * Swing GUI for {@link McpServerProcess}.
 */
public class McpServerProcessGui extends AbstractConfigGui implements Scrollable {

    private static final long serialVersionUID = 1L;

    private final JTextField commandField = new JTextField(20);
    private final JTextField argsField = new JTextField(40);
    private final JTextArea envArea = EnvVarsField.newTextArea();
    private final JTextField readyHostField = new JTextField(15);
    private final JTextField readyPortField = new JTextField(8);
    private final JTextField startupWaitField = new JTextField(8);
    private final JCheckBox keepServerRunningAfterTestCheck =
            new JCheckBox("Keep server running after test ends");

    private final JButton startButton = new JButton("Start");
    private final JButton stopButton = new JButton("Stop");
    private final JLabel serverStatusLabel = new JLabel("Not running");

    private final McpServerControl serverControl;

    public McpServerProcessGui() {
        this(DefaultMcpServerControl.getInstance());
    }

    McpServerProcessGui(McpServerControl serverControl) {
        this.serverControl = Objects.requireNonNull(serverControl, "serverControl");
        init();
    }

    @Override
    public String getStaticLabel() {
        return "bzm - MCP Server Process";
    }

    @Override
    public String getLabelResource() {
        return "mcp_server_process_title";
    }

    private void init() {
        McpRuntimeCleanup.ensureRegistered();
        setLayout(new BorderLayout(0, 5));
        setBorder(makeBorder());
        add(makeTitlePanel(), BorderLayout.NORTH);

        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        addStackSection(stack, buildLaunchPanel());
        addStackSection(stack, buildManualPanel());

        JPanel center = new JPanel(new BorderLayout(0, 5));
        center.add(stack, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(new BlazemeterLabsLogo(PluginGuiConstants.PLUGIN_REPOSITORY_URL), BorderLayout.PAGE_END);

        ResponsiveSizing.applyTree(this);

        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());
        clearGui();
        assignComponentNames();
    }

    private static void addStackSection(JPanel stack, Component section) {
        if (section instanceof JComponent) {
            ((JComponent) section).setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        stack.add(section);
    }

    private JPanel buildLaunchPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Server launch"));
        GridBagConstraints c = GridBagForm.horizontalRowConstraints();
        GridBagForm.addLabelAndField(panel, c, 0, "Command:", commandField);
        GridBagForm.addLabelAndField(panel, c, 1, "Args (space separated):", argsField);
        JScrollPane envScroll = makeScrollPane(envArea);
        EnvVarsField.applyScrollPaneSize(envScroll, envArea);
        GridBagForm.addLabelAndMultilineField(panel, c, 2, "Env (KEY=value per line):", envScroll);
        GridBagForm.addLabelAndField(panel, c, 3, "Ready host:", readyHostField);
        GridBagForm.addLabelAndField(panel, c, 4, "Ready port:", readyPortField);
        GridBagForm.addLabelAndField(panel, c, 5, "Startup wait (ms):", startupWaitField);
        GridBagForm.addLabelAndField(panel, c, 6, "", keepServerRunningAfterTestCheck);
        return panel;
    }

    private JPanel buildManualPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Manual server control"));
        GridBagConstraints c = GridBagForm.horizontalRowConstraints();
        JPanel buttons = new JPanel();
        buttons.add(startButton);
        buttons.add(stopButton);
        GridBagForm.addLabelAndField(panel, c, 0, "", buttons);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 3;
        serverStatusLabel.setOpaque(false);
        panel.add(serverStatusLabel, c);
        return panel;
    }

    private void assignComponentNames() {
        commandField.setName("mcpServerProcess.command");
        argsField.setName("mcpServerProcess.args");
        envArea.setName("mcpServerProcess.env");
        readyHostField.setName("mcpServerProcess.readyHost");
        readyPortField.setName("mcpServerProcess.readyPort");
        startupWaitField.setName("mcpServerProcess.startupWait");
        keepServerRunningAfterTestCheck.setName("mcpServerProcess.keepServerRunningAfterTest");
        startButton.setName("mcpServerProcess.start");
        stopButton.setName("mcpServerProcess.stop");
        serverStatusLabel.setName("mcpServerProcess.status");
        refreshServerStatus();
    }

    private void startServer() {
        McpServerLaunchSettings settings = readLaunchSettingsFromGui();
        startButton.setEnabled(false);
        stopButton.setEnabled(false);
        serverStatusLabel.setText("Starting…");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                serverControl.start(settings);
                return null;
            }

            @Override
            protected void done() {
                startButton.setEnabled(true);
                stopButton.setEnabled(true);
                try {
                    get();
                    refreshServerStatus();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    serverStatusLabel.setText("Not running");
                    JOptionPane.showMessageDialog(McpServerProcessGui.this,
                            cause.getMessage(),
                            "Start failed",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void stopServer() {
        startButton.setEnabled(false);
        stopButton.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                serverControl.stop();
                return null;
            }

            @Override
            protected void done() {
                startButton.setEnabled(true);
                stopButton.setEnabled(true);
                refreshServerStatus();
            }
        }.execute();
    }

    private void refreshServerStatus() {
        String host = Strings.trimToDefault(readyHostField.getText(), "localhost");
        int port = (int) GridBagForm.parseLong(readyPortField.getText(), 3001L);
        Long pid = serverControl.getManagedProcessPid();
        if (pid != null) {
            serverStatusLabel.setText("Running at " + host + ":" + port
                    + " (pid " + pid + ", reused on test run)");
            stopButton.setEnabled(true);
            return;
        }
        if (serverControl.isPortOpen(host, port, 500)) {
            serverStatusLabel.setText("Reachable at " + host + ":" + port + " (not managed here)");
            stopButton.setEnabled(false);
            return;
        }
        serverStatusLabel.setText("Not running");
        stopButton.setEnabled(false);
    }

    private McpServerLaunchSettings readLaunchSettingsFromGui() {
        return new McpServerLaunchSettings(
                commandField.getText(),
                argsField.getText(),
                envArea.getText(),
                Strings.trimToDefault(readyHostField.getText(), "localhost"),
                (int) GridBagForm.parseLong(readyPortField.getText(), 3001L),
                GridBagForm.parseLong(startupWaitField.getText(), 60_000L));
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return JMeterScrollableSupport.preferredViewportSize(this);
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return JMeterScrollableSupport.scrollableUnitIncrement(visibleRect, orientation);
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return JMeterScrollableSupport.scrollableBlockIncrement(visibleRect, orientation);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return JMeterScrollableSupport.tracksViewportWidth();
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return JMeterScrollableSupport.tracksViewportHeight();
    }

    @Override
    public TestElement createTestElement() {
        McpServerProcess element = new McpServerProcess();
        modifyTestElement(element);
        return element;
    }

    @Override
    public void modifyTestElement(TestElement element) {
        super.configureTestElement(element);
        if (!(element instanceof McpServerProcess)) {
            return;
        }
        McpServerProcess cfg = (McpServerProcess) element;
        cfg.setProperty(McpServerProcess.COMMAND, commandField.getText());
        cfg.setProperty(McpServerProcess.ARGS, argsField.getText());
        cfg.setProperty(McpServerProcess.ENV, envArea.getText());
        cfg.setProperty(McpServerProcess.READY_HOST, readyHostField.getText());
        cfg.setProperty(McpServerProcess.READY_PORT,
                GridBagForm.parseLong(readyPortField.getText(), 3001L));
        cfg.setProperty(McpServerProcess.STARTUP_WAIT_MS,
                GridBagForm.parseLong(startupWaitField.getText(), 60_000L));
        cfg.setProperty(McpServerProcess.KEEP_SERVER_RUNNING_AFTER_TEST,
                keepServerRunningAfterTestCheck.isSelected());
    }

    @Override
    public void configure(TestElement element) {
        super.configure(element);
        if (!(element instanceof McpServerProcess)) {
            return;
        }
        McpServerProcess cfg = (McpServerProcess) element;
        commandField.setText(cfg.getPropertyAsString(McpServerProcess.COMMAND, ""));
        argsField.setText(cfg.getPropertyAsString(McpServerProcess.ARGS, ""));
        envArea.setText(cfg.getPropertyAsString(McpServerProcess.ENV, ""));
        readyHostField.setText(cfg.getPropertyAsString(McpServerProcess.READY_HOST, "localhost"));
        readyPortField.setText(String.valueOf(
                cfg.getPropertyAsLong(McpServerProcess.READY_PORT, 3001L)));
        startupWaitField.setText(String.valueOf(
                cfg.getPropertyAsLong(McpServerProcess.STARTUP_WAIT_MS, 60_000L)));
        keepServerRunningAfterTestCheck.setSelected(
                cfg.getPropertyAsBoolean(McpServerProcess.KEEP_SERVER_RUNNING_AFTER_TEST, true));
        refreshServerStatus();
    }

    @Override
    public void clearGui() {
        super.clearGui();
        commandField.setText("npx");
        argsField.setText("-y @modelcontextprotocol/server-everything sse");
        envArea.setText("");
        readyHostField.setText("localhost");
        readyPortField.setText("3001");
        startupWaitField.setText("60000");
        keepServerRunningAfterTestCheck.setSelected(true);
        refreshServerStatus();
    }
}
