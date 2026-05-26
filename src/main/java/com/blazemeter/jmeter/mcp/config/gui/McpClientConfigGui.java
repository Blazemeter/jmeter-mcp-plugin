package com.blazemeter.jmeter.mcp.config.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Scrollable;

import com.blazemeter.jmeter.commons.BlazemeterLabsLogo;

import com.blazemeter.jmeter.mcp.client.TransportType;
import com.blazemeter.jmeter.mcp.config.McpClientConfig;
import com.blazemeter.jmeter.mcp.gui.EnvVarsField;
import com.blazemeter.jmeter.mcp.gui.GridBagForm;
import com.blazemeter.jmeter.mcp.gui.PluginGuiConstants;
import com.blazemeter.jmeter.mcp.gui.responsive.AdaptiveCardLayoutHost;
import com.blazemeter.jmeter.mcp.gui.responsive.ResponsiveSizing;
import com.blazemeter.jmeter.mcp.gui.scroll.JMeterScrollableSupport;
import com.blazemeter.jmeter.mcp.util.Strings;
import org.apache.jmeter.config.gui.AbstractConfigGui;
import org.apache.jmeter.testelement.TestElement;

/**
 * Swing GUI for the {@link McpClientConfig} element. Renders one panel per
 * transport type and swaps them with a {@link CardLayout} when the user
 * changes the {@code Transport} selection.
 */
public class McpClientConfigGui extends AbstractConfigGui implements Scrollable {

    private static final long serialVersionUID = 1L;

    private static final String CARD_HTTP = "HTTP";
    private static final String CARD_STDIO = "STDIO";

    private final JTextField nameField = new JTextField(20);
    private final JComboBox<TransportType> transportCombo =
            new JComboBox<>(TransportType.values());
    private final JCheckBox connectOnStartupCheck =
            new JCheckBox("Connect on test start (otherwise wait for first sampler)");

    private final JTextField serverUrlField = new JTextField(30);
    private final JLabel httpEndpointLabel = new JLabel("Endpoint (default /mcp):");
    private final JTextField endpointField = new JTextField(20);
    private final JTextField stdioCommandField = new JTextField(20);
    private final JTextField stdioArgsField = new JTextField(30);
    private final JTextArea stdioEnvArea = EnvVarsField.newTextArea();

    private final JTextField clientNameField = new JTextField(20);
    private final JTextField clientVersionField = new JTextField(10);
    private final JTextField requestTimeoutField = new JTextField(8);
    private final JTextField initTimeoutField = new JTextField(8);

    private final CardLayout transportCards = new CardLayout();
    private final JPanel transportPanel = new JPanel(transportCards);
    private AdaptiveCardLayoutHost transportCardHost;

    public McpClientConfigGui() {
        super();
        init();
    }

    @Override
    public String getStaticLabel() {
        return "bzm - MCP Client Config";
    }

    @Override
    public String getLabelResource() {
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
        add(new BlazemeterLabsLogo(PluginGuiConstants.PLUGIN_REPOSITORY_URL), BorderLayout.PAGE_END);

        ResponsiveSizing.applyTree(this);

        transportCombo.addActionListener(e -> showSelectedTransport());
        transportCombo.setSelectedItem(TransportType.STDIO);
        showSelectedTransport();
        assignComponentNames();
    }

    private void assignComponentNames() {
        nameField.setName("mcpClientConfig.name");
        transportCombo.setName("mcpClientConfig.transport");
        connectOnStartupCheck.setName("mcpClientConfig.connectOnStartup");
        serverUrlField.setName("mcpClientConfig.serverUrl");
        endpointField.setName("mcpClientConfig.endpoint");
        stdioCommandField.setName("mcpClientConfig.stdioCommand");
        stdioArgsField.setName("mcpClientConfig.stdioArgs");
        stdioEnvArea.setName("mcpClientConfig.stdioEnv");
        clientNameField.setName("mcpClientConfig.clientName");
        clientVersionField.setName("mcpClientConfig.clientVersion");
        requestTimeoutField.setName("mcpClientConfig.requestTimeout");
        initTimeoutField.setName("mcpClientConfig.initTimeout");
    }

    private JPanel buildCommonPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Connection"));
        GridBagConstraints c = GridBagForm.horizontalRowConstraints();

