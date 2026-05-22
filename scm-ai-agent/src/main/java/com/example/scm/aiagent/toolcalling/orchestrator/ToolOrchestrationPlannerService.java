package com.example.scm.aiagent.toolcalling.orchestrator;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.tool.service.ToolRegistry;
import com.example.scm.aiagent.toolcalling.model.ToolCallingPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Orchestrator 层受控计划构建服务。
 *
 * <p>它不替代 SpringAiToolPlanner，只把当前单步 Tool 计划包装为可演进的 Orchestration plan。</p>
 */
@Slf4j
@Service
public class ToolOrchestrationPlannerService {

    private static final String PLACEHOLDER_TOOL = "orchestrator.futureStep";

    private final AiAgentProperties properties;
    private final ToolRegistry toolRegistry;
    private final ToolOrchestrationStepRefBuilder stepRefBuilder;
    private final ToolOrchestrationPlanValidator validator;

    public ToolOrchestrationPlannerService(AiAgentProperties properties,
                                           ToolRegistry toolRegistry,
                                           ToolOrchestrationStepRefBuilder stepRefBuilder,
                                           ToolOrchestrationPlanValidator validator) {
        this.properties = properties;
        this.toolRegistry = toolRegistry;
        this.stepRefBuilder = stepRefBuilder;
        this.validator = validator;
    }

    /**
     * 基于单步 ToolCallingPlan 构造受控 Orchestration plan。
     */
    public ToolOrchestrationPlan buildPlan(ToolOrchestrationRun run, ToolCallingPlan toolPlan) {
        AiAgentProperties.OrchestratorProperties orchestrator = properties.getToolCalling().getOrchestrator();
        ToolOrchestrationPlanMode mode = resolvePlanMode(run, orchestrator, toolPlan);
        int maxSteps = mode == ToolOrchestrationPlanMode.SINGLE_STEP ? 1 : Math.max(1, orchestrator.getMaxSteps());
        ToolOrchestrationPlan plan = buildPlan(run, toolPlan, mode, maxSteps);
        ToolOrchestrationPlanValidationResult result = validator.validate(plan, maxSteps);
        if (!result.valid()) {
            ToolOrchestrationPlan fallback = buildPlan(run, toolPlan, ToolOrchestrationPlanMode.SINGLE_STEP, 1);
            log.warn("AI tool orchestration plan validation failed, tenantId={}, userId={}, runId={}, planId={}, planMode={}, validationResult={}, fallbackMode={}",
                    run.getTenantId(), run.getUserId(), run.getRunId(), plan.getPlanId(), plan.getMode(), result.reason(),
                    fallback.getMode());
            return fallback;
        }
        log.info("AI tool orchestration plan built, tenantId={}, userId={}, runId={}, planId={}, planMode={}, generatedBy={}, maxSteps={}, validationResult={}",
                run.getTenantId(), run.getUserId(), run.getRunId(), plan.getPlanId(), plan.getMode(),
                plan.getGeneratedBy(), plan.getMaxSteps(), result.reason());
        return plan;
    }

    private ToolOrchestrationPlan buildPlan(ToolOrchestrationRun run,
                                            ToolCallingPlan toolPlan,
                                            ToolOrchestrationPlanMode mode,
                                            int maxSteps) {
        List<ToolOrchestrationStep> steps = new ArrayList<>();
        ToolOrchestrationStep firstStep = firstStep(run, toolPlan);
        steps.add(firstStep);
        if (mode != ToolOrchestrationPlanMode.SINGLE_STEP && maxSteps > 1) {
            steps.add(followUpSkippedStep(run, firstStep, mode));
        }
        return ToolOrchestrationPlan.builder()
                .planId(run.getRunId() + "-plan-1")
                .runId(run.getRunId())
                .mode(mode)
                .objective(trimObjective(run.getUserMessage()))
                .steps(steps)
                .maxSteps(maxSteps)
                .generatedBy(generatedBy(mode))
                .createdAt(Instant.now())
                .build();
    }

