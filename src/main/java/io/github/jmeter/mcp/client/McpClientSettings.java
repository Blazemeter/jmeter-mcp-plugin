package io.github.jmeter.mcp.client;

import java.io.Serializable;

/**
 * Plain value object holding the resolved configuration for an MCP client.
 *
 * <p>This is decoupled from the JMeter {@code TestElement} so it can be reused
 * from samplers, unit tests, or standalone tools.
 */
public final class McpClientSettings implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name = "mcpClient";
    private TransportType transport = TransportType.STDIO;

    // HTTP-based transports
    private String serverUrl = "";
    private String endpoint = "";

    // STDIO transport
    private String stdioCommand = "";
    private String stdioArgs = "";
    private String stdioEnv = "";

    // Client identity
    private String clientName = "jmeter-mcp-plugin";
    private String clientVersion = "0.1.0";

    // Timeouts (milliseconds)
    private long requestTimeoutMillis = 30_000L;
    private long initializationTimeoutMillis = 30_000L;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TransportType getTransport() {
        return transport;
    }

    public void setTransport(TransportType transport) {
        this.transport = transport;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getStdioCommand() {
        return stdioCommand;
    }

    public void setStdioCommand(String stdioCommand) {
        this.stdioCommand = stdioCommand;
    }

    public String getStdioArgs() {
        return stdioArgs;
    }

    public void setStdioArgs(String stdioArgs) {
        this.stdioArgs = stdioArgs;
    }

    public String getStdioEnv() {
        return stdioEnv;
    }

    public void setStdioEnv(String stdioEnv) {
        this.stdioEnv = stdioEnv;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    public long getRequestTimeoutMillis() {
        return requestTimeoutMillis;
    }

    public void setRequestTimeoutMillis(long requestTimeoutMillis) {
        this.requestTimeoutMillis = requestTimeoutMillis;
    }

    public long getInitializationTimeoutMillis() {
        return initializationTimeoutMillis;
    }

    public void setInitializationTimeoutMillis(long initializationTimeoutMillis) {
        this.initializationTimeoutMillis = initializationTimeoutMillis;
    }
}
