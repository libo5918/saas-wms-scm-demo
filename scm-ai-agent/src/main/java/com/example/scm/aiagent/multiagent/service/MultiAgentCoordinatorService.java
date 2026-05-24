package com.example.scm.aiagent.multiagent.service;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.multiagent.dto.MultiAgentAgentView;
import com.example.scm.aiagent.multiagent.dto.MultiAgentChatRequest;
import com.example.scm.aiagent.multiagent.dto.MultiAgentChatResponse;
import com.example.scm.aiagent.multiagent.dto.MultiAgentMessageView;
import com.example.scm.aiagent.multiagent.dto.MultiAgentStepView;
import com.example.scm.aiagent.multiagent.model.AgentRoleDefinition;
import com.example.scm.aiagent.multiagent.model.MultiAgentActionType;
import com.example.scm.aiagent.multiagent.model.MultiAgentAgentState;
import com.example.scm.aiagent.multiagent.model.MultiAgentDefinition;
import com.example.scm.aiagent.multiagent.model.MultiAgentMessage;
import com.example.scm.aiagent.multiagent.model.MultiAgentMessageType;
import com.example.scm.aiagent.multiagent.model.MultiAgentRole;
import com.example.scm.aiagent.multiagent.model.MultiAgentRun;
import com.example.scm.aiagent.multiagent.model.MultiAgentRunStatus;
import com.example.scm.aiagent.multiagent.model.MultiAgentStep;
import com.example.scm.aiagent.multiagent.model.MultiAgentStepStatus;
import com.example.scm.aiagent.multiagent.store.MultiAgentRunStore;
import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Multi-Agent Coordinator 最小骨架，Phase 10.1 只记录受控单轮协作状态。 */
@Slf4j
@Service
public class MultiAgentCoordinatorService {

    private static final String COORDINATOR_AGENT = "CoordinatorAgent";
    private static final String PLANNER_AGENT = "PlannerAgent";

    private final AiAgentProperties properties;
    private final MultiAgentDefinitionRegistry definitionRegistry;
    private final MultiAgentRunStore runStore;

    public MultiAgentCoordinatorService(AiAgentProperties properties,
                                        MultiAgentDefinitionRegistry definitionRegistry,
                                        MultiAgentRunStore runStore) {
        this.properties = properties;
        this.definitionRegistry = definitionRegistry;
        this.runStore = runStore;
    }

    public MultiAgentChatResponse chat(MultiAgentChatRequest request, AgentRequestContext context) {
        long started = System.currentTimeMillis();
        String runId = StringUtils.hasText(request.getRunId()) ? request.getRunId() : UUID.randomUUID().toString();
        MultiAgentDefinition definition = definitionRegistry.getDefaultDefinition();
        Instant createdAt = Instant.now();

        MultiAgentRun run = MultiAgentRun.builder()
                .runId(runId)
                .tenantId(context.tenantId())
                .userId(context.userId())
                .userMessage(safeText(request.getMessage(), 300))
                .status(MultiAgentRunStatus.RUNNING)
                .createdAt(createdAt)
                .build();

        run.getAgents().addAll(definition.getAgents().stream()
                .map(this::toInitialState)
                .toList());

        MultiAgentStep coordinatorStep = successStep(1, COORDINATOR_AGENT, MultiAgentRole.COORDINATOR,
                MultiAgentActionType.NOOP, "用户任务已进入 Multi-Agent Coordinator",
                "已接收用户任务，Phase 10.1 仅记录受控协作骨架");
        run.getSteps().add(coordinatorStep);
        updateAgent(run, COORDINATOR_AGENT, MultiAgentStepStatus.SUCCESS, coordinatorStep.getOutputSummary());

        String planSummary = buildPlanSummary(request);
        MultiAgentStep plannerStep = successStep(2, PLANNER_AGENT, MultiAgentRole.PLANNER,
                MultiAgentActionType.PLAN, "基于用户问题生成安全计划摘要", planSummary);
        run.getSteps().add(plannerStep);
        updateAgent(run, PLANNER_AGENT, MultiAgentStepStatus.SUCCESS, planSummary);

        if (properties.getMultiAgent().isRecordMessages()) {
            run.getMessages().add(MultiAgentMessage.builder()
                    .messageId("msg-" + runId + "-1")
                    .fromAgent(COORDINATOR_AGENT)
                    .toAgent(PLANNER_AGENT)
                    .messageType(MultiAgentMessageType.TASK)
                    .contentSummary("请求 PlannerAgent 生成协作计划摘要")
                    .structuredData(Map.of("mode", request.getMode() == null ? "default" : request.getMode()))
                    .createdAt(Instant.now())
                    .build());
            run.getMessages().add(MultiAgentMessage.builder()
                    .messageId("msg-" + runId + "-2")
                    .fromAgent(PLANNER_AGENT)
                    .toAgent(COORDINATOR_AGENT)
                    .messageType(MultiAgentMessageType.PLAN_SUMMARY)
                    .contentSummary(planSummary)
                    .structuredData(Map.of("phase", "10.1", "executedExternalActions", false))
                    .createdAt(Instant.now())
                    .build());
        }

        String answer = "已创建 Multi-Agent 协作运行骨架：CoordinatorAgent 负责调度与状态记录，"
                + "PlannerAgent 已生成计划摘要；Phase 10.1 暂不执行真实 RAG、Tool、Workflow 或 MCP 调用。";
        long latencyMs = System.currentTimeMillis() - started;
        run.setFinalAnswer(answer);
        run.setSuccess(true);
        run.setStatus(MultiAgentRunStatus.SUCCESS);
        run.setFinishedAt(Instant.now());
        run.setLatencyMs(latencyMs);
        runStore.save(run);

        log.info("AI multi-agent run finished, tenantId={}, userId={}, runId={}, multiAgentEnabled={}, agentName={}, agentRole={}, actionType={}, status={}, maxRounds={}, maxAgents={}, maxToolCalls={}, latencyMs={}",
                context.tenantId(), context.userId(), runId, properties.getMultiAgent().isEnabled(),
                PLANNER_AGENT, MultiAgentRole.PLANNER, MultiAgentActionType.PLAN, run.getStatus(),
                properties.getMultiAgent().getMaxRounds(), properties.getMultiAgent().getMaxAgents(),
                properties.getMultiAgent().getMaxToolCalls(), latencyMs);
        return toResponse(run);
    }

