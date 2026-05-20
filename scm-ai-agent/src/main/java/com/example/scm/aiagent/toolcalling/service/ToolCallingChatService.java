package com.example.scm.aiagent.toolcalling.service;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.tool.dto.ToolResponse;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatRequest;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatResponse;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecuteRequest;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecuteResponse;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecutionView;
import com.example.scm.aiagent.toolcalling.model.ToolCallingPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Tool Calling Chat 应用服务。
 *
 * <p>当前阶段实现最小闭环：规划工具、执行工具、拼装回答，
 * 并支持 requestedTool、mock planner、spring-ai planner 三种入口。</p>
 */
@Slf4j
@Service
public class ToolCallingChatService {

    private final AiAgentProperties properties;
    private final MockToolPlanner mockToolPlanner;
    private final SpringAiToolPlanner springAiToolPlanner;
    private final SpringAiToolCallingService springAiToolCallingService;
    private final ToolCallingAnswerBuilder toolCallingAnswerBuilder;

    public ToolCallingChatService(AiAgentProperties properties,
                                  MockToolPlanner mockToolPlanner,
                                  SpringAiToolPlanner springAiToolPlanner,
                                  SpringAiToolCallingService springAiToolCallingService,
                                  ToolCallingAnswerBuilder toolCallingAnswerBuilder) {
        this.properties = properties;
        this.mockToolPlanner = mockToolPlanner;
        this.springAiToolPlanner = springAiToolPlanner;
        this.springAiToolCallingService = springAiToolCallingService;
        this.toolCallingAnswerBuilder = toolCallingAnswerBuilder;
    }

    /**
     * 执行一次 Tool Calling Chat。
     */
    public ToolCallingChatResponse chat(ToolCallingChatRequest request, AgentRequestContext context) {
        long startedAt = System.nanoTime();
        String runId = StringUtils.hasText(request.getRunId()) ? request.getRunId() : UUID.randomUUID().toString();
        String plannerMode = StringUtils.hasText(request.getPlannerMode())
                ? request.getPlannerMode()
                : properties.getToolCalling().getPlannerMode();

        log.info("AI tool calling chat request received, tenantId={}, userId={}, runId={}, plannerMode={}, requestedTool={}, messageLength={}",
                context.tenantId(), context.userId(), runId, plannerMode, request.getRequestedTool(), safeLength(request.getMessage()));

        ToolCallingPlan plan = resolvePlan(request, context, runId, plannerMode);

        ToolCallingExecuteRequest executeRequest = new ToolCallingExecuteRequest();
        executeRequest.setRunId(runId);
        executeRequest.setToolName(plan.selectedTool());
        executeRequest.setArguments(plan.toolArguments());

        ToolCallingExecuteResponse executeResponse = springAiToolCallingService.execute(executeRequest, context);
        ToolCallingExecutionView execution = toExecutionView(executeResponse);
        String answer = toolCallingAnswerBuilder.buildAnswer(plan, execution);

        long latencyMs = elapsedMs(startedAt);
        log.info("AI tool calling chat finished, tenantId={}, userId={}, runId={}, plannerMode={}, planningSource={}, fallbackUsed={}, selectedTool={}, success={}, latencyMs={}",
                context.tenantId(), context.userId(), runId, plan.plannerMode(), plan.planningSource(),
                plan.fallbackUsed(), plan.selectedTool(), execution.isSuccess(), latencyMs);

        return ToolCallingChatResponse.builder()
                .runId(runId)
                .plannerMode(plan.plannerMode())
                .planningSource(plan.planningSource())
                .fallbackUsed(plan.fallbackUsed())
                .selectedTool(plan.selectedTool())
                .toolArguments(plan.toolArguments())
                .planningReason(plan.reason())
                .execution(execution)
                .answer(answer)
                .latencyMs(latencyMs)
                .build();
    }

    private ToolCallingPlan resolvePlan(ToolCallingChatRequest request, AgentRequestContext context,
                                        String runId, String plannerMode) {
        if (StringUtils.hasText(request.getRequestedTool())) {
            return mockToolPlanner.planRequestedTool(plannerMode, request.getRequestedTool(), request.getToolArguments());
        }

        if ("spring-ai".equalsIgnoreCase(plannerMode)) {
            try {
                return springAiToolPlanner.plan(request, context, runId);
            } catch (Exception ex) {
                if (properties.getToolCalling().getSpringAiPlanner().isFallbackToMock()) {
                    log.warn("AI tool calling planner fallback to mock, tenantId={}, userId={}, runId={}, plannerMode={}, errorType={}, errorMessage={}",
                            context.tenantId(), context.userId(), runId, plannerMode,
                            ex.getClass().getSimpleName(), ex.getMessage());
                    return mockToolPlanner.planFallback(plannerMode, request,
                            "spring_ai_planner_failed:" + ex.getClass().getSimpleName());
                }
                throw ex;
            }
        }

        return mockToolPlanner.planByRules(plannerMode, request);
    }

    /**
     * 将底层 Tool Calling 执行结果压平成更适合 chat 接口展示的 execution 结构。
     */
    private ToolCallingExecutionView toExecutionView(ToolCallingExecuteResponse executeResponse) {
        ToolResponse toolResponse = executeResponse.getToolResponse();
        return ToolCallingExecutionView.builder()
                .success(toolResponse != null ? toolResponse.isSuccess() : executeResponse.isSuccess())
                .toolName(StringUtils.hasText(executeResponse.getToolName())
                        ? executeResponse.getToolName()
                        : toolResponse == null ? null : toolResponse.getToolName())
                .errorCode(toolResponse == null ? null : toolResponse.getErrorCode())
                .errorMessage(toolResponse == null ? null : toolResponse.getErrorMessage())
                .data(toolResponse == null ? null : toolResponse.getData())
                .latencyMs(executeResponse.getLatencyMs())
                .build();
    }

    private int safeLength(String value) {
        return value == null ? 0 : value.length();
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
