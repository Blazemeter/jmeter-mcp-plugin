package com.blazemeter.jmeter.mcp.sampler;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.blazemeter.jmeter.mcp.client.JsonMappers;
import com.blazemeter.jmeter.mcp.client.McpClientRegistry;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JMeter Sampler that invokes operations on a connected MCP server.
 *
 * <p>The sampler resolves the {@link McpSyncClient} via
 * {@link McpClientRegistry} using the configured {@link #CONFIG_NAME} (which
 * must match the {@code Variable Name} of an {@code MCP Client Config}
 * element).
 */
public class McpSampler extends AbstractSampler {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(McpSampler.class);

    public static final String CONFIG_NAME = "McpSampler.configName";
    public static final String OPERATION = "McpSampler.operation";
    public static final String TOOL_NAME = "McpSampler.toolName";
    public static final String ARGUMENTS_JSON = "McpSampler.argumentsJson";
    public static final String RESOURCE_URI = "McpSampler.resourceUri";
    public static final String PROMPT_NAME = "McpSampler.promptName";

    private static final String CONTENT_TYPE_JSON = "application/json";

    @Override
    public SampleResult sample(Entry e) {
        String configName = getPropertyAsString(CONFIG_NAME, "mcpClient");
        McpOperation operation = McpOperation.fromString(
                getPropertyAsString(OPERATION, McpOperation.PING.name()));

        SampleResult result = new SampleResult();
        result.setSampleLabel(getName());
        result.setSamplerData(samplerData(configName, operation));
        result.setDataType(SampleResult.TEXT);
        result.setContentType(CONTENT_TYPE_JSON);

        try {
            long connectStart = System.currentTimeMillis();
            McpSyncClient client = McpClientRegistry.getInstance().getOrConnect(configName);
            long connectElapsed = System.currentTimeMillis() - connectStart;
            if (connectElapsed > 0) {
                result.setConnectTime(connectElapsed);
            }
            if (client == null) {
                throw new IllegalStateException(
                        "No MCP client settings for '" + configName
                                + "'. Add a 'bzm - MCP Client Config' element whose Variable Name "
                                + "matches this sampler's Client Config field.");
            }

            // Timers start after the client is ready so connect + initialize (including
            // background "connect on test start") are not counted as sample latency.
            result.sampleStart();

            Object operationResult = invoke(client, operation);
            String responseBody = toPrettyJson(operationResult);

            result.setResponseHeaders(buildResponseHeaders(client, operation));
            result.setResponseData(responseBody, "UTF-8");
            result.setDataEncoding("UTF-8");
            result.setResponseCodeOK();
            result.setResponseMessageOK();
            result.setSuccessful(!isErrorResponse(operationResult));
            if (!result.isSuccessful()) {
                result.setResponseMessage("MCP tool returned isError=true");
            }
            return result;
        } catch (Exception ex) {
            LOG.warn("MCP sampler '{}' failed: {}", getName(), ex.getMessage(), ex);
            result.setSuccessful(false);
            result.setResponseCode(ex.getClass().getSimpleName());
            result.setResponseMessage(ex.getMessage() == null ? ex.toString() : ex.getMessage());
            result.setResponseData(stackTrace(ex), "UTF-8");
            return result;
        } finally {
            result.sampleEnd();
        }
    }

    /**
     * Builds HTTP-style response headers with MCP session metadata so View
     * Results Tree shows handshake data separately from the operation body.
     */
    private static String buildResponseHeaders(McpSyncClient client, McpOperation operation) {
        StringBuilder headers = new StringBuilder();
        appendHeader(headers, "X-MCP-Operation", operation.name());
        appendHeader(headers, "X-MCP-Session", toJson(buildSessionMetadata(client)));
        appendHeader(headers, "Content-Type", CONTENT_TYPE_JSON);
        return headers.toString();
    }

    private static void appendHeader(StringBuilder headers, String name, String value) {
        if (headers.length() > 0) {
            headers.append('\n');
        }
        headers.append(name).append(": ").append(value);
    }