    public MultiAgentChatResponse getRun(String runId) {
        return runStore.get(runId)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Multi-Agent run not found"));
    }

    public List<MultiAgentChatResponse> listRuns(int limit) {
        return runStore.list(limit).stream()
                .map(this::toResponse)
                .toList();
    }

    private MultiAgentAgentState toInitialState(AgentRoleDefinition definition) {
        return MultiAgentAgentState.builder()
                .agentName(definition.getAgentName())
                .role(definition.getRole())
                .status(MultiAgentStepStatus.PENDING)
                .summary(definition.getDescription())
                .build();
    }

    private MultiAgentStep successStep(int stepNo, String agentName, MultiAgentRole role,
                                       MultiAgentActionType actionType, String inputSummary, String outputSummary) {
        Instant now = Instant.now();
        return MultiAgentStep.builder()
                .stepId("step-" + stepNo)
                .stepNo(stepNo)
                .agentName(agentName)
                .agentRole(role)
                .actionType(actionType)
                .status(MultiAgentStepStatus.SUCCESS)
                .inputSummary(inputSummary)
                .outputSummary(outputSummary)
                .startedAt(now)
                .finishedAt(now)
                .latencyMs(0)
                .build();
    }

    private void updateAgent(MultiAgentRun run, String agentName, MultiAgentStepStatus status, String summary) {
        run.getAgents().stream()
                .filter(agent -> agentName.equals(agent.getAgentName()))
                .findFirst()
                .ifPresent(agent -> {
                    agent.setStatus(status);
                    agent.setSummary(summary);
                });
    }

    private String buildPlanSummary(MultiAgentChatRequest request) {
        String message = request.getMessage() == null ? "" : request.getMessage();
        boolean hasToolIntent = message.contains("物料") || message.contains("库存") || message.contains("订单");
        boolean hasRagIntent = message.contains("解释") || message.contains("规则") || message.contains("口径");
        if (hasRagIntent && hasToolIntent) {
            return "识别为后续可扩展的 RAG + Tool 多 Agent 协作任务";
        }
        if (hasToolIntent) {
            return "识别为后续可扩展的 Tool 多 Agent 协作任务";
        }
        if (hasRagIntent) {
            return "识别为后续可扩展的 Knowledge 多 Agent 协作任务";
        }
        return "识别为通用 Multi-Agent 协作任务，当前阶段仅记录计划摘要";
    }

    private String safeText(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String sanitized = value
                .replaceAll("(?i)authorization\\s*[:=]\\s*\\S+", "[REDACTED]")
                .replaceAll("(?i)cookie\\s*[:=]\\s*\\S+", "[REDACTED]")
                .replaceAll("(?i)token\\s*[:=]\\s*\\S+", "[REDACTED]");
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
    }

    private MultiAgentChatResponse toResponse(MultiAgentRun run) {
        return MultiAgentChatResponse.builder()
                .runId(run.getRunId())
                .status(run.getStatus())
                .answer(run.getFinalAnswer())
                .agents(run.getAgents().stream().map(this::toAgentView).toList())
                .steps(run.getSteps().stream().map(this::toStepView).toList())
                .messages(run.getMessages().stream().map(this::toMessageView).toList())
                .latencyMs(run.getLatencyMs())
                .build();
    }

    private MultiAgentAgentView toAgentView(MultiAgentAgentState state) {
        return MultiAgentAgentView.builder()
                .agentName(state.getAgentName())
                .role(state.getRole())
                .status(state.getStatus())
                .summary(state.getSummary())
                .build();
    }

    private MultiAgentStepView toStepView(MultiAgentStep step) {
        return MultiAgentStepView.builder()
                .stepNo(step.getStepNo())
                .agentName(step.getAgentName())
                .agentRole(step.getAgentRole())
                .actionType(step.getActionType())
                .status(step.getStatus())
                .inputSummary(step.getInputSummary())
                .outputSummary(step.getOutputSummary())
                .errorCode(step.getErrorCode())
                .errorMessage(step.getErrorMessage())
                .latencyMs(step.getLatencyMs())
                .build();
    }

    private MultiAgentMessageView toMessageView(MultiAgentMessage message) {
        return MultiAgentMessageView.builder()
                .fromAgent(message.getFromAgent())
                .toAgent(message.getToAgent())
                .messageType(message.getMessageType())
                .contentSummary(message.getContentSummary())
                .structuredData(message.getStructuredData())
                .build();
    }
}
