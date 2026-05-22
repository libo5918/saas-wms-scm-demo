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
 * <p>Phase 4.13 引入显式 plan 与 dry-run 多步骤表达，但默认仍只执行单步 Tool Calling。</p>
 */
@Slf4j
@Service
public class ToolCallingOrchestratorService {

    private final AiAgentProperties properties;
    private final ToolOrchestrationRunStore runStore;
    private final ToolOrchestrationStepSummaryBuilder summaryBuilder;

    public ToolCallingOrchestratorService(AiAgentProperties properties,
                                          ToolOrchestrationRunStore runStore,
                                          ToolOrchestrationStepSummaryBuilder summaryBuilder) {
        this.properties = properties;
        this.runStore = runStore;
        this.summaryBuilder = summaryBuilder;
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
     * 根据当前 Tool plan 构造显式 Orchestration plan，并启动第一个真实执行步骤。
     */
    public void startStep(ToolOrchestrationRun run, ToolCallingPlan plan) {
        if (run == null || plan == null) {
            return;
        }
        AiAgentProperties.OrchestratorProperties orchestrator = orchestratorProperties();
        ToolOrchestrationPlanMode mode = resolvePlanMode(run, orchestrator);
        int maxSteps = resolveMaxSteps(mode, orchestrator);

        ToolOrchestrationStep firstStep = ToolOrchestrationStep.builder()
                .stepId(stepId(run.getRunId(), 1))
                .stepNo(1)
                .toolName(plan.selectedTool())
                .arguments(copyArguments(plan.toolArguments()))
                .reason(plan.reason())
                .dependsOnStepIds(List.of())
                .inputSummary("")
                .status(ToolOrchestrationStepStatus.RUNNING)
                .startedAt(Instant.now())
                .build();

        List<ToolOrchestrationStep> plannedSteps = new ArrayList<>();
        plannedSteps.add(firstStep);
        if (shouldCreateDryRunStep(run, mode, orchestrator, maxSteps)) {
            plannedSteps.add(ToolOrchestrationStep.builder()
                    .stepId(stepId(run.getRunId(), 2))
                    .stepNo(2)
                    .toolName("orchestrator.futureStep")
                    .arguments(Map.of())
                    .reason("Phase 4.13 dry-run placeholder for future multi-step orchestration")
                    .dependsOnStepIds(List.of(firstStep.getStepId()))
                    .inputSummary("等待前置步骤输出摘要")
                    .status(ToolOrchestrationStepStatus.SKIPPED)
                    .skipReason("multi-step dry-run only; real Tool is not executed")
                    .finishedAt(Instant.now())
                    .build());
        }

        ToolOrchestrationPlan orchestrationPlan = ToolOrchestrationPlan.builder()
                .planId(run.getRunId() + "-plan-1")
                .runId(run.getRunId())
                .mode(mode)
                .objective(trimObjective(run.getUserMessage()))
                .steps(plannedSteps)
                .maxSteps(maxSteps)
                .generatedBy(mode == ToolOrchestrationPlanMode.SINGLE_STEP ? "service-single-step" : "service-dry-run")
                .createdAt(Instant.now())
                .build();

        run.setPlan(orchestrationPlan);
        run.setSteps(plannedSteps);
        runStore.save(run);
        log.info("AI tool orchestration step started, tenantId={}, userId={}, runId={}, planId={}, planMode={}, selectedTool={}, stepNo={}, stepStatus={}, maxSteps={}, multiStepEnabled={}, dryRunEnabled={}",
                run.getTenantId(), run.getUserId(), run.getRunId(), orchestrationPlan.getPlanId(), mode,
                firstStep.getToolName(), firstStep.getStepNo(), firstStep.getStatus(), maxSteps,
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
        log.info("AI tool orchestration step finished, tenantId={}, userId={}, runId={}, planId={}, planMode={}, selectedTool={}, stepNo={}, stepStatus={}, success={}, errorCode={}, latencyMs={}",
                run.getTenantId(), run.getUserId(), run.getRunId(), planId(run), planMode(run), step.getToolName(),
                step.getStepNo(), step.getStatus(), execution.isSuccess(), execution.getErrorCode(), step.getLatencyMs());
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
        log.info("AI tool orchestration run finished, tenantId={}, userId={}, runId={}, planId={}, planMode={}, success={}, latencyMs={}",
                run.getTenantId(), run.getUserId(), run.getRunId(), planId(run), planMode(run), success, latencyMs);
    }

    public List<ToolOrchestrationRun> listRuns(Integer limit) {
        return runStore.list(limit);
    }

    public ToolOrchestrationRun getRun(String runId) {
        return runStore.findByRunId(runId).orElse(null);
    }

    private boolean shouldRecord() {
        AiAgentProperties.OrchestratorProperties orchestrator = orchestratorProperties();
        return orchestrator.isEnabled() && orchestrator.isRecordRuns();
    }

    private AiAgentProperties.OrchestratorProperties orchestratorProperties() {
        return properties.getToolCalling().getOrchestrator();
    }

    private ToolOrchestrationPlanMode resolvePlanMode(ToolOrchestrationRun run,
                                                      AiAgentProperties.OrchestratorProperties orchestrator) {
        if (run.getRequestedTool() != null && !run.getRequestedTool().isBlank()) {
            return ToolOrchestrationPlanMode.SINGLE_STEP;
        }
        if (orchestrator.isMultiStepEnabled()
                && orchestrator.isDryRunEnabled()
                && orchestrator.getPlanMode() == ToolOrchestrationPlanMode.MULTI_STEP_DRY_RUN) {
            return ToolOrchestrationPlanMode.MULTI_STEP_DRY_RUN;
        }
        return ToolOrchestrationPlanMode.SINGLE_STEP;
    }

    private int resolveMaxSteps(ToolOrchestrationPlanMode mode,
                                AiAgentProperties.OrchestratorProperties orchestrator) {
        if (mode == ToolOrchestrationPlanMode.SINGLE_STEP) {
            return 1;
        }
        return normalizedMaxSteps(orchestrator);
    }

    private int normalizedMaxSteps(AiAgentProperties.OrchestratorProperties orchestrator) {
        return Math.max(1, orchestrator.getMaxSteps());
    }

    private boolean shouldCreateDryRunStep(ToolOrchestrationRun run,
                                           ToolOrchestrationPlanMode mode,
                                           AiAgentProperties.OrchestratorProperties orchestrator,
                                           int maxSteps) {
        return mode == ToolOrchestrationPlanMode.MULTI_STEP_DRY_RUN
                && orchestrator.isMultiStepEnabled()
                && orchestrator.isDryRunEnabled()
                && maxSteps > 1
                && (run.getRequestedTool() == null || run.getRequestedTool().isBlank());
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

    private String stepId(String runId, int stepNo) {
        return runId + "-step-" + stepNo;
    }

    private String trimObjective(String userMessage) {
        if (userMessage == null) {
            return "";
        }
        String normalized = userMessage.strip();
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120);
    }

    private String planId(ToolOrchestrationRun run) {
        return run.getPlan() == null ? null : run.getPlan().getPlanId();
    }

    private ToolOrchestrationPlanMode planMode(ToolOrchestrationRun run) {
        return run.getPlan() == null ? null : run.getPlan().getMode();
    }
}
