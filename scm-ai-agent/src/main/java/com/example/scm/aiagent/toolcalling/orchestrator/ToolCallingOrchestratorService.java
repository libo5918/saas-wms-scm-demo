package com.example.scm.aiagent.toolcalling.orchestrator;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.agent.service.RagToolIntentRouter;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.tool.dto.ToolInvokeRequest;
import com.example.scm.aiagent.tool.dto.ToolResponse;
import com.example.scm.aiagent.tool.service.ToolInvocationService;
import com.example.scm.aiagent.tool.service.ToolRegistry;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatRequest;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecutionView;
import com.example.scm.aiagent.toolcalling.display.ToolCallingDisplaySchemaBuilder;
import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayData;
import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayField;
import com.example.scm.aiagent.toolcalling.model.ToolCallingPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tool Calling Orchestrator 最小服务。
 *
 * <p>Phase 4.14 将 plan 构造交给 Orchestrator Planner，本服务只负责 run/step 生命周期记录。</p>
 */
@Slf4j
@Service
public class ToolCallingOrchestratorService {

    private static final String PLACEHOLDER_TOOL = "orchestrator.futureStep";
    private static final Set<String> SAFE_EXECUTION_FIELD_KEYS = Set.of(
            "id", "materialId", "materialCode", "warehouseId", "warehouseCode", "locationId", "locationCode",
            "availableQty", "lockedQty", "unit");

    private final AiAgentProperties properties;
    private final ToolOrchestrationRunStore runStore;
    private final ToolOrchestrationStepSummaryBuilder summaryBuilder;
    private final ToolOrchestrationPlannerService plannerService;
    private final ToolOrchestrationParameterResolver parameterResolver;
    private final ToolInvocationService toolInvocationService;
    private final ToolCallingDisplaySchemaBuilder displaySchemaBuilder;
    private final ToolRegistry toolRegistry;
    private final RagToolIntentRouter intentRouter;

    public ToolCallingOrchestratorService(AiAgentProperties properties,
                                          ToolOrchestrationRunStore runStore,
                                          ToolOrchestrationStepSummaryBuilder summaryBuilder,
                                          ToolOrchestrationPlannerService plannerService,
                                          ToolOrchestrationParameterResolver parameterResolver,
                                          ToolInvocationService toolInvocationService,
                                          ToolCallingDisplaySchemaBuilder displaySchemaBuilder,
                                          ToolRegistry toolRegistry,
                                          RagToolIntentRouter intentRouter) {
        this.properties = properties;
        this.runStore = runStore;
        this.summaryBuilder = summaryBuilder;
        this.plannerService = plannerService;
        this.parameterResolver = parameterResolver;
        this.toolInvocationService = toolInvocationService;
        this.displaySchemaBuilder = displaySchemaBuilder;
        this.toolRegistry = toolRegistry;
        this.intentRouter = intentRouter;
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
        step.setExecuted(true);
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
     * 在显式 controlled 配置开启时执行第二个只读 Tool。
     *
     * <p>Phase 4.15 只允许第二步真实执行，且必须走 ToolInvocationService，
     * 从而复用权限、runtime 保护和 audit 链路。</p>
     */
    public void executeControlledFollowUp(ToolOrchestrationRun run, AgentRequestContext context) {
        if (!canAttemptControlledFollowUp(run)) {
            return;
        }
        ToolOrchestrationStep firstStep = run.getSteps().get(0);
        ToolOrchestrationStep secondStep = run.getSteps().get(1);
        if (firstStep.getStatus() != ToolOrchestrationStepStatus.SUCCESS) {
            markSkipped(secondStep, "previous step failed; controlled follow-up is not executed", false, false);
            runStore.save(run);
            return;
        }
        if (!intentRouter.hasInventoryFollowUpIntent(run.getUserMessage())) {
            markSkipped(secondStep, "用户问题未表达库存查询意图，受控第二步库存 Tool 不执行", false, false);
            secondStep.setInputResolved(false);
            secondStep.setInputResolveError("inventory follow-up intent is missing");
            runStore.save(run);
            return;
        }
        if (!isExecutableReadOnlyTool(secondStep)) {
            markSkipped(secondStep, "follow-up tool is not registered readOnly", false, false);
            runStore.save(run);
            return;
        }
        ToolOrchestrationParameterResolveResult resolveResult =
                parameterResolver.resolve(run, firstStep, secondStep);
        secondStep.setInputResolved(resolveResult.resolved());
        secondStep.setInputResolveError(resolveResult.error());
        if (!resolveResult.resolved()) {
            markSkipped(secondStep, resolveResult.error(), false, false);
            runStore.save(run);
            return;
        }

        secondStep.setArguments(resolveResult.arguments());
        secondStep.setExecutable(true);
        secondStep.setExecuted(false);
        secondStep.setStatus(ToolOrchestrationStepStatus.RUNNING);
        secondStep.setSkipReason(null);
        secondStep.setStartedAt(Instant.now());
        secondStep.setFinishedAt(null);
        runStore.save(run);

        ToolInvokeRequest request = new ToolInvokeRequest();
        request.setRunId(run.getRunId());
        request.setToolName(secondStep.getToolName());
        request.setParameters(resolveResult.arguments());
        ToolResponse response = toolInvocationService.invoke(request, context);
        ToolCallingExecutionView execution = toExecutionView(response);

        Instant finishedAt = Instant.now();
        ToolOrchestrationExecutionSummary executionSummary = toSummary(execution);
        secondStep.setExecution(executionSummary);
        secondStep.setOutputSummary(summaryBuilder.buildOutputSummary(executionSummary));
        secondStep.setStatus(response.isSuccess() ? ToolOrchestrationStepStatus.SUCCESS : ToolOrchestrationStepStatus.FAILED);
        secondStep.setExecuted(true);
        secondStep.setFinishedAt(finishedAt);
        secondStep.setLatencyMs(secondStep.getStartedAt() == null ? execution.getLatencyMs() :
                Duration.between(secondStep.getStartedAt(), finishedAt).toMillis());
        runStore.save(run);
        log.info("AI tool orchestration controlled follow-up finished, tenantId={}, userId={}, runId={}, planId={}, planMode={}, generatedBy={}, stepNo={}, stepRef={}, toolName={}, stepStatus={}, executable={}, executed={}, inputResolved={}, success={}, errorCode={}, latencyMs={}",
                run.getTenantId(), run.getUserId(), run.getRunId(), planId(run), planMode(run), generatedBy(run),
                secondStep.getStepNo(), secondStep.getStepRef(), secondStep.getToolName(), secondStep.getStatus(),
                secondStep.getExecutable(), secondStep.getExecuted(), secondStep.getInputResolved(),
                response.isSuccess(), response.getErrorCode(), secondStep.getLatencyMs());
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
            step.setExecutable(false);
            step.setExecuted(false);
            if (step.getFinishedAt() == null) {
                step.setFinishedAt(Instant.now());
            }
        }
    }

