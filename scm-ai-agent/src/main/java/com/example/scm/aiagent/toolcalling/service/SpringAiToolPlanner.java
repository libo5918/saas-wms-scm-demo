package com.example.scm.aiagent.toolcalling.service;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.model.ChatModelInvocation;
import com.example.scm.aiagent.model.ChatModelResult;
import com.example.scm.aiagent.model.ModelRoute;
import com.example.scm.aiagent.model.ModelRouteRequest;
import com.example.scm.aiagent.service.ChatModelClient;
import com.example.scm.aiagent.service.ModelRouter;
import com.example.scm.aiagent.tool.service.ToolRegistry;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatRequest;
import com.example.scm.aiagent.toolcalling.model.ToolCallingPlan;
import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 基于真实 Spring AI 模型的 Tool Planner。
 *
 * <p>该服务负责把当前已注册工具的 schema 交给模型，让模型输出结构化 Tool Plan，
 * 再交由服务端继续执行后续 Tool Calling 流程。</p>
 */
@Slf4j
@Service
public class SpringAiToolPlanner {

    private static final List<String> REQUIRED_CAPABILITIES = List.of("TOOL_CALLING", "STRUCTURED_OUTPUT");

    private final AiAgentProperties properties;
    private final ModelRouter modelRouter;
    private final ChatModelClient chatModelClient;
    private final ToolRegistry toolRegistry;
    private final ToolSchemaConverter toolSchemaConverter;
    private final ToolPlanningPromptBuilder promptBuilder;
    private final ToolPlanParser toolPlanParser;

    public SpringAiToolPlanner(AiAgentProperties properties,
                               ModelRouter modelRouter,
                               ChatModelClient chatModelClient,
                               ToolRegistry toolRegistry,
                               ToolSchemaConverter toolSchemaConverter,
                               ToolPlanningPromptBuilder promptBuilder,
                               ToolPlanParser toolPlanParser) {
        this.properties = properties;
        this.modelRouter = modelRouter;
        this.chatModelClient = chatModelClient;
        this.toolRegistry = toolRegistry;
        this.toolSchemaConverter = toolSchemaConverter;
        this.promptBuilder = promptBuilder;
        this.toolPlanParser = toolPlanParser;
    }

    /**
     * 使用真实模型执行一次 Tool 规划。
     */
    public ToolCallingPlan plan(ToolCallingChatRequest request, AgentRequestContext context, String runId) {
        AiAgentProperties.SpringAiPlannerProperties plannerProperties = properties.getToolCalling().getSpringAiPlanner();
        if (!plannerProperties.isEnabled()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(),
                    "Spring AI planner is disabled by configuration");
        }

        long startedAt = System.nanoTime();
        String taskType = StringUtils.hasText(plannerProperties.getTaskType())
                ? plannerProperties.getTaskType()
                : "tool_calling";
        String prompt = promptBuilder.build(request.getMessage(), toolRegistry.listDefinitions().stream()
                .map(toolSchemaConverter::convert)
                .toList());
        Exception lastError = null;
        int maxRetries = Math.max(1, plannerProperties.getMaxRetries());

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                ModelRoute route = modelRouter.route(new ModelRouteRequest(
                        context.tenantId(),
                        context.userId(),
                        taskType,
                        null,
                        "spring-ai",
                        REQUIRED_CAPABILITIES,
                        null,
                        null
                ));
                log.info("AI spring planner started, tenantId={}, userId={}, runId={}, attempt={}, modelName={}, provider={}, providerMode={}",
                        context.tenantId(), context.userId(), runId, attempt, route.modelName(), route.provider(), route.providerMode());

                ChatModelResult result = chatModelClient.chat(new ChatModelInvocation(
                        runId,
                        prompt,
                        taskType,
                        context,
                        route
                ));
                ToolCallingPlan parsedPlan = toolPlanParser.parse(result.answer(), "spring-ai");
                if (toolRegistry.findDefinition(parsedPlan.selectedTool()).isEmpty()) {
                    throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(),
                            "Spring AI planner selected unknown tool: " + parsedPlan.selectedTool());
                }

                long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;
                log.info("AI spring planner finished, tenantId={}, userId={}, runId={}, attempt={}, selectedTool={}, latencyMs={}",
                        context.tenantId(), context.userId(), runId, attempt, parsedPlan.selectedTool(), latencyMs);
                return ToolCallingPlan.builder()
                        .plannerMode("spring-ai")
                        .planningSource("spring-ai")
                        .fallbackUsed(false)
                        .selectedTool(parsedPlan.selectedTool())
                        .toolArguments(parsedPlan.toolArguments())
                        .reason(parsedPlan.reason())
                        .build();
            } catch (BusinessException ex) {
                lastError = ex;
                log.warn("AI spring planner attempt failed, tenantId={}, userId={}, runId={}, attempt={}, errorType={}, errorMessage={}",
                        context.tenantId(), context.userId(), runId, attempt, ex.getClass().getSimpleName(), ex.getMessage());
            } catch (Exception ex) {
                lastError = ex;
                log.warn("AI spring planner attempt failed, tenantId={}, userId={}, runId={}, attempt={}, errorType={}, errorMessage={}",
                        context.tenantId(), context.userId(), runId, attempt, ex.getClass().getSimpleName(), ex.getMessage());
            }
        }

        if (lastError instanceof BusinessException businessException) {
            throw businessException;
        }
        throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(),
                "Spring AI planner failed: " + (lastError == null ? "unknown error" : lastError.getMessage()));
    }
}
