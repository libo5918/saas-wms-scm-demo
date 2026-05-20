package com.example.scm.aiagent.toolcalling.service;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatRequest;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatResponse;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecuteRequest;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecuteResponse;
import com.example.scm.aiagent.toolcalling.model.ToolCallingPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;

/**
 * Tool Calling Chat 应用服务。
 *
 * <p>当前阶段实现最小闭环：规划工具、执行工具、拼装答案。</p>
 */
@Slf4j
@Service
public class ToolCallingChatService {

    private final AiAgentProperties properties;
    private final MockToolPlanner mockToolPlanner;
    private final SpringAiToolCallingService springAiToolCallingService;

    public ToolCallingChatService(AiAgentProperties properties,
                                  MockToolPlanner mockToolPlanner,
                                  SpringAiToolCallingService springAiToolCallingService) {
        this.properties = properties;
        this.mockToolPlanner = mockToolPlanner;
        this.springAiToolCallingService = springAiToolCallingService;
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

        log.info("AI tool calling chat request received, tenantId={}, userId={}, runId={}, plannerMode={}, messageLength={}",
                context.tenantId(), context.userId(), runId, plannerMode, safeLength(request.getMessage()));

        ToolCallingPlan plan = switch (plannerMode) {
            case "spring-ai" -> planWithSpringAiFallback(request);
            case "mock" -> mockToolPlanner.plan(request);
            default -> mockToolPlanner.plan(request);
        };

        ToolCallingExecuteRequest executeRequest = new ToolCallingExecuteRequest();
        executeRequest.setRunId(runId);
        executeRequest.setToolName(plan.selectedTool());
        executeRequest.setArguments(plan.toolArguments());

        ToolCallingExecuteResponse toolResponse = springAiToolCallingService.execute(executeRequest, context);
        String answer = buildAnswer(request.getMessage(), plan.selectedTool(), plan.toolArguments(), toolResponse);

        long latencyMs = elapsedMs(startedAt);
        log.info("AI tool calling chat finished, tenantId={}, userId={}, runId={}, plannerMode={}, selectedTool={}, success={}, latencyMs={}",
                context.tenantId(), context.userId(), runId, plan.plannerMode(), plan.selectedTool(), toolResponse.isSuccess(), latencyMs);

        return ToolCallingChatResponse.builder()
                .runId(runId)
                .plannerMode(plan.plannerMode())
                .selectedTool(plan.selectedTool())
                .toolArguments(plan.toolArguments())
                .toolResponse(toolResponse)
                .answer(answer)
                .latencyMs(latencyMs)
                .build();
    }

    private ToolCallingPlan planWithSpringAiFallback(ToolCallingChatRequest request) {
        ToolCallingPlan fallbackPlan = mockToolPlanner.plan(request);
        return ToolCallingPlan.builder()
                .plannerMode("spring-ai")
                .selectedTool(fallbackPlan.selectedTool())
                .toolArguments(fallbackPlan.toolArguments())
                .reason("spring_ai_planner_fallback_to_mock")
                .build();
    }

    private String buildAnswer(String message, String selectedTool, Map<String, Object> arguments,
                               ToolCallingExecuteResponse toolResponse) {
        if (!toolResponse.isSuccess()) {
            return "我已尝试调用工具 `" + selectedTool + "`，但执行失败：" +
                    toolResponse.getToolResponse().getErrorMessage();
        }
        return "已根据你的问题调用工具 `" + selectedTool + "` 完成查询。"
                + " toolArguments=" + arguments
                + "，可继续基于这次结果追问更细的信息。";
    }

    private int safeLength(String value) {
        return value == null ? 0 : value.length();
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
