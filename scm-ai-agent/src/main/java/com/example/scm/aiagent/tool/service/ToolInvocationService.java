package com.example.scm.aiagent.tool.service;

import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.tool.dto.ToolInvokeRequest;
import com.example.scm.aiagent.tool.dto.ToolListResponse;
import com.example.scm.aiagent.tool.dto.ToolResponse;
import com.example.scm.aiagent.tool.model.ToolRequest;
import com.example.scm.aiagent.tool.spi.ToolExecutor;
import com.example.scm.common.core.CommonErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;

/**
 * Tool 调用服务。
 *
 * <p>负责工具查找、上下文组装、执行耗时统计和统一响应封装。</p>
 */
@Slf4j
@Service
public class ToolInvocationService {

    private final ToolRegistry toolRegistry;

    public ToolInvocationService(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /**
     * 查询当前可用工具列表。
     */
    public ToolListResponse listTools(AgentRequestContext context) {
        var definitions = toolRegistry.listDefinitions();
        log.info("AI tool list queried, tenantId={}, userId={}, toolCount={}",
                context.tenantId(), context.userId(), definitions.size());
        return ToolListResponse.builder()
                .tenantId(context.tenantId())
                .toolCount(definitions.size())
                .tools(definitions)
                .build();
    }

    /**
     * 执行指定工具。找不到工具时返回业务失败响应，不抛出系统异常。
     */
    public ToolResponse invoke(ToolInvokeRequest request, AgentRequestContext context) {
        long startedAt = System.nanoTime();
        String runId = StringUtils.hasText(request.getRunId()) ? request.getRunId() : UUID.randomUUID().toString();
        String toolName = request.getToolName();
        ToolExecutor executor = toolRegistry.findExecutor(toolName).orElse(null);
        if (executor == null) {
            long latencyMs = elapsedMs(startedAt);
            log.warn("AI tool not found, tenantId={}, userId={}, runId={}, toolName={}, latencyMs={}",
                    context.tenantId(), context.userId(), runId, toolName, latencyMs);
            return ToolResponse.builder()
                    .success(false)
                    .toolName(toolName)
                    .runId(runId)
                    .errorCode(CommonErrorCode.NOT_FOUND.code())
                    .errorMessage("Tool not found: " + toolName)
                    .latencyMs(latencyMs)
                    .build();
        }

        try {
            Object data = executor.execute(ToolRequest.builder()
                    .runId(runId)
                    .toolName(toolName)
                    .context(context)
                    .parameters(request.getParameters() == null ? Map.of() : request.getParameters())
                    .build());
            long latencyMs = elapsedMs(startedAt);
            log.info("AI tool invoked, tenantId={}, userId={}, runId={}, toolName={}, success=true, latencyMs={}",
                    context.tenantId(), context.userId(), runId, toolName, latencyMs);
            return ToolResponse.builder()
                    .success(true)
                    .toolName(toolName)
                    .runId(runId)
                    .data(data)
                    .latencyMs(latencyMs)
                    .build();
        } catch (RuntimeException ex) {
            long latencyMs = elapsedMs(startedAt);
            log.warn("AI tool invoke failed, tenantId={}, userId={}, runId={}, toolName={}, success=false, errorType={}, latencyMs={}",
                    context.tenantId(), context.userId(), runId, toolName, ex.getClass().getSimpleName(), latencyMs);
            return ToolResponse.builder()
                    .success(false)
                    .toolName(toolName)
                    .runId(runId)
                    .errorCode(CommonErrorCode.BAD_REQUEST.code())
                    .errorMessage(ex.getMessage())
                    .latencyMs(latencyMs)
                    .build();
        }
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
