package com.example.scm.aiagent.toolcalling.orchestrator;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatRequest;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecutionView;
import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayData;
import com.example.scm.aiagent.toolcalling.model.ToolCallingPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tool Calling Orchestrator 最小服务。
 *
 * <p>Phase 4.12 只记录单步 run/step 状态，不接管复杂多步规划。</p>
 */
@Slf4j
@Service
public class ToolCallingOrchestratorService {

    private final AiAgentProperties properties;
    private final ToolOrchestrationRunStore runStore;

    public ToolCallingOrchestratorService(AiAgentProperties properties,
                                          ToolOrchestrationRunStore runStore) {
        this.properties = properties;
        this.runStore = runStore;
    }

    /**
     * 创建 run 记录。Orchestrator 未启用或关闭记录时返回 null。
     */
    public ToolOrchestrationRun startRun(ToolCallingChatRequest request,
                                         AgentRequestContext context,
                                         String runId,
                                         String plannerMode,
                                         String answerMode) {
        if (!shouldRecord()) {
            return null;
        }
        ToolOrchestrationRun run = ToolOrchestrationRun.builder()
                .runId(runId)
                .tenantId(context.tenantId())
                .userId(context.userId())
                .userMessage(request.getMessage())
                .plannerMode(plannerMode)
                .answerMode(answerMode)
                .requestedTool(request.getRequestedTool())
                .requestedDomain(request.getRequestedDomain())
                .requestedCategory(request.getRequestedCategory())
                .routeTags(request.getRouteTags() == null ? List.of() : List.copyOf(request.getRouteTags()))
                .steps(new ArrayList<>())
                .createdAt(Instant.now())
                .build();
        runStore.save(run);
        log.info("AI tool orchestration run started, tenantId={}, userId={}, runId={}, plannerMode={}, answerMode={}",
                context.tenantId(), context.userId(), runId, plannerMode, answerMode);
        return run;
    }

    /**
     * 根据当前单步 plan 创建并标记 RUNNING。
     */
    public void startStep(ToolOrchestrationRun run, ToolCallingPlan plan) {
        if (run == null || plan == null) {
            return;
        }
        ToolOrchestrationStep step = ToolOrchestrationStep.builder()
                .stepId(run.getRunId() + "-step-1")
                .stepNo(1)
                .toolName(plan.selectedTool())
                .arguments(copyArguments(plan.toolArguments()))
                .reason(plan.reason())
                .status(ToolOrchestrationStepStatus.RUNNING)
                .startedAt(Instant.now())
                .build();
        run.getSteps().add(step);
        runStore.save(run);
        log.info("AI tool orchestration step started, tenantId={}, userId={}, runId={}, selectedTool={}, stepNo={}, stepStatus={}",
                run.getTenantId(), run.getUserId(), run.getRunId(), step.getToolName(), step.getStepNo(), step.getStatus());
    }

    /**
     * 结束当前单步并保存执行摘要。
     */
    public void finishStep(ToolOrchestrationRun run, ToolCallingExecutionView execution) {
        if (run == null || run.getSteps().isEmpty() || execution == null) {
            return;
        }
        ToolOrchestrationStep step = run.getSteps().get(run.getSteps().size() - 1);
        Instant finishedAt = Instant.now();
        step.setExecution(toSummary(execution));
        step.setStatus(execution.isSuccess() ? ToolOrchestrationStepStatus.SUCCESS : ToolOrchestrationStepStatus.FAILED);
        step.setFinishedAt(finishedAt);
        step.setLatencyMs(step.getStartedAt() == null ? execution.getLatencyMs() :
                Duration.between(step.getStartedAt(), finishedAt).toMillis());
        runStore.save(run);
        log.info("AI tool orchestration step finished, tenantId={}, userId={}, runId={}, selectedTool={}, stepNo={}, stepStatus={}, success={}, errorCode={}, latencyMs={}",
                run.getTenantId(), run.getUserId(), run.getRunId(), step.getToolName(), step.getStepNo(),
                step.getStatus(), execution.isSuccess(), execution.getErrorCode(), step.getLatencyMs());
    }

    /**
     * 结束 run 并记录最终答案摘要。
     */
    public void finishRun(ToolOrchestrationRun run, boolean success, String finalAnswer, long latencyMs) {
        if (run == null) {
            return;
        }
        run.setSuccess(success);
        run.setFinalAnswer(finalAnswer);
        run.setFinishedAt(Instant.now());
        run.setLatencyMs(latencyMs);
        runStore.save(run);
        log.info("AI tool orchestration run finished, tenantId={}, userId={}, runId={}, success={}, latencyMs={}",
                run.getTenantId(), run.getUserId(), run.getRunId(), success, latencyMs);
    }

    public List<ToolOrchestrationRun> listRuns(Integer limit) {
        return runStore.list(limit);
    }

    public ToolOrchestrationRun getRun(String runId) {
        return runStore.findByRunId(runId).orElse(null);
    }

    private boolean shouldRecord() {
        AiAgentProperties.OrchestratorProperties orchestrator = properties.getToolCalling().getOrchestrator();
        return orchestrator.isEnabled() && orchestrator.isRecordRuns();
    }

    private Map<String, Object> copyArguments(Map<String, Object> arguments) {
        return arguments == null ? Map.of() : Map.copyOf(arguments);
    }

    private ToolOrchestrationExecutionSummary toSummary(ToolCallingExecutionView execution) {
        String displayTitle = null;
        String displaySummary = null;
        if (execution.getData() instanceof ToolCallingDisplayData displayData) {
            displayTitle = displayData.displayTitle();
            displaySummary = displayData.displaySummary();
        }
        return ToolOrchestrationExecutionSummary.builder()
                .success(execution.isSuccess())
                .toolName(execution.getToolName())
                .errorCode(execution.getErrorCode())
                .errorMessage(execution.getErrorMessage())
                .latencyMs(execution.getLatencyMs())
                .displayTitle(displayTitle)
                .displaySummary(displaySummary)
                .build();
    }
}