    private boolean canAttemptControlledFollowUp(ToolOrchestrationRun run) {
        if (run == null || run.getPlan() == null || run.getSteps() == null || run.getSteps().size() < 2) {
            return false;
        }
        AiAgentProperties.OrchestratorProperties orchestrator = orchestratorProperties();
        return orchestrator.isControlledExecutionEnabled()
                && orchestrator.isMultiStepEnabled()
                && orchestrator.getPlanMode() == ToolOrchestrationPlanMode.MULTI_STEP_CONTROLLED
                && run.getPlan().getMode() == ToolOrchestrationPlanMode.MULTI_STEP_CONTROLLED
                && Math.max(1, orchestrator.getMaxExecutableSteps()) >= 2;
    }

    private boolean isExecutableReadOnlyTool(ToolOrchestrationStep step) {
        if (step == null || PLACEHOLDER_TOOL.equals(step.getToolName())) {
            return false;
        }
        AiAgentProperties.OrchestratorProperties orchestrator = orchestratorProperties();
        if (step.getStepNo() > Math.max(1, orchestrator.getMaxExecutableSteps())
                || step.getStepNo() > 2
                || !orchestrator.isAllowSecondStepReadOnly()) {
            return false;
        }
        return toolRegistry.findDefinition(step.getToolName())
                .map(definition -> definition.isReadOnly())
                .orElse(false);
    }

    private void markSkipped(ToolOrchestrationStep step, String reason, boolean executable, boolean executed) {
        if (step == null) {
            return;
        }
        step.setStatus(ToolOrchestrationStepStatus.SKIPPED);
        step.setSkipReason(reason);
        step.setExecutable(executable);
        step.setExecuted(executed);
        step.setFinishedAt(Instant.now());
    }

    private ToolCallingExecutionView toExecutionView(ToolResponse response) {
        Object rawData = response.getData();
        return ToolCallingExecutionView.builder()
                .success(response.isSuccess())
                .toolName(response.getToolName())
                .errorCode(response.getErrorCode())
                .errorMessage(response.getErrorMessage())
                .data(response.isSuccess() ? displaySchemaBuilder.build(response.getToolName(), rawData) : rawData)
                .latencyMs(response.getLatencyMs())
                .build();
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
        Map<String, Object> safeFields = Map.of();
        if (execution.getData() instanceof ToolCallingDisplayData displayData) {
            displayTitle = displayData.displayTitle();
            displaySummary = displayData.displaySummary();
            safeFields = extractSafeFields(execution.getToolName(), displayData);
        }
        return ToolOrchestrationExecutionSummary.builder()
                .success(execution.isSuccess())
                .toolName(execution.getToolName())
                .errorCode(execution.getErrorCode())
                .errorMessage(execution.getErrorMessage())
                .latencyMs(execution.getLatencyMs())
                .displayTitle(displayTitle)
                .displaySummary(displaySummary)
                .safeFields(safeFields)
                .build();
    }

    private Map<String, Object> extractSafeFields(String toolName, ToolCallingDisplayData displayData) {
        Map<String, Object> safeFields = new LinkedHashMap<>();
        if (displayData.displayFields() != null) {
            for (ToolCallingDisplayField field : displayData.displayFields()) {
                if (field != null && SAFE_EXECUTION_FIELD_KEYS.contains(field.key()) && field.value() != null) {
                    safeFields.put(normalizeSafeFieldKey(toolName, field.key()), field.value());
                }
            }
        }
        if (displayData.rawData() instanceof Map<?, ?> rawMap) {
            rawMap.forEach((key, value) -> {
                String textKey = String.valueOf(key);
                if (SAFE_EXECUTION_FIELD_KEYS.contains(textKey) && value != null) {
                    safeFields.putIfAbsent(normalizeSafeFieldKey(toolName, textKey), value);
                }
            });
        }
        return safeFields.isEmpty() ? Map.of() : Map.copyOf(safeFields);
    }

    private String normalizeSafeFieldKey(String toolName, String key) {
        if ("id".equals(key) && "mdm.getMaterial".equals(toolName)) {
            return "materialId";
        }
        return key;
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
