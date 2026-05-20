package io.github.jmeter.mcp.config.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import io.github.jmeter.mcp.client.TransportType;
import io.github.jmeter.mcp.config.McpClientConfig;
import org.apache.jmeter.config.gui.AbstractConfigGui;
import org.apache.jmeter.testelement.TestElement;

/**
 * Swing GUI for the {@link McpClientConfig} element. Renders one panel per
 * transport type and swaps them with a {@link CardLayout} when the user
 * changes the {@code Transport} selection.
 */
public class McpClientConfigGui extends AbstractConfigGui {

    private static final long serialVersionUID = 1L;

    private static final String CARD_STDIO = "STDIO";
    private static final String CARD_SSE = "SSE";
    private static final String CARD_STREAMABLE_HTTP = "STREAMABLE_HTTP";

    private final JTextField nameField = new JTextField(20);
    private final JComboBox<TransportType> transportCombo =
            new JComboBox<>(TransportType.values());

    private final JTextField serverUrlField = new JTextField(30);
    private final JTextField endpointField = new JTextField(20);
    private final JTextField stdioCommandField = new JTextField(20);
    private final JTextField stdioArgsField = new JTextField(30);
    private final JTextArea stdioEnvArea = new JTextArea(4, 30);

    private final JTextField clientNameField = new JTextField(20);
    private final JTextField clientVersionField = new JTextField(10);
    private final JTextField requestTimeoutField = new JTextField(8);
    private final JTextField initTimeoutField = new JTextField(8);

    private final CardLayout transportCards = new CardLayout();
    private final JPanel transportPanel = new JPanel(transportCards);

    public McpClientConfigGui() {
        super();
        init();
    }

    @Override
    public String getStaticLabel() {
        return "MCP Client Config";
    }

    @Override
    public String getLabelResource() {
        // We provide a static label directly; this value is only used if
        // JMeter falls back to its resource lookup mechanism.
        return "mcp_client_config_title";
    }

    private void init() {
        setLayout(new BorderLayout(0, 5));
        setBorder(makeBorder());
        add(makeTitlePanel(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 5));
        center.add(buildCommonPanel(), BorderLayout.NORTH);
        center.add(buildTransportPanel(), BorderLayout.CENTER);
        center.add(buildAdvancedPanel(), BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        transportCombo.addActionListener(e -> showSelectedTransport());
        transportCombo.setSelectedItem(TransportType.STDIO);
        showSelectedTransport();
    }

    private JPanel buildCommonPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Connection"));
        GridBagConstraints c = baseConstraints();

        addRow(p, c, 0, "Variable Name:", nameField);
        addRow(p, c, 1, "Transport:", transportCombo);
        return p;
    }

    private JPanel buildTransportPanel() {
        transportPanel.setBorder(BorderFactory.createTitledBorder("Transport Settings"));

        transportPanel.add(buildHttpPanel(true), CARD_STREAMABLE_HTTP);
        transportPanel.add(buildHttpPanel(false), CARD_SSE);
        transportPanel.add(buildStdioPanel(), CARD_STDIO);
        return transportPanel;
    }

    private JPanel buildHttpPanel(boolean streamable) {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = baseConstraints();
        addRow(p, c, 0, "Server URL:", serverUrlField);
        addRow(p, c, 1, streamable ? "Endpoint (default /mcp):" : "SSE Endpoint:",
                endpointField);
        return p;
    }