    /**
     * Session / initialize handshake (protocol, capabilities, server info,
     * instructions) — same content the SDK logs at INFO from
     * {@code LifecycleInitializer}.
     */
    private static Map<String, Object> buildSessionMetadata(McpSyncClient client) {
        Map<String, Object> session = new LinkedHashMap<>();
        session.put("initialized", client.isInitialized());

        McpSchema.InitializeResult init = client.getCurrentInitializationResult();
        if (init != null) {
            session.put("protocolVersion", init.protocolVersion());
            session.put("capabilities", init.capabilities());
            session.put("serverInfo", init.serverInfo());
            session.put("instructions", init.instructions());
            if (init.meta() != null && !init.meta().isEmpty()) {
                session.put("meta", init.meta());
            }
        } else {
            session.put("serverInfo", client.getServerInfo());
            session.put("capabilities", client.getServerCapabilities());
            session.put("instructions", client.getServerInstructions());
        }
        return session;
    }

    private Object invoke(McpSyncClient client, McpOperation op) {
        switch (op) {
            case PING: {
                Object pingResult = client.ping();
                if (pingResult == null) {
                    Map<String, Object> body = new HashMap<>();
                    body.put("ok", true);
                    body.put("operation", "ping");
                    body.put("result", null);
                    body.put("note",
                            "McpSyncClient.ping() returned null; the ping RPC completed successfully.");
                    return body;
                }
                return pingResult;
            }
            case LIST_TOOLS:
                return client.listTools();
            case CALL_TOOL: {
                String tool = required(TOOL_NAME, "Tool Name");
                Map<String, Object> args = parseArguments();
                return client.callTool(new McpSchema.CallToolRequest(tool, args));
            }
            case LIST_RESOURCES:
                return client.listResources();
            case READ_RESOURCE: {
                String uri = required(RESOURCE_URI, "Resource URI");
                return client.readResource(new McpSchema.ReadResourceRequest(uri));
            }
            case LIST_PROMPTS:
                return client.listPrompts();
            case GET_PROMPT: {
                String prompt = required(PROMPT_NAME, "Prompt Name");
                Map<String, Object> args = parseArguments();
                return client.getPrompt(new McpSchema.GetPromptRequest(prompt, args));
            }
            default:
                throw new IllegalStateException("Unhandled MCP operation: " + op);
        }
    }

    private String required(String propKey, String label) {
        String value = getPropertyAsString(propKey, "").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(label + " is required for this operation");
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArguments() {
        String raw = getPropertyAsString(ARGUMENTS_JSON, "").trim();
        if (raw.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return (Map<String, Object>) JsonMappers.getDefault().readValue(raw, Map.class);
        } catch (IOException | RuntimeException ex) {
            throw new IllegalArgumentException(
                    "Arguments must be a JSON object: " + ex.getMessage(), ex);
        }
    }

    private static boolean isErrorResponse(Object response) {
        if (response instanceof McpSchema.CallToolResult) {
            Boolean isError = ((McpSchema.CallToolResult) response).isError();
            return Boolean.TRUE.equals(isError);
        }
        return false;
    }

    private static String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        try {
            return JsonMappers.getDefault().writeValueAsString(value);
        } catch (IOException | RuntimeException ex) {
            LOG.debug("Falling back to toString() for {}: {}",
                    value.getClass().getName(), ex.getMessage());
            return String.valueOf(value);
        }
    }

    private static String toPrettyJson(Object value) {
        if (value == null) {
            return "null";
        }
        try {
            return JsonMappers.writeValueAsPrettyString(value);
        } catch (IOException | RuntimeException ex) {
            LOG.debug("Falling back to compact JSON for {}: {}",
                    value.getClass().getName(), ex.getMessage());
            return toJson(value);
        }
    }

    private String samplerData(String configName, McpOperation operation) {
        StringBuilder sb = new StringBuilder();
        sb.append("client=").append(configName).append('\n');
        sb.append("operation=").append(operation).append('\n');
        switch (operation) {
            case CALL_TOOL:
                sb.append("tool=").append(getPropertyAsString(TOOL_NAME, "")).append('\n');
                sb.append("arguments=").append(getPropertyAsString(ARGUMENTS_JSON, "{}"));
                break;
            case READ_RESOURCE:
                sb.append("uri=").append(getPropertyAsString(RESOURCE_URI, ""));
                break;
            case GET_PROMPT:
                sb.append("prompt=").append(getPropertyAsString(PROMPT_NAME, "")).append('\n');
                sb.append("arguments=").append(getPropertyAsString(ARGUMENTS_JSON, "{}"));
                break;
            default:
                break;
        }
        return sb.toString();
    }

    private static String stackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
