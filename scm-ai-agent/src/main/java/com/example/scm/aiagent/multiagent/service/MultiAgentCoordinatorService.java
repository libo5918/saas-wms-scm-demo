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
import com.example.scm.aiagent.multiagent.model.MultiAgentIntentType;
import com.example.scm.aiagent.multiagent.model.MultiAgentMessage;
import com.example.scm.aiagent.multiagent.model.MultiAgentMessageType;
import com.example.scm.aiagent.multiagent.model.MultiAgentPlan;
import com.example.scm.aiagent.multiagent.model.MultiAgentReviewResult;
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

/** Multi-Agent Coordinator，负责单轮受控协作、状态记录与最终答案汇总。 */
@Slf4j
@Service
public class MultiAgentCoordinatorService {

    private static final String COORDINATOR_AGENT = "CoordinatorAgent";
    private static final String PLANNER_AGENT = "PlannerAgent";
    private static final String KNOWLEDGE_AGENT = "KnowledgeAgent";
    private static final String TOOL_AGENT = "ToolAgent";
    private static final String REVIEWER_AGENT = "ReviewerAgent";

    private final AiAgentProperties properties;
    private final MultiAgentDefinitionRegistry definitionRegistry;
    private final MultiAgentRunStore runStore;
    private final MultiAgentPlannerService plannerService;
    private final MultiAgentKnowledgeService knowledgeService;
    private final MultiAgentToolService toolService;
    private final MultiAgentReviewService reviewService;

    public MultiAgentCoordinatorService(AiAgentProperties properties,
                                        MultiAgentDefinitionRegistry definitionRegistry,
                                        MultiAgentRunStore runStore,
                                        MultiAgentPlannerService plannerService,
                                        MultiAgentKnowledgeService knowledgeService,
                                        MultiAgentToolService toolService,
                                        MultiAgentReviewService reviewService) {
        this.properties = properties;
        this.definitionRegistry = definitionRegistry;
        this.runStore = runStore;
        this.plannerService = plannerService;
        this.knowledgeService = knowledgeService;
        this.toolService = toolService;
        this.reviewService = reviewService;
    }

