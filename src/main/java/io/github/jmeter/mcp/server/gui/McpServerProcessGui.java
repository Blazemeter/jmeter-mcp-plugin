package io.github.jmeter.mcp.server.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import io.github.jmeter.mcp.server.McpServerProcess;
import org.apache.jmeter.config.gui.AbstractConfigGui;
import org.apache.jmeter.testelement.TestElement;

/**
 * Swing GUI for {@link McpServerProcess}.
 */
public class McpServerProcessGui extends AbstractConfigGui {

    private static final long serialVersionUID = 1L;

    private final JTextField commandField = new JTextField(20);
    private final JTextField argsField = new JTextField(40);
    private final JTextArea envArea = new JTextArea(4, 30);
    private final JTextField readyHostField = new JTextField(15);
    private final JTextField readyPortField = new JTextField(8);
    private final JTextField startupWaitField = new JTextField(8);

    public McpServerProcessGui() {
        init();
    }

    @Override
    public String getStaticLabel() {
        return "MCP Server Process";
    }

    @Override
    public String getLabelResource() {
        return "mcp_server_process_title";
    }

    private void init() {
        setLayout(new BorderLayout(0, 5));
        setBorder(makeBorder());
        add(makeTitlePanel(), BorderLayout.NORTH);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Server launch"));
        GridBagConstraints c = baseConstraints();
        addRow(panel, c, 0, "Command:", commandField);
        addRow(panel, c, 1, "Args (space separated):", argsField);
        envArea.setLineWrap(false);
        addRow(panel, c, 2, "Env (KEY=value per line):", new JScrollPane(envArea));
        addRow(panel, c, 3, "Ready host:", readyHostField);
        addRow(panel, c, 4, "Ready port:", readyPortField);
        addRow(panel, c, 5, "Startup wait (ms):", startupWaitField);
        add(panel, BorderLayout.CENTER);

        clearGui();
    }

    private GridBagConstraints baseConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 4, 2, 4);
        c.weightx = 1;
        return c;
    }

    private void addRow(JPanel panel, GridBagConstraints c, int row, String label,
                        java.awt.Component field) {
        c.gridy = row;
        c.gridx = 0;
        c.weightx = 0;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        panel.add(field, c);
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
                parseLongOrDefault(readyPortField.getText(), 3001L));
        cfg.setProperty(McpServerProcess.STARTUP_WAIT_MS,
                parseLongOrDefault(startupWaitField.getText(), 60_000L));
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
    }

    private static long parseLongOrDefault(String value, long fallback) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException | NullPointerException ex) {
            return fallback;
        }
    }
}
