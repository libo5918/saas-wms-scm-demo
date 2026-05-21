package com.example.scm.aiagent.toolcalling.application;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.tool.dto.ToolResponse;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatRequest;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatResponse;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecuteRequest;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecuteResponse;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecutionView;
import com.example.scm.aiagent.toolcalling.answer.ToolCallingAnswerSummaryService;
import com.example.scm.aiagent.toolcalling.display.ToolCallingDisplaySchemaBuilder;
import com.example.scm.aiagent.toolcalling.model.ToolCallingAnswerSummaryResult;
import com.example.scm.aiagent.toolcalling.model.ToolCallingPlan;
import com.example.scm.aiagent.toolcalling.planning.MockToolPlanner;
import com.example.scm.aiagent.toolcalling.planning.SpringAiToolPlanner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Tool Calling Chat 应用服务。
 *
 * <p>当前阶段实现三段式最小闭环：
 * 1. 规划工具；
 * 2. 执行工具；
 * 3. 基于执行结果生成最终答案。</p>
 */
@Slf4j
@Service
public class ToolCallingChatService {

    private final AiAgentProperties properties;
    private final MockToolPlanner mockToolPlanner;
    private final SpringAiToolPlanner springAiToolPlanner;
    private final SpringAiToolCallingService springAiToolCallingService;
    private final ToolCallingAnswerSummaryService answerSummaryService;
    private final ToolCallingDisplaySchemaBuilder displaySchemaBuilder;

    public ToolCallingChatService(AiAgentProperties properties,
                                  MockToolPlanner mockToolPlanner,
                                  SpringAiToolPlanner springAiToolPlanner,
                                  SpringAiToolCallingService springAiToolCallingService,
                                  ToolCallingAnswerSummaryService answerSummaryService,
                                  ToolCallingDisplaySchemaBuilder displaySchemaBuilder) {
        this.properties = properties;
        this.mockToolPlanner = mockToolPlanner;
        this.springAiToolPlanner = springAiToolPlanner;
        this.springAiToolCallingService = springAiToolCallingService;
        this.answerSummaryService = answerSummaryService;
        this.displaySchemaBuilder = displaySchemaBuilder;
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

        log.info("AI tool calling chat request received, tenantId={}, userId={}, runId={}, plannerMode={}, requestedTool={}, answerMode={}, messageLength={}",
                context.tenantId(), context.userId(), runId, plannerMode, request.getRequestedTool(),
                properties.getToolCalling().getAnswerMode(), safeLength(request.getMessage()));

        ToolCallingPlan plan = resolvePlan(request, context, runId, plannerMode);

        ToolCallingExecuteRequest executeRequest = new ToolCallingExecuteRequest();
        executeRequest.setRunId(runId);
        executeRequest.setToolName(plan.selectedTool());
        executeRequest.setArguments(plan.toolArguments());

        ToolCallingExecuteResponse executeResponse = springAiToolCallingService.execute(executeRequest, context);
        ToolCallingExecutionView execution = toExecutionView(executeResponse);
        ToolCallingAnswerSummaryResult answerSummary = answerSummaryService.summarize(
                request, context, plan, execution, runId);

        long latencyMs = elapsedMs(startedAt);
        boolean fallbackUsed = plan.fallbackUsed() || answerSummary.fallbackUsed();
        log.info("AI tool calling chat finished, tenantId={}, userId={}, runId={}, plannerMode={}, answerMode={}, planningSource={}, selectedTool={}, success={}, fallbackUsed={}, latencyMs={}",
                context.tenantId(), context.userId(), runId, plan.plannerMode(), answerSummary.answerMode(),
                plan.planningSource(), plan.selectedTool(), execution.isSuccess(), fallbackUsed, latencyMs);

        return ToolCallingChatResponse.builder()
                .runId(runId)
                .plannerMode(plan.plannerMode())
                .planningSource(plan.planningSource())
                .fallbackUsed(plan.fallbackUsed())
                .selectedTool(plan.selectedTool())
                .toolArguments(plan.toolArguments())
                .planningReason(plan.reason())
                .execution(execution)
                .answer(answerSummary.answer())
                .latencyMs(latencyMs)
                .build();
    }

    private ToolCallingPlan resolvePlan(ToolCallingChatRequest request,
                                        AgentRequestContext context,
                                        String runId,
                                        String plannerMode) {
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
     * 将底层 Tool Calling 执行结果压平为 chat 接口使用的 execution 结构。
     */
    private ToolCallingExecutionView toExecutionView(ToolCallingExecuteResponse executeResponse) {
        ToolResponse toolResponse = executeResponse.getToolResponse();
        boolean success = toolResponse != null ? toolResponse.isSuccess() : executeResponse.isSuccess();
        String toolName = StringUtils.hasText(executeResponse.getToolName())
                ? executeResponse.getToolName()
                : toolResponse == null ? null : toolResponse.getToolName();
        Object rawData = toolResponse == null ? null : toolResponse.getData();
        return ToolCallingExecutionView.builder()
                .success(success)
                .toolName(toolName)
                .errorCode(toolResponse == null ? null : toolResponse.getErrorCode())
                .errorMessage(toolResponse == null ? null : toolResponse.getErrorMessage())
                .data(success ? displaySchemaBuilder.build(toolName, rawData) : rawData)
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
