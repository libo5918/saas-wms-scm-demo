package com.example.scm.aiagent.mcp.service;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.mcp.dto.McpJsonRpcRequest;
import com.example.scm.aiagent.mcp.dto.McpJsonRpcResponse;
import com.example.scm.aiagent.mcp.dto.McpServerContent;
import com.example.scm.aiagent.mcp.dto.McpServerToolCallResult;
import com.example.scm.aiagent.mcp.dto.McpServerToolView;
import com.example.scm.aiagent.mcp.dto.McpServerToolsListResult;
import com.example.scm.aiagent.mcp.dto.McpToolDisplayView;
import com.example.scm.aiagent.mcp.dto.McpToolInvokeRequest;
import com.example.scm.aiagent.mcp.dto.McpToolInvokeResponse;
import com.example.scm.aiagent.mcp.dto.McpToolView;
import com.example.scm.aiagent.model.AgentRequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MCP Server HTTP JSON-RPC 最小 transport。
 *
 * <p>该服务只负责 MCP tools/list 与 tools/call 的协议映射，真实 Tool 暴露治理和执行仍委托
 * McpToolExposureService，避免绕过权限、审计、runtime protection 和 display schema。</p>
 */
@Slf4j
@Service
public class McpServerTransportService {

    private static final int ERROR_METHOD_NOT_FOUND = -32601;
    private static final int ERROR_INVALID_PARAMS = -32602;
    private static final int ERROR_SERVER_DISABLED = -32000;
    private static final int ERROR_TOOL_CALL_FAILED = -32010;

    private final AiAgentProperties properties;
    private final McpToolExposureService mcpToolExposureService;

    public McpServerTransportService(AiAgentProperties properties,
                                     McpToolExposureService mcpToolExposureService) {
        this.properties = properties;
        this.mcpToolExposureService = mcpToolExposureService;
    }

    public McpJsonRpcResponse handle(McpJsonRpcRequest request, AgentRequestContext context) {
        long startedAt = System.nanoTime();
        String method = request == null ? null : request.getMethod();
        Object id = request == null ? null : request.getId();
        if (!properties.getMcp().getServer().isEnabled()) {
            log.warn("AI MCP server transport disabled, tenantId={}, userId={}, mcpMethod={}, transport={}",
                    context.tenantId(), context.userId(), method, properties.getMcp().getServer().getTransport());
            return McpJsonRpcResponse.failure(id, ERROR_SERVER_DISABLED, "MCP server transport is disabled", null);
        }
        if (!properties.getMcp().getServer().isExposeTools()) {
            return McpJsonRpcResponse.failure(id, ERROR_SERVER_DISABLED, "MCP tool exposure is disabled", null);
        }
        if ("tools/list".equals(method)) {
            McpJsonRpcResponse response = toolsList(id, context);
            log.info("AI MCP server tools/list finished, tenantId={}, userId={}, mcpMethod={}, success=true, latencyMs={}, transport={}",
                    context.tenantId(), context.userId(), method, elapsedMs(startedAt), properties.getMcp().getServer().getTransport());
            return response;
        }
        if ("tools/call".equals(method)) {
            McpJsonRpcResponse response = toolsCall(id, request.getParams(), context, startedAt);
            boolean success = response.getError() == null;
            String errorCode = success ? null : String.valueOf(response.getError().getCode());
            log.info("AI MCP server tools/call finished, tenantId={}, userId={}, mcpMethod={}, success={}, errorCode={}, latencyMs={}, transport={}",
                    context.tenantId(), context.userId(), method, success, errorCode, elapsedMs(startedAt),
                    properties.getMcp().getServer().getTransport());
            return response;
        }
        return McpJsonRpcResponse.failure(id, ERROR_METHOD_NOT_FOUND, "Unsupported MCP method: " + method, null);
    }

    private McpJsonRpcResponse toolsList(Object id, AgentRequestContext context) {
        List<McpServerToolView> tools = mcpToolExposureService.listTools(context).getTools().stream()
                .map(this::toServerTool)
                .toList();
        return McpJsonRpcResponse.success(id, McpServerToolsListResult.builder().tools(tools).build());
    }

    private McpJsonRpcResponse toolsCall(Object id, Map<String, Object> params, AgentRequestContext context, long startedAt) {
        if (params == null || !StringUtils.hasText(asString(params.get("name")))) {
            return McpJsonRpcResponse.failure(id, ERROR_INVALID_PARAMS, "Missing MCP tool name", null);
        }
        String toolName = asString(params.get("name"));
        McpToolInvokeRequest request = new McpToolInvokeRequest();
        request.setRunId(StringUtils.hasText(asString(params.get("runId"))) ? asString(params.get("runId")) : UUID.randomUUID().toString());
        request.setArguments(asMap(params.get("arguments")));
        McpToolInvokeResponse response = mcpToolExposureService.invoke(toolName, request, context);
        if (!response.isSuccess()) {
            return McpJsonRpcResponse.failure(id, ERROR_TOOL_CALL_FAILED,
                    StringUtils.hasText(response.getErrorMessage()) ? response.getErrorMessage() : "MCP tool call failed",
                    safeErrorData(response));
        }
        return McpJsonRpcResponse.success(id, toToolCallResult(response, elapsedMs(startedAt)));
    }

    private McpServerToolView toServerTool(McpToolView tool) {
        return McpServerToolView.builder()
                .name(tool.getName())
                .description(tool.getDescription())
                .inputSchema(tool.getInputSchema())
                .annotations(Map.of(
                        "domain", nullToEmpty(tool.getDomain()),
                        "category", nullToEmpty(tool.getCategory()),
                        "routeTags", tool.getRouteTags() == null ? List.of() : tool.getRouteTags(),
                        "readOnly", tool.isReadOnly(),
                        "requiredPermissions", tool.getRequiredPermissions() == null ? List.of() : tool.getRequiredPermissions()))
                .build();
    }

    private McpServerToolCallResult toToolCallResult(McpToolInvokeResponse response, long latencyMs) {
        Map<String, Object> structured = new LinkedHashMap<>();
        structured.put("runId", response.getRunId());
        structured.put("toolName", response.getToolName());
        structured.put("success", response.isSuccess());
        structured.put("errorCode", response.getErrorCode());
        structured.put("errorMessage", response.getErrorMessage());
        structured.put("latencyMs", response.getLatencyMs());
        McpToolDisplayView display = response.getDisplay();
        String text = "Tool call succeeded";
        if (display != null) {
            text = StringUtils.hasText(display.getDisplaySummary()) ? display.getDisplaySummary() : display.getDisplayTitle();
            structured.put("displayTitle", display.getDisplayTitle());
            structured.put("displaySummary", display.getDisplaySummary());
            structured.put("displayFields", display.getDisplayFields());
            structured.put("displayItems", display.getDisplayItems());
        }
        structured.put("transportLatencyMs", latencyMs);
        return McpServerToolCallResult.builder()
                .content(List.of(McpServerContent.builder().text(text).build()))
                .structuredContent(structured)
                .isError(false)
                .build();
    }

    private Map<String, Object> safeErrorData(McpToolInvokeResponse response) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("runId", response.getRunId());
        data.put("toolName", response.getToolName());
        data.put("success", response.isSuccess());
        data.put("errorCode", response.getErrorCode());
        data.put("errorMessage", response.getErrorMessage());
        data.put("latencyMs", response.getLatencyMs());
        return data;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), item));
            return result;
        }
        return Map.of();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
