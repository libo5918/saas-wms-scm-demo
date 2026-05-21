package com.example.scm.aiagent.toolcalling.application;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.tool.dto.ToolInvokeRequest;
import com.example.scm.aiagent.tool.dto.ToolResponse;
import com.example.scm.aiagent.tool.model.ToolDefinition;
import com.example.scm.aiagent.tool.service.ToolInvocationAuditService;
import com.example.scm.aiagent.tool.service.ToolInvocationService;
import com.example.scm.aiagent.tool.service.ToolRegistry;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecuteRequest;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecuteResponse;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingSchemaListResponse;
import com.example.scm.aiagent.toolcalling.schema.ToolSchemaConverter;
import com.example.scm.common.core.CommonErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Spring AI Tool Calling 适配服务。
 *
 * <p>当前阶段只打通 schema 转换和服务端统一执行入口，
 * 不直接负责真实 LLM 自动决策。</p>
 */
@Slf4j
@Service
public class SpringAiToolCallingService {

    private final ToolRegistry toolRegistry;
    private final ToolSchemaConverter toolSchemaConverter;
    private final ToolInvocationService toolInvocationService;
    private final ToolInvocationAuditService toolInvocationAuditService;
    private final AiAgentProperties aiAgentProperties;

    public SpringAiToolCallingService(ToolRegistry toolRegistry,
                                      ToolSchemaConverter toolSchemaConverter,
                                      ToolInvocationService toolInvocationService,
                                      ToolInvocationAuditService toolInvocationAuditService,
                                      AiAgentProperties aiAgentProperties) {
        this.toolRegistry = toolRegistry;
        this.toolSchemaConverter = toolSchemaConverter;
        this.toolInvocationService = toolInvocationService;
        this.toolInvocationAuditService = toolInvocationAuditService;
        this.aiAgentProperties = aiAgentProperties;
    }

    /**
     * 查询当前模型可见的 Tool schema 列表。
     */
    public ToolCallingSchemaListResponse listSchemas(AgentRequestContext context) {
        var schemas = toolRegistry.listDefinitions().stream()
                .map(toolSchemaConverter::convert)
                .toList();
        log.info("AI tool calling schema queried, tenantId={}, userId={}, toolCount={}",
                context.tenantId(), context.userId(), schemas.size());
        return ToolCallingSchemaListResponse.builder()
                .tenantId(context.tenantId())
                .toolCount(schemas.size())
                .tools(schemas)
                .build();
    }

    /**
     * 执行一次模拟的 Tool Calling 指令。
     */
    public ToolCallingExecuteResponse execute(ToolCallingExecuteRequest request, AgentRequestContext context) {
        long startedAt = System.nanoTime();
        String runId = StringUtils.hasText(request.getRunId()) ? request.getRunId() : UUID.randomUUID().toString();
        String toolName = request.getToolName();
        Map<String, Object> arguments = request.getArguments() == null ? Map.of() : request.getArguments();
        String adapterMode = aiAgentProperties.getTools().getAdapterMode();

        ToolDefinition definition = toolRegistry.findDefinition(toolName).orElse(null);
        if (definition == null) {
            ToolResponse toolResponse = toolInvocationService.invoke(buildInvokeRequest(toolName, runId, arguments), context);
            return buildResponse(toolName, arguments, toolResponse, startedAt);
        }

        List<String> validationErrors = validateArguments(definition, arguments);
        if (!validationErrors.isEmpty()) {
            long latencyMs = elapsedMs(startedAt);
            String errorMessage = String.join("; ", validationErrors);
            log.warn("AI tool calling validation failed, tenantId={}, userId={}, runId={}, toolName={}, adapterMode={}, latencyMs={}, message={}",
                    context.tenantId(), context.userId(), runId, toolName, adapterMode, latencyMs, errorMessage);
            toolInvocationAuditService.record(context, runId, toolName, adapterMode, false,
                    CommonErrorCode.BAD_REQUEST.code(), latencyMs);
            ToolResponse toolResponse = ToolResponse.builder()
                    .success(false)
                    .toolName(toolName)
                    .runId(runId)
                    .errorCode(CommonErrorCode.BAD_REQUEST.code())
                    .errorMessage(errorMessage)
                    .latencyMs(latencyMs)
                    .build();
            return ToolCallingExecuteResponse.builder()
                    .success(false)
                    .toolName(toolName)
                    .arguments(arguments)
                    .toolResponse(toolResponse)
                    .latencyMs(latencyMs)
                    .build();
        }

        ToolResponse toolResponse = toolInvocationService.invoke(buildInvokeRequest(toolName, runId, arguments), context);
        log.info("AI tool calling executed, tenantId={}, userId={}, runId={}, toolName={}, adapterMode={}, success={}, latencyMs={}",
                context.tenantId(), context.userId(), runId, toolName, adapterMode, toolResponse.isSuccess(), toolResponse.getLatencyMs());
        return buildResponse(toolName, arguments, toolResponse, startedAt);
    }

    private ToolInvokeRequest buildInvokeRequest(String toolName, String runId, Map<String, Object> arguments) {
        ToolInvokeRequest invokeRequest = new ToolInvokeRequest();
        invokeRequest.setToolName(toolName);
        invokeRequest.setRunId(runId);
        invokeRequest.setParameters(arguments);
        return invokeRequest;
    }

    private ToolCallingExecuteResponse buildResponse(String toolName, Map<String, Object> arguments,
                                                     ToolResponse toolResponse, long startedAt) {
        return ToolCallingExecuteResponse.builder()
                .success(toolResponse.isSuccess())
                .toolName(toolName)
                .arguments(arguments)
                .toolResponse(toolResponse)
                .latencyMs(elapsedMs(startedAt))
                .build();
    }

    private List<String> validateArguments(ToolDefinition definition, Map<String, Object> arguments) {
        List<String> errors = new ArrayList<>();
        List<String> requiredParameters = definition.getRequiredParameters() == null ? List.of() : definition.getRequiredParameters();
        for (String parameter : requiredParameters) {
            if (!hasValue(arguments, parameter)) {
                errors.add("Missing required parameter: " + parameter);
            }
        }
        List<List<String>> oneOfGroups = definition.getOneOfRequiredGroups() == null ? List.of() : definition.getOneOfRequiredGroups();
        for (List<String> group : oneOfGroups) {
            boolean matched = group.stream().anyMatch(parameter -> hasValue(arguments, parameter));
            if (!matched) {
                errors.add("At least one parameter is required from group: " + String.join(", ", group));
            }
        }
        return errors;
    }

    private boolean hasValue(Map<String, Object> arguments, String parameter) {
        if (arguments == null || !arguments.containsKey(parameter)) {
            return false;
        }
        Object value = arguments.get(parameter);
        if (value == null) {
            return false;
        }
        if (value instanceof String text) {
            return StringUtils.hasText(text);
        }
        return true;
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
