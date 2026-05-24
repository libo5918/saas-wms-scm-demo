package com.example.scm.aiagent.mcp.service;

import com.example.scm.aiagent.mcp.dto.McpToolDisplayView;
import com.example.scm.aiagent.mcp.dto.McpToolInvokeRequest;
import com.example.scm.aiagent.mcp.dto.McpToolInvokeResponse;
import com.example.scm.aiagent.mcp.dto.McpToolListResponse;
import com.example.scm.aiagent.mcp.dto.McpToolView;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.tool.dto.ToolInvokeRequest;
import com.example.scm.aiagent.tool.dto.ToolResponse;
import com.example.scm.aiagent.tool.model.ToolDefinition;
import com.example.scm.aiagent.tool.service.ToolInvocationService;
import com.example.scm.aiagent.tool.service.ToolRegistry;
import com.example.scm.aiagent.toolcalling.display.ToolCallingDisplaySchemaBuilder;
import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayData;
import com.example.scm.aiagent.toolcalling.schema.ToolSchemaConverter;
import com.example.scm.common.core.CommonErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * MCP 风格 Tool 暴露服务。
 *
 * <p>只做暴露治理和安全视图转换，实际执行仍复用 ToolInvocationService。</p>
 */
@Slf4j
@Service
public class McpToolExposureService {

    private static final Set<String> DEFAULT_EXPOSED_TOOLS = Set.of("mdm.getMaterial", "inventory.getBalance");

    private final ToolRegistry toolRegistry;
    private final ToolInvocationService toolInvocationService;
    private final ToolSchemaConverter toolSchemaConverter;
    private final ToolCallingDisplaySchemaBuilder displaySchemaBuilder;

    public McpToolExposureService(ToolRegistry toolRegistry,
                                  ToolInvocationService toolInvocationService,
                                  ToolSchemaConverter toolSchemaConverter,
                                  ToolCallingDisplaySchemaBuilder displaySchemaBuilder) {
        this.toolRegistry = toolRegistry;
        this.toolInvocationService = toolInvocationService;
        this.toolSchemaConverter = toolSchemaConverter;
        this.displaySchemaBuilder = displaySchemaBuilder;
    }

    public McpToolListResponse listTools(AgentRequestContext context) {
        List<McpToolView> tools = toolRegistry.listDefinitions().stream()
                .filter(this::isMcpExposed)
                .sorted(Comparator.comparing(ToolDefinition::getName))
                .map(this::toView)
                .toList();
        log.info("AI MCP tool list queried, tenantId={}, userId={}, toolCount={}",
                context.tenantId(), context.userId(), tools.size());
        return McpToolListResponse.builder()
                .tenantId(context.tenantId())
                .toolCount(tools.size())
                .tools(tools)
                .build();
    }

    public McpToolInvokeResponse invoke(String toolName, McpToolInvokeRequest request, AgentRequestContext context) {
        long startedAt = System.nanoTime();
        String runId = StringUtils.hasText(request.getRunId()) ? request.getRunId() : UUID.randomUUID().toString();
        ToolDefinition definition = toolRegistry.findDefinition(toolName).orElse(null);
        if (definition == null) {
            long latencyMs = elapsedMs(startedAt);
            log.warn("AI MCP tool not found, tenantId={}, userId={}, runId={}, toolName={}, mcpExposed=false, latencyMs={}",
                    context.tenantId(), context.userId(), runId, toolName, latencyMs);
            return failure(runId, toolName, CommonErrorCode.NOT_FOUND.code(), "Tool not found: " + toolName, latencyMs);
        }
        if (!isMcpExposed(definition)) {
            long latencyMs = elapsedMs(startedAt);
            log.warn("AI MCP tool exposure denied, tenantId={}, userId={}, runId={}, toolName={}, readOnly={}, mcpExposed=false, latencyMs={}",
                    context.tenantId(), context.userId(), runId, toolName, definition.isReadOnly(), latencyMs);
            return failure(runId, toolName, CommonErrorCode.FORBIDDEN.code(), "Tool is not exposed to MCP: " + toolName, latencyMs);
        }

        ToolInvokeRequest toolInvokeRequest = new ToolInvokeRequest();
        toolInvokeRequest.setRunId(runId);
        toolInvokeRequest.setToolName(toolName);
        toolInvokeRequest.setParameters(request.effectiveArguments());
        ToolResponse response = toolInvocationService.invoke(toolInvokeRequest, context);
        McpToolDisplayView display = null;
        if (response.isSuccess()) {
            ToolCallingDisplayData displayData = displaySchemaBuilder.build(response.getToolName(), response.getData());
            display = McpToolDisplayView.builder()
                    .displayTitle(displayData.displayTitle())
                    .displaySummary(displayData.displaySummary())
                    .displayFields(displayData.displayFields())
                    .displayItems(displayData.displayItems())
                    .build();
        }
        log.info("AI MCP tool invoked, tenantId={}, userId={}, runId={}, toolName={}, success={}, errorCode={}, latencyMs={}, mcpExposed=true",
                context.tenantId(), context.userId(), response.getRunId(), toolName, response.isSuccess(),
                response.getErrorCode(), response.getLatencyMs());
        return McpToolInvokeResponse.builder()
                .runId(response.getRunId())
                .toolName(response.getToolName())
                .success(response.isSuccess())
                .errorCode(response.getErrorCode())
                .errorMessage(response.getErrorMessage())
                .display(display)
                .latencyMs(response.getLatencyMs())
                .build();
    }

    public boolean isMcpExposed(ToolDefinition definition) {
        return definition != null
                && definition.isReadOnly()
                && DEFAULT_EXPOSED_TOOLS.contains(definition.getName());
    }

    private McpToolView toView(ToolDefinition definition) {
        var descriptor = toolSchemaConverter.convert(definition);
        return McpToolView.builder()
                .name(definition.getName())
                .description(definition.getDescription())
                .inputSchema(descriptor.getInputSchema())
                .displaySchema(Map.of(
                        "type", "display",
                        "fields", List.of("displayTitle", "displaySummary", "displayFields", "displayItems")))
                .domain(definition.getDomain())
                .category(definition.getCategory())
                .routeTags(definition.getRouteTags() == null ? List.of() : definition.getRouteTags())
                .readOnly(definition.isReadOnly())
                .requiredPermissions(definition.getRequiredPermissions() == null ? List.of() : definition.getRequiredPermissions())
                .build();
    }

    private McpToolInvokeResponse failure(String runId, String toolName, String errorCode, String errorMessage, long latencyMs) {
        return McpToolInvokeResponse.builder()
                .runId(runId)
                .toolName(toolName)
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .latencyMs(latencyMs)
                .build();
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
