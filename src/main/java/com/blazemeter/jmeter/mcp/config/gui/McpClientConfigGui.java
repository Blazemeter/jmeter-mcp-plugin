package com.blazemeter.jmeter.mcp.config.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;

import javax.swing.BoxLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SwingWorker;

import com.blazemeter.jmeter.commons.BlazemeterLabsLogo;

import com.blazemeter.jmeter.mcp.client.McpClientPreviewLauncher;
import com.blazemeter.jmeter.mcp.client.McpClientRegistry;
import com.blazemeter.jmeter.mcp.client.McpClientSettings;
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

    private static final String CARD_STDIO = "STDIO";
    private static final String CARD_SSE = "SSE";
    private static final String CARD_STREAMABLE_HTTP = "STREAMABLE_HTTP";

    private final JTextField nameField = new JTextField(20);
    private final JComboBox<TransportType> transportCombo =
            new JComboBox<>(TransportType.values());
    private final JCheckBox connectOnStartupCheck =
            new JCheckBox("Connect on test start (otherwise wait for first sampler)");

    private final JTextField serverUrlField = new JTextField(30);
    private final JTextField endpointField = new JTextField(20);
    private final JTextField stdioCommandField = new JTextField(20);
    private final JTextField stdioArgsField = new JTextField(30);
    private final JTextArea stdioEnvArea = EnvVarsField.newTextArea();

    private final JTextField clientNameField = new JTextField(20);
    private final JTextField clientVersionField = new JTextField(10);
    private final JTextField requestTimeoutField = new JTextField(8);
    private final JTextField initTimeoutField = new JTextField(8);

    private final JTextField serverLaunchCommandField = new JTextField(20);
    private final JTextField serverLaunchArgsField = new JTextField(40);
    private final JTextArea serverLaunchEnvArea = EnvVarsField.newTextArea();
    private final JTextField serverReadyHostField = new JTextField(15);
    private final JTextField serverReadyPortField = new JTextField(8);
    private final JTextField serverStartupWaitField = new JTextField(8);
    private final JPanel serverLaunchPanel = new JPanel(new GridBagLayout());

    private final JButton startNowButton = new JButton("Start Now");
    private final JButton stopNowButton = new JButton("Stop");
    private final JLabel connectionStatusLabel = new JLabel("Not connected");

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

        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.add(buildCommonPanel());
        stack.add(buildTransportPanel());
        stack.add(buildServerLaunchPanel());
        stack.add(buildPreviewPanel());
        stack.add(buildAdvancedPanel());

        JPanel center = new JPanel(new BorderLayout(0, 5));
        center.add(stack, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(new BlazemeterLabsLogo(PluginGuiConstants.PLUGIN_REPOSITORY_URL), BorderLayout.PAGE_END);

        ResponsiveSizing.applyTree(this);

        transportCombo.addActionListener(e -> showSelectedTransport());
        transportCombo.setSelectedItem(TransportType.STDIO);
        startNowButton.addActionListener(e -> startNow());
        stopNowButton.addActionListener(e -> stopNow());
        showSelectedTransport();
        refreshConnectionStatus();
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

        transportPanel.add(buildHttpPanel(true), CARD_STREAMABLE_HTTP);
        transportPanel.add(buildHttpPanel(false), CARD_SSE);
        transportPanel.add(buildStdioPanel(), CARD_STDIO);

        transportCardHost = new AdaptiveCardLayoutHost(transportPanel);
        return transportCardHost;
    }

    private JPanel buildHttpPanel(boolean streamable) {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = GridBagForm.horizontalRowConstraints();
        GridBagForm.addLabelAndField(p, c, 0, "Server URL:", serverUrlField);
        GridBagForm.addLabelAndField(p, c, 1, streamable ? "Endpoint (default /mcp):" : "SSE Endpoint:",
                endpointField);
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

    private JPanel buildServerLaunchPanel() {
        serverLaunchPanel.setBorder(BorderFactory.createTitledBorder(
                "Server launch (Start Now — HTTP/SSE only)"));
        GridBagConstraints c = GridBagForm.horizontalRowConstraints();
        GridBagForm.addLabelAndField(serverLaunchPanel, c, 0, "Command:", serverLaunchCommandField);
        GridBagForm.addLabelAndField(serverLaunchPanel, c, 1, "Args:", serverLaunchArgsField);
        JScrollPane envScroll = makeScrollPane(serverLaunchEnvArea);
        EnvVarsField.applyScrollPaneSize(envScroll, serverLaunchEnvArea);
        GridBagForm.addLabelAndMultilineField(serverLaunchPanel, c, 2, "Env:", envScroll);
        GridBagForm.addLabelAndField(serverLaunchPanel, c, 3, "Ready host:", serverReadyHostField);
        GridBagForm.addLabelAndField(serverLaunchPanel, c, 4, "Ready port:", serverReadyPortField);
        GridBagForm.addLabelAndField(serverLaunchPanel, c, 5, "Startup wait (ms):", serverStartupWaitField);
        return serverLaunchPanel;
    }

    private JPanel buildPreviewPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Manually start connection"));
        GridBagConstraints c = GridBagForm.horizontalRowConstraints();
        JPanel buttons = new JPanel();
        buttons.add(startNowButton);
        buttons.add(stopNowButton);
        GridBagForm.addLabelAndField(p, c, 0, "", buttons);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 3;
        connectionStatusLabel.setOpaque(false);
        p.add(connectionStatusLabel, c);
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
                transportCards.show(transportPanel, CARD_SSE);
                break;
            case STREAMABLE_HTTP:
            default:
                transportCards.show(transportPanel, CARD_STREAMABLE_HTTP);
                break;
        }
        if (transportCardHost != null) {
            transportCardHost.afterCardShown();
        }
        boolean http = t == TransportType.SSE || t == TransportType.STREAMABLE_HTTP;
        serverLaunchPanel.setVisible(http);
        if (http) {
            applyDefaultServerLaunchArgs(t);
        }
        refreshConnectionStatus();
    }

    private void applyDefaultServerLaunchArgs(TransportType transport) {
        if (!serverLaunchCommandField.getText().isBlank()) {
            return;
        }
        serverLaunchCommandField.setText("npx");
        if (transport == TransportType.SSE) {
            serverLaunchArgsField.setText("-y @modelcontextprotocol/server-everything sse");
        } else {
            serverLaunchArgsField.setText("-y @modelcontextprotocol/server-everything streamableHttp");
        }
    }

    private void startNow() {
        McpClientSettings settings = readSettingsFromGui();
        startNowButton.setEnabled(false);
        stopNowButton.setEnabled(false);
        connectionStatusLabel.setText("Starting…");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                McpClientPreviewLauncher.startNow(settings);
                return null;
            }

            @Override
            protected void done() {
                startNowButton.setEnabled(true);
                stopNowButton.setEnabled(true);
                try {
                    get();
                    refreshConnectionStatus();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    connectionStatusLabel.setText("Not connected");
                    JOptionPane.showMessageDialog(McpClientConfigGui.this,
                            cause.getMessage(),
                            "Start Now failed",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void stopNow() {
        McpClientSettings settings = readSettingsFromGui();
        startNowButton.setEnabled(false);
        stopNowButton.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                McpClientPreviewLauncher.stopNow(settings.getName(), settings);
                return null;
            }

            @Override
            protected void done() {
                startNowButton.setEnabled(true);
                stopNowButton.setEnabled(true);
                refreshConnectionStatus();
            }
        }.execute();
    }

    private void refreshConnectionStatus() {
        String name = Strings.trimToDefault(nameField.getText(), "mcpClient");
        if (McpClientRegistry.getInstance().isConnected(name)) {
            connectionStatusLabel.setText("Connected as '" + name + "' (reused on test run)");
            stopNowButton.setEnabled(true);
        } else {
            connectionStatusLabel.setText("Not connected");
            stopNowButton.setEnabled(false);
        }
    }

    private McpClientSettings readSettingsFromGui() {
        McpClientSettings s = new McpClientSettings();
        s.setName(Strings.trimToDefault(nameField.getText(), "mcpClient"));
        TransportType selected = (TransportType) transportCombo.getSelectedItem();
        s.setTransport(selected != null ? selected : TransportType.STDIO);
        s.setServerUrl(serverUrlField.getText());
        s.setEndpoint(endpointField.getText());
        s.setStdioCommand(stdioCommandField.getText());
        s.setStdioArgs(stdioArgsField.getText());
        s.setStdioEnv(stdioEnvArea.getText());
        s.setClientName(Strings.trimToDefault(clientNameField.getText(), "jmeter-mcp-plugin"));
        s.setClientVersion(Strings.trimToDefault(clientVersionField.getText(), "0.1.0"));
        s.setRequestTimeoutMillis(GridBagForm.parseLong(requestTimeoutField.getText(), 30_000L));
        s.setInitializationTimeoutMillis(GridBagForm.parseLong(initTimeoutField.getText(), 30_000L));
        s.setConnectOnStartup(connectOnStartupCheck.isSelected());
        s.setServerLaunchCommand(serverLaunchCommandField.getText());
        s.setServerLaunchArgs(serverLaunchArgsField.getText());
        s.setServerLaunchEnv(serverLaunchEnvArea.getText());
        s.setServerReadyHost(Strings.trimToDefault(serverReadyHostField.getText(), "localhost"));
        s.setServerReadyPort((int) GridBagForm.parseLong(serverReadyPortField.getText(), 3001L));
        s.setServerStartupWaitMs(GridBagForm.parseLong(serverStartupWaitField.getText(), 60_000L));
        return s;
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
        cfg.setProperty(McpClientConfig.SERVER_LAUNCH_COMMAND, serverLaunchCommandField.getText());
        cfg.setProperty(McpClientConfig.SERVER_LAUNCH_ARGS, serverLaunchArgsField.getText());
        cfg.setProperty(McpClientConfig.SERVER_LAUNCH_ENV, serverLaunchEnvArea.getText());
        cfg.setProperty(McpClientConfig.SERVER_READY_HOST, serverReadyHostField.getText());
        cfg.setProperty(McpClientConfig.SERVER_READY_PORT,
                GridBagForm.parseLong(serverReadyPortField.getText(), 3001L));
        cfg.setProperty(McpClientConfig.SERVER_STARTUP_WAIT_MS,
                GridBagForm.parseLong(serverStartupWaitField.getText(), 60_000L));
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
        serverLaunchCommandField.setText(
                cfg.getPropertyAsString(McpClientConfig.SERVER_LAUNCH_COMMAND, ""));
        serverLaunchArgsField.setText(cfg.getPropertyAsString(McpClientConfig.SERVER_LAUNCH_ARGS, ""));
        serverLaunchEnvArea.setText(cfg.getPropertyAsString(McpClientConfig.SERVER_LAUNCH_ENV, ""));
        serverReadyHostField.setText(
                cfg.getPropertyAsString(McpClientConfig.SERVER_READY_HOST, "localhost"));
        serverReadyPortField.setText(String.valueOf(
                cfg.getPropertyAsLong(McpClientConfig.SERVER_READY_PORT, 3001L)));
        serverStartupWaitField.setText(String.valueOf(
                cfg.getPropertyAsLong(McpClientConfig.SERVER_STARTUP_WAIT_MS, 60_000L)));
        showSelectedTransport();
        refreshConnectionStatus();
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
        serverLaunchCommandField.setText("npx");
        serverLaunchArgsField.setText("-y @modelcontextprotocol/server-everything sse");
        serverLaunchEnvArea.setText("");
        serverReadyHostField.setText("localhost");
        serverReadyPortField.setText("3001");
        serverStartupWaitField.setText("60000");
        showSelectedTransport();
        refreshConnectionStatus();
    }
}