    private ToolOrchestrationStep firstStep(ToolOrchestrationRun run, ToolCallingPlan toolPlan) {
        return ToolOrchestrationStep.builder()
                .stepId(stepId(run.getRunId(), 1))
                .stepRef(stepRefBuilder.stepRef(1))
                .stepNo(1)
                .toolName(toolPlan.selectedTool())
                .arguments(copyArguments(toolPlan.toolArguments()))
                .reason(toolPlan.reason())
                .dependsOnStepIds(List.of())
                .inputRefs(List.of())
                .outputRef(stepRefBuilder.outputRef(0))
                .inputSummary("")
                .status(ToolOrchestrationStepStatus.RUNNING)
                .startedAt(Instant.now())
                .build();
    }

    private ToolOrchestrationStep followUpSkippedStep(ToolOrchestrationRun run,
                                                      ToolOrchestrationStep firstStep,
                                                      ToolOrchestrationPlanMode mode) {
        String skipReason = mode == ToolOrchestrationPlanMode.MULTI_STEP_DRY_RUN
                ? "multi-step dry-run only; real Tool is not executed"
                : "multi-step controlled plan only; follow-up Tool execution is disabled in Phase 4.14";
        return ToolOrchestrationStep.builder()
                .stepId(stepId(run.getRunId(), 2))
                .stepRef(stepRefBuilder.stepRef(2))
                .stepNo(2)
                .toolName(PLACEHOLDER_TOOL)
                .arguments(Map.of())
                .reason("Phase 4.14 planned follow-up placeholder")
                .dependsOnStepIds(List.of(firstStep.getStepId()))
                .inputRefs(List.of(stepRefBuilder.outputSummaryInputRef(1)))
                .outputRef(stepRefBuilder.outputRef(1))
                .inputSummary("inputRefs=[" + stepRefBuilder.outputSummaryInputRef(1) + "]")
                .status(ToolOrchestrationStepStatus.SKIPPED)
                .skipReason(skipReason)
                .finishedAt(Instant.now())
                .build();
    }

    private ToolOrchestrationPlanMode resolvePlanMode(ToolOrchestrationRun run,
                                                      AiAgentProperties.OrchestratorProperties orchestrator,
                                                      ToolCallingPlan toolPlan) {
        if (run.getRequestedTool() != null && !run.getRequestedTool().isBlank()) {
            return ToolOrchestrationPlanMode.SINGLE_STEP;
        }
        if (!hasEnoughReadOnlyCandidates(toolPlan)) {
            return ToolOrchestrationPlanMode.SINGLE_STEP;
        }
        if (orchestrator.isMultiStepEnabled()
                && orchestrator.isDryRunEnabled()
                && orchestrator.getPlanMode() == ToolOrchestrationPlanMode.MULTI_STEP_DRY_RUN) {
            return ToolOrchestrationPlanMode.MULTI_STEP_DRY_RUN;
        }
        if (orchestrator.isMultiStepEnabled()
                && orchestrator.getPlanMode() == ToolOrchestrationPlanMode.MULTI_STEP_CONTROLLED) {
            return ToolOrchestrationPlanMode.MULTI_STEP_CONTROLLED;
        }
        return ToolOrchestrationPlanMode.SINGLE_STEP;
    }

    private boolean hasEnoughReadOnlyCandidates(ToolCallingPlan toolPlan) {
        boolean selectedToolReadOnly = toolRegistry.findDefinition(toolPlan.selectedTool())
                .map(definition -> definition.isReadOnly())
                .orElse(false);
        long readOnlyCount = toolRegistry.listDefinitions().stream()
                .filter(definition -> definition.isReadOnly())
                .count();
        return selectedToolReadOnly && readOnlyCount > 1;
    }

    private String generatedBy(ToolOrchestrationPlanMode mode) {
        return switch (mode) {
            case SINGLE_STEP -> "orchestration-planner-single-step";
            case MULTI_STEP_DRY_RUN -> "orchestration-planner-dry-run";
            case MULTI_STEP_CONTROLLED -> "orchestration-planner-controlled";
        };
    }

    private Map<String, Object> copyArguments(Map<String, Object> arguments) {
        return arguments == null ? Map.of() : Map.copyOf(arguments);
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
}