        GridBagForm.addLabelAndField(p, c, 0, "Variable Name:", nameField);
        GridBagForm.addLabelAndField(p, c, 1, "Transport:", transportCombo);
        GridBagForm.addLabelAndField(p, c, 2, "", connectOnStartupCheck);
        return p;
    }

    private AdaptiveCardLayoutHost buildTransportPanel() {
        transportPanel.setBorder(BorderFactory.createTitledBorder("Transport Settings"));

        transportPanel.add(buildHttpPanel(), CARD_HTTP);
        transportPanel.add(buildStdioPanel(), CARD_STDIO);

        transportCardHost = new AdaptiveCardLayoutHost(transportPanel);
        return transportCardHost;
    }

    private JPanel buildHttpPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = GridBagForm.horizontalRowConstraints();
        GridBagForm.addLabelAndField(p, c, 0, "Server URL:", serverUrlField);
        c.gridy = 1;
        c.gridx = 0;
        c.weightx = 0;
        p.add(httpEndpointLabel, c);
        c.gridx = 1;
        c.weightx = 1;
        p.add(endpointField, c);
        return p;
    }

    private JPanel buildStdioPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = GridBagForm.horizontalRowConstraints();
        GridBagForm.addLabelAndField(p, c, 0, "Command:", stdioCommandField);
        GridBagForm.addLabelAndField(p, c, 1, "Args (space separated):", stdioArgsField);
        JScrollPane envScroll = makeScrollPane(stdioEnvArea);
        EnvVarsField.applyScrollPaneSize(envScroll, stdioEnvArea);
        GridBagForm.addLabelAndMultilineField(p, c, 2, "Env (KEY=value per line):", envScroll);
        return p;
    }

    private JPanel buildAdvancedPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Client Identity & Timeouts"));
        GridBagConstraints c = GridBagForm.horizontalRowConstraints();
        GridBagForm.addLabelAndField(p, c, 0, "Client Name:", clientNameField);
        GridBagForm.addLabelAndField(p, c, 1, "Client Version:", clientVersionField);
        GridBagForm.addLabelAndField(p, c, 2, "Request Timeout (ms):", requestTimeoutField);
        GridBagForm.addLabelAndField(p, c, 3, "Init Timeout (ms):", initTimeoutField);
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
                httpEndpointLabel.setText("SSE Endpoint:");
                transportCards.show(transportPanel, CARD_HTTP);
                break;
            case STREAMABLE_HTTP:
            default:
                httpEndpointLabel.setText("Endpoint (default /mcp):");
                transportCards.show(transportPanel, CARD_HTTP);
                break;
        }
        if (transportCardHost != null) {
            transportCardHost.afterCardShown();
        }
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
        cfg.setProperty(McpClientConfig.NAME, Strings.trimToDefault(nameField.getText(), "mcpClient"));

        TransportType selected = (TransportType) transportCombo.getSelectedItem();
        cfg.setProperty(McpClientConfig.TRANSPORT,
                selected != null ? selected.name() : TransportType.STDIO.name());

        cfg.setProperty(McpClientConfig.SERVER_URL, serverUrlField.getText());
        cfg.setProperty(McpClientConfig.ENDPOINT, endpointField.getText());
        cfg.setProperty(McpClientConfig.STDIO_COMMAND, stdioCommandField.getText());
        cfg.setProperty(McpClientConfig.STDIO_ARGS, stdioArgsField.getText());
        cfg.setProperty(McpClientConfig.STDIO_ENV, stdioEnvArea.getText());

        cfg.setProperty(McpClientConfig.CLIENT_NAME,
                Strings.trimToDefault(clientNameField.getText(), "jmeter-mcp-plugin"));
        cfg.setProperty(McpClientConfig.CLIENT_VERSION,
                Strings.trimToDefault(clientVersionField.getText(), "0.1.0"));
        cfg.setProperty(McpClientConfig.REQUEST_TIMEOUT_MS,
                GridBagForm.parseLong(requestTimeoutField.getText(), 30_000L));
        cfg.setProperty(McpClientConfig.INIT_TIMEOUT_MS,
                GridBagForm.parseLong(initTimeoutField.getText(), 30_000L));
        cfg.setProperty(McpClientConfig.CONNECT_ON_STARTUP, connectOnStartupCheck.isSelected());
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
        connectOnStartupCheck.setSelected(
                cfg.getPropertyAsBoolean(McpClientConfig.CONNECT_ON_STARTUP, false));
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
        connectOnStartupCheck.setSelected(false);
        showSelectedTransport();
    }
}