    private JPanel buildStdioPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = baseConstraints();
        addRow(p, c, 0, "Command:", stdioCommandField);
        addRow(p, c, 1, "Args (space separated):", stdioArgsField);
        stdioEnvArea.setLineWrap(false);
        addRow(p, c, 2, "Env (KEY=value per line):", new JScrollPane(stdioEnvArea));
        return p;
    }

    private JPanel buildAdvancedPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Client Identity & Timeouts"));
        GridBagConstraints c = baseConstraints();
        addRow(p, c, 0, "Client Name:", clientNameField);
        addRow(p, c, 1, "Client Version:", clientVersionField);
        addRow(p, c, 2, "Request Timeout (ms):", requestTimeoutField);
        addRow(p, c, 3, "Init Timeout (ms):", initTimeoutField);
        return p;
    }

    private void showSelectedTransport() {
        TransportType t = (TransportType) transportCombo.getSelectedItem();
        if (t == null) {
            return;
        }
        switch (t) {
            case STDIO:
                transportCards.show(transportPanel, CARD_STDIO);
                break;
            case SSE:
                transportCards.show(transportPanel, CARD_SSE);
                break;
            case STREAMABLE_HTTP:
            default:
                transportCards.show(transportPanel, CARD_STREAMABLE_HTTP);
                break;
        }
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
        McpClientConfig config = new McpClientConfig();
        modifyTestElement(config);
        return config;
    }

    @Override
    public void modifyTestElement(TestElement element) {
        super.configureTestElement(element);
        if (!(element instanceof McpClientConfig)) {
            return;
        }
        McpClientConfig cfg = (McpClientConfig) element;
        cfg.setProperty(McpClientConfig.NAME, trimmed(nameField.getText(), "mcpClient"));

        TransportType selected = (TransportType) transportCombo.getSelectedItem();
        cfg.setProperty(McpClientConfig.TRANSPORT,
                selected != null ? selected.name() : TransportType.STDIO.name());

        cfg.setProperty(McpClientConfig.SERVER_URL, serverUrlField.getText());
        cfg.setProperty(McpClientConfig.ENDPOINT, endpointField.getText());
        cfg.setProperty(McpClientConfig.STDIO_COMMAND, stdioCommandField.getText());
        cfg.setProperty(McpClientConfig.STDIO_ARGS, stdioArgsField.getText());
        cfg.setProperty(McpClientConfig.STDIO_ENV, stdioEnvArea.getText());

        cfg.setProperty(McpClientConfig.CLIENT_NAME,
                trimmed(clientNameField.getText(), "jmeter-mcp-plugin"));
        cfg.setProperty(McpClientConfig.CLIENT_VERSION,
                trimmed(clientVersionField.getText(), "0.1.0"));
        cfg.setProperty(McpClientConfig.REQUEST_TIMEOUT_MS,
                parseLongOrDefault(requestTimeoutField.getText(), 30_000L));
        cfg.setProperty(McpClientConfig.INIT_TIMEOUT_MS,
                parseLongOrDefault(initTimeoutField.getText(), 30_000L));
    }

    @Override
    public void configure(TestElement element) {
        super.configure(element);
        if (!(element instanceof McpClientConfig)) {
            return;
        }
        McpClientConfig cfg = (McpClientConfig) element;
        nameField.setText(cfg.getPropertyAsString(McpClientConfig.NAME, "mcpClient"));
        transportCombo.setSelectedItem(TransportType.fromString(
                cfg.getPropertyAsString(McpClientConfig.TRANSPORT,
                        TransportType.STDIO.name())));
        serverUrlField.setText(cfg.getPropertyAsString(McpClientConfig.SERVER_URL, ""));
        endpointField.setText(cfg.getPropertyAsString(McpClientConfig.ENDPOINT, ""));
        stdioCommandField.setText(cfg.getPropertyAsString(McpClientConfig.STDIO_COMMAND, ""));
        stdioArgsField.setText(cfg.getPropertyAsString(McpClientConfig.STDIO_ARGS, ""));
        stdioEnvArea.setText(cfg.getPropertyAsString(McpClientConfig.STDIO_ENV, ""));
        clientNameField.setText(cfg.getPropertyAsString(McpClientConfig.CLIENT_NAME,
                "jmeter-mcp-plugin"));
        clientVersionField.setText(cfg.getPropertyAsString(McpClientConfig.CLIENT_VERSION,
                "0.1.0"));
        requestTimeoutField.setText(String.valueOf(
                cfg.getPropertyAsLong(McpClientConfig.REQUEST_TIMEOUT_MS, 30_000L)));
        initTimeoutField.setText(String.valueOf(
                cfg.getPropertyAsLong(McpClientConfig.INIT_TIMEOUT_MS, 30_000L)));
        showSelectedTransport();
    }

    @Override
    public void clearGui() {
        super.clearGui();
        nameField.setText("mcpClient");
        transportCombo.setSelectedItem(TransportType.STDIO);
        serverUrlField.setText("http://localhost:8080");
        endpointField.setText("");
        stdioCommandField.setText("");
        stdioArgsField.setText("");
        stdioEnvArea.setText("");
        clientNameField.setText("jmeter-mcp-plugin");
        clientVersionField.setText("0.1.0");
        requestTimeoutField.setText("30000");
        initTimeoutField.setText("30000");
        showSelectedTransport();
    }

    private static String trimmed(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String t = value.trim();
        return t.isEmpty() ? fallback : t;
    }

    private static long parseLongOrDefault(String value, long fallback) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException | NullPointerException ex) {
            return fallback;
        }
    }
}
