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

/**
 * Tool Calling Orchestrator 最小服务。
 *
 * <p>Phase 4.14 将 plan 构造交给 Orchestrator Planner，本服务只负责 run/step 生命周期记录。</p>
 */
@Slf4j
@Service
public class ToolCallingOrchestratorService {

    private final AiAgentProperties properties;
    private final ToolOrchestrationRunStore runStore;
    private final ToolOrchestrationStepSummaryBuilder summaryBuilder;
    private final ToolOrchestrationPlannerService plannerService;

    public ToolCallingOrchestratorService(AiAgentProperties properties,
                                          ToolOrchestrationRunStore runStore,
                                          ToolOrchestrationStepSummaryBuilder summaryBuilder,
                                          ToolOrchestrationPlannerService plannerService) {
        this.properties = properties;
        this.runStore = runStore;
        this.summaryBuilder = summaryBuilder;
        this.plannerService = plannerService;
    }

    /**
     * 创建 run 记录；Orchestrator 未启用或关闭记录时返回 null。
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
        AiAgentProperties.OrchestratorProperties orchestrator = orchestratorProperties();
        log.info("AI tool orchestration run started, tenantId={}, userId={}, runId={}, plannerMode={}, answerMode={}, planMode={}, maxSteps={}, multiStepEnabled={}, dryRunEnabled={}",
                context.tenantId(), context.userId(), runId, plannerMode, answerMode,
                orchestrator.getPlanMode(), normalizedMaxSteps(orchestrator), orchestrator.isMultiStepEnabled(),
                orchestrator.isDryRunEnabled());
        return run;
    }

    /**
     * 基于当前单步 Tool plan 构造 Orchestration plan，并启动第一个真实执行步骤。
     */
    public void startStep(ToolOrchestrationRun run, ToolCallingPlan plan) {
        if (run == null || plan == null) {
            return;
        }
        ToolOrchestrationPlan orchestrationPlan = plannerService.buildPlan(run, plan);
        ToolOrchestrationStep firstStep = orchestrationPlan.getSteps().get(0);
        run.setPlan(orchestrationPlan);
        run.setSteps(orchestrationPlan.getSteps());
        runStore.save(run);
        AiAgentProperties.OrchestratorProperties orchestrator = orchestratorProperties();
        log.info("AI tool orchestration step started, tenantId={}, userId={}, runId={}, planId={}, planMode={}, generatedBy={}, selectedTool={}, stepNo={}, stepRef={}, stepStatus={}, maxSteps={}, multiStepEnabled={}, dryRunEnabled={}",
                run.getTenantId(), run.getUserId(), run.getRunId(), orchestrationPlan.getPlanId(),
                orchestrationPlan.getMode(), orchestrationPlan.getGeneratedBy(), firstStep.getToolName(),
                firstStep.getStepNo(), firstStep.getStepRef(), firstStep.getStatus(), orchestrationPlan.getMaxSteps(),
                orchestrator.isMultiStepEnabled(), orchestrator.isDryRunEnabled());
    }

    /**
     * 结束当前 RUNNING 步骤并保存安全执行摘要。
     */
    public void finishStep(ToolOrchestrationRun run, ToolCallingExecutionView execution) {
        if (run == null || run.getSteps().isEmpty() || execution == null) {
            return;
        }
        ToolOrchestrationStep step = findRunningStep(run);
        if (step == null) {
            return;
        }
        Instant finishedAt = Instant.now();
        ToolOrchestrationExecutionSummary executionSummary = toSummary(execution);
        step.setExecution(executionSummary);
        step.setOutputSummary(summaryBuilder.buildOutputSummary(executionSummary));
        step.setStatus(execution.isSuccess() ? ToolOrchestrationStepStatus.SUCCESS : ToolOrchestrationStepStatus.FAILED);
        step.setFinishedAt(finishedAt);
        step.setLatencyMs(step.getStartedAt() == null ? execution.getLatencyMs() :
                Duration.between(step.getStartedAt(), finishedAt).toMillis());
        updateSkippedStepsAfterFinishedStep(run, step, execution.isSuccess());
        runStore.save(run);
        log.info("AI tool orchestration step finished, tenantId={}, userId={}, runId={}, planId={}, planMode={}, generatedBy={}, selectedTool={}, stepNo={}, stepRef={}, stepStatus={}, success={}, errorCode={}, latencyMs={}, skipReason={}",
                run.getTenantId(), run.getUserId(), run.getRunId(), planId(run), planMode(run), generatedBy(run),
                step.getToolName(), step.getStepNo(), step.getStepRef(), step.getStatus(), execution.isSuccess(),
                execution.getErrorCode(), step.getLatencyMs(), step.getSkipReason());
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
        log.info("AI tool orchestration run finished, tenantId={}, userId={}, runId={}, planId={}, planMode={}, generatedBy={}, success={}, latencyMs={}",
                run.getTenantId(), run.getUserId(), run.getRunId(), planId(run), planMode(run), generatedBy(run),
                success, latencyMs);
    }

    public List<ToolOrchestrationRun> listRuns(Integer limit) {
        return runStore.list(limit);
    }

    public ToolOrchestrationRun getRun(String runId) {
        return runStore.findByRunId(runId).orElse(null);
    }

    private void updateSkippedStepsAfterFinishedStep(ToolOrchestrationRun run,
                                                     ToolOrchestrationStep finishedStep,
                                                     boolean success) {
        for (ToolOrchestrationStep step : run.getSteps()) {
            if (step.getStepNo() <= finishedStep.getStepNo() || step.getStatus() != ToolOrchestrationStepStatus.SKIPPED) {
                continue;
            }
            step.setInputSummary(summaryBuilder.buildInputSummary(finishedStep));
            if (!success) {
                step.setSkipReason("previous step failed; real Tool is not executed");
            }
            if (step.getFinishedAt() == null) {
                step.setFinishedAt(Instant.now());
            }
        }
    }

    private ToolOrchestrationStep findRunningStep(ToolOrchestrationRun run) {
        return run.getSteps().stream()
                .filter(step -> step.getStatus() == ToolOrchestrationStepStatus.RUNNING)
                .findFirst()
                .orElse(null);
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

    private boolean shouldRecord() {
        AiAgentProperties.OrchestratorProperties orchestrator = orchestratorProperties();
        return orchestrator.isEnabled() && orchestrator.isRecordRuns();
    }

    private AiAgentProperties.OrchestratorProperties orchestratorProperties() {
        return properties.getToolCalling().getOrchestrator();
    }

    private int normalizedMaxSteps(AiAgentProperties.OrchestratorProperties orchestrator) {
        return Math.max(1, orchestrator.getMaxSteps());
    }

    private String planId(ToolOrchestrationRun run) {
        return run.getPlan() == null ? null : run.getPlan().getPlanId();
    }

    private ToolOrchestrationPlanMode planMode(ToolOrchestrationRun run) {
        return run.getPlan() == null ? null : run.getPlan().getMode();
    }

    private String generatedBy(ToolOrchestrationRun run) {
        return run.getPlan() == null ? null : run.getPlan().getGeneratedBy();
    }
}