    public MultiAgentChatResponse chat(MultiAgentChatRequest request, AgentRequestContext context) {
        long started = System.currentTimeMillis();
        String runId = StringUtils.hasText(request.getRunId()) ? request.getRunId() : UUID.randomUUID().toString();
        MultiAgentDefinition definition = definitionRegistry.getDefaultDefinition();

        MultiAgentRun run = MultiAgentRun.builder()
                .runId(runId)
                .tenantId(context.tenantId())
                .userId(context.userId())
                .userMessage(safeText(request.getMessage(), 300))
                .status(MultiAgentRunStatus.RUNNING)
                .createdAt(Instant.now())
                .build();
        run.getAgents().addAll(definition.getAgents().stream().map(this::toInitialState).toList());

        addSuccessStep(run, 1, COORDINATOR_AGENT, MultiAgentRole.COORDINATOR, MultiAgentActionType.NOOP,
                "用户任务进入 Multi-Agent Coordinator", "已接收用户任务，开始单轮受控协作");

        MultiAgentPlan plan = plannerService.plan(request);
        run.setIntentType(plan.getIntentType());
        run.setPlanSummary(plan.toSafeMap());
        addSuccessStep(run, 2, PLANNER_AGENT, MultiAgentRole.PLANNER, MultiAgentActionType.PLAN,
                "基于用户问题生成受控计划", plan.getReason());

        Map<String, Object> rag = knowledgeService.retrieve(request, context, plan, properties.getMultiAgent().isRagEnabled());
        run.setRag(rag);
        addResultStep(run, 3, KNOWLEDGE_AGENT, MultiAgentRole.KNOWLEDGE, MultiAgentActionType.RAG_RETRIEVE,
                "根据 Planner 计划决定是否检索知识库", rag);

        Map<String, Object> tool = toolService.execute(request, context, plan, runId, properties.getMultiAgent().isToolEnabled());
        run.setTool(tool);
        addResultStep(run, 4, TOOL_AGENT, MultiAgentRole.TOOL, MultiAgentActionType.TOOL_CALL,
                "根据 Planner 计划决定是否调用只读 Tool", tool);

        String draftAnswer = buildDraftAnswer(plan, rag, tool);
        MultiAgentReviewResult review = properties.getMultiAgent().isReviewEnabled()
                ? reviewService.review(draftAnswer, rag, tool)
                : MultiAgentReviewResult.builder().passed(true).issues(List.of()).suggestions(List.of()).safetyLevel("SKIPPED").build();
        run.setReview(review.toSafeMap());
        addReviewStep(run, 5, review);

        String finalAnswer = review.isPassed() ? draftAnswer : buildConservativeAnswer(draftAnswer, review);
        addSuccessStep(run, 6, COORDINATOR_AGENT, MultiAgentRole.COORDINATOR, MultiAgentActionType.FINAL_ANSWER,
                "汇总 Planner/Knowledge/Tool/Reviewer 安全摘要", safeText(finalAnswer, 500));

        if (properties.getMultiAgent().isRecordMessages()) {
            recordMessages(run, plan, review);
        }

        long latencyMs = System.currentTimeMillis() - started;
        run.setFinalAnswer(finalAnswer);
        run.setSuccess(true);
        run.setStatus(MultiAgentRunStatus.SUCCESS);
        run.setFinishedAt(Instant.now());
        run.setLatencyMs(latencyMs);
        runStore.save(run);

        log.info("AI multi-agent run finished, tenantId={}, userId={}, runId={}, multiAgentEnabled={}, agentName={}, agentRole={}, actionType={}, status={}, intentType={}, ragRetrievedCount={}, selectedTool={}, reviewPassed={}, maxRounds={}, maxAgents={}, maxToolCalls={}, latencyMs={}",
                context.tenantId(), context.userId(), runId, properties.getMultiAgent().isEnabled(),
                COORDINATOR_AGENT, MultiAgentRole.COORDINATOR, MultiAgentActionType.FINAL_ANSWER, run.getStatus(),
                run.getIntentType(), rag.getOrDefault("retrievedCount", 0), selectedTool(tool), review.isPassed(),
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
        return runStore.list(limit).stream().map(this::toResponse).toList();
    }

    private MultiAgentAgentState toInitialState(AgentRoleDefinition definition) {
        return MultiAgentAgentState.builder()
                .agentName(definition.getAgentName())
                .role(definition.getRole())
                .status(MultiAgentStepStatus.PENDING)
                .summary(definition.getDescription())
                .build();
    }

    private void addSuccessStep(MultiAgentRun run, int stepNo, String agentName, MultiAgentRole role,
                                MultiAgentActionType actionType, String inputSummary, String outputSummary) {
        Instant now = Instant.now();
        MultiAgentStep step = MultiAgentStep.builder()
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
        run.getSteps().add(step);
        updateAgent(run, agentName, MultiAgentStepStatus.SUCCESS, outputSummary);
    }

    private void addResultStep(MultiAgentRun run, int stepNo, String agentName, MultiAgentRole role,
                               MultiAgentActionType actionType, String inputSummary, Map<String, Object> result) {
        String status = String.valueOf(result.getOrDefault("status", "SUCCESS"));
        MultiAgentStepStatus stepStatus = switch (status) {
            case "SKIPPED" -> MultiAgentStepStatus.SKIPPED;
            case "FAILED" -> MultiAgentStepStatus.FAILED;
            default -> MultiAgentStepStatus.SUCCESS;
        };
        Instant now = Instant.now();
        MultiAgentStep step = MultiAgentStep.builder()
                .stepId("step-" + stepNo)
                .stepNo(stepNo)
                .agentName(agentName)
                .agentRole(role)
                .actionType(actionType)
                .status(stepStatus)
                .inputSummary(inputSummary)
                .outputSummary(summary(result))
                .errorCode(String.valueOf(result.getOrDefault("errorCode", "")))
                .errorMessage(String.valueOf(result.getOrDefault("errorMessage", result.getOrDefault("skipReason", ""))))
                .startedAt(now)
                .finishedAt(now)
                .latencyMs(numberValue(result.get("latencyMs")))
                .build();
        run.getSteps().add(step);
        updateAgent(run, agentName, stepStatus, step.getOutputSummary());
    }

    private void addReviewStep(MultiAgentRun run, int stepNo, MultiAgentReviewResult review) {
        MultiAgentStepStatus status = review.isPassed() ? MultiAgentStepStatus.SUCCESS : MultiAgentStepStatus.FAILED;
        Instant now = Instant.now();
        MultiAgentStep step = MultiAgentStep.builder()
                .stepId("step-" + stepNo)
                .stepNo(stepNo)
                .agentName(REVIEWER_AGENT)
                .agentRole(MultiAgentRole.REVIEWER)
                .actionType(MultiAgentActionType.REVIEW)
                .status(status)
                .inputSummary("审查最终回答是否基于事实且不泄露敏感信息")
                .outputSummary(review.isPassed() ? "ReviewerAgent 审查通过" : "ReviewerAgent 发现风险：" + review.getIssues())
                .startedAt(now)
                .finishedAt(now)
                .latencyMs(0)
                .build();
        run.getSteps().add(step);
        updateAgent(run, REVIEWER_AGENT, status, step.getOutputSummary());
    }

    private String buildDraftAnswer(MultiAgentPlan plan, Map<String, Object> rag, Map<String, Object> tool) {
        StringBuilder answer = new StringBuilder("Multi-Agent 单轮协作结果：");
        answer.append("PlannerAgent 将任务识别为 ").append(plan.getIntentType()).append("。");
        if (plan.isNeedRag()) {
            long retrievedCount = numberValue(rag.get("retrievedCount"));
            if (retrievedCount > 0) {
                answer.append("KnowledgeAgent 检索到 ").append(retrievedCount).append(" 条知识片段，可用于解释规则或口径。");
            } else {
                answer.append("KnowledgeAgent 未召回知识库片段，不编造知识库规则。");
            }
        }
        if (plan.isNeedTool()) {
            Map<?, ?> execution = tool.get("execution") instanceof Map<?, ?> map ? map : Map.of();
            boolean success = Boolean.TRUE.equals(execution.get("success"));
            if (success) {
                answer.append("ToolAgent 已完成只读工具查询，")
                        .append(String.valueOf(valueOrDefault(execution, "displaySummary", "已返回工具结果"))).append("。");
            } else {
                answer.append("ToolAgent 查询失败，原因：")
                        .append(String.valueOf(valueOrDefault(execution, "errorMessage",
                                tool.getOrDefault("skipReason", "未知错误")))).append("。");
            }
        }
        if (plan.getIntentType() == MultiAgentIntentType.GENERAL) {
            answer.append("当前未识别到必须执行的 RAG 或 Tool 动作，已完成受控协作骨架记录。");
        }
        return answer.toString();
    }

    private String buildConservativeAnswer(String draftAnswer, MultiAgentReviewResult review) {
        return "ReviewerAgent 发现回答存在风险，已按保守模式返回。风险：" + review.getIssues()
                + "。可参考的安全摘要：" + safeText(draftAnswer, 300);
    }

    private void recordMessages(MultiAgentRun run, MultiAgentPlan plan, MultiAgentReviewResult review) {
        run.getMessages().add(MultiAgentMessage.builder()
                .messageId("msg-" + run.getRunId() + "-plan")
                .fromAgent(PLANNER_AGENT)
                .toAgent(COORDINATOR_AGENT)
                .messageType(MultiAgentMessageType.PLAN_SUMMARY)
                .contentSummary(plan.getReason())
                .structuredData(plan.toSafeMap())
                .createdAt(Instant.now())
                .build());
        run.getMessages().add(MultiAgentMessage.builder()
                .messageId("msg-" + run.getRunId() + "-review")
                .fromAgent(REVIEWER_AGENT)
                .toAgent(COORDINATOR_AGENT)
                .messageType(MultiAgentMessageType.RESULT_SUMMARY)
                .contentSummary(review.isPassed() ? "审查通过" : "审查发现风险")
                .structuredData(review.toSafeMap())
                .createdAt(Instant.now())
                .build());
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

    private String summary(Map<String, Object> result) {
        if (result.containsKey("skipReason")) {
            return "SKIPPED: " + result.get("skipReason");
        }
        if (result.containsKey("retrievedCount")) {
            return "retrievedCount=" + result.get("retrievedCount");
        }
        if (result.containsKey("execution")) {
            Map<?, ?> execution = result.get("execution") instanceof Map<?, ?> map ? map : Map.of();
            return "tool=" + result.get("selectedTool") + ", success=" + execution.get("success")
                    + ", displaySummary=" + execution.get("displaySummary");
        }
        return String.valueOf(result.getOrDefault("status", "SUCCESS"));
    }

    private String selectedTool(Map<String, Object> tool) {
        return String.valueOf(tool.getOrDefault("selectedTool", ""));
    }

    private Object valueOrDefault(Map<?, ?> map, String key, Object defaultValue) {
        Object value = map.get(key);
        return value == null ? defaultValue : value;
    }

    private long numberValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String safeText(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return value == null ? "" : value;
        }
        String sanitized = value
                .replaceAll("(?i)authorization\\s*[:=]\\s*\\S+", "[REDACTED]")
                .replaceAll("(?i)cookie\\s*[:=]\\s*\\S+", "[REDACTED]")
                .replaceAll("(?i)token\\s*[:=]\\s*\\S+", "[REDACTED]")
                .replaceAll("(?i)api\\s*key\\s*[:=]\\s*\\S+", "[REDACTED]");
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
    }

    private MultiAgentChatResponse toResponse(MultiAgentRun run) {
        return MultiAgentChatResponse.builder()
                .runId(run.getRunId())
                .status(run.getStatus())
                .intentType(run.getIntentType())
                .answer(run.getFinalAnswer())
                .planSummary(run.getPlanSummary())
                .rag(run.getRag())
                .tool(run.getTool())
                .review(run.getReview())
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
