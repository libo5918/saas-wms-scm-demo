package com.example.scm.aiagent.toolcalling.orchestrator;

import com.example.scm.aiagent.tool.model.ToolDefinition;
import com.example.scm.aiagent.tool.service.ToolRegistry;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Orchestration plan 安全校验器。
 */
@Component
public class ToolOrchestrationPlanValidator {

    private static final String DRY_RUN_PLACEHOLDER_TOOL = "orchestrator.futureStep";
    private static final String[] SENSITIVE_KEYWORDS = {
            "rawdata", "prompt", "token", "authorization", "cookie", "api-key", "apikey", "secret"
    };

    private final ToolRegistry toolRegistry;

    public ToolOrchestrationPlanValidator(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /**
     * 校验 plan 是否满足只读、脱敏、引用顺序和步骤数量约束。
     */
    public ToolOrchestrationPlanValidationResult validate(ToolOrchestrationPlan plan, int maxSteps) {
        if (plan == null) {
            return ToolOrchestrationPlanValidationResult.invalid("plan is null");
        }
        if (plan.getSteps() == null || plan.getSteps().isEmpty()) {
            return ToolOrchestrationPlanValidationResult.invalid("plan has no steps");
        }
        if (plan.getSteps().size() > Math.max(1, maxSteps)) {
            return ToolOrchestrationPlanValidationResult.invalid("steps exceed maxSteps");
        }
        for (ToolOrchestrationStep step : plan.getSteps()) {
            ToolOrchestrationPlanValidationResult result = validateStep(plan, step);
            if (!result.valid()) {
                return result;
            }
        }
        return ToolOrchestrationPlanValidationResult.ok();
    }

    private ToolOrchestrationPlanValidationResult validateStep(ToolOrchestrationPlan plan, ToolOrchestrationStep step) {
        if (step == null) {
            return ToolOrchestrationPlanValidationResult.invalid("step is null");
        }
        if (plan.getMode() == ToolOrchestrationPlanMode.MULTI_STEP_DRY_RUN
                && step.getStepNo() > 1
                && step.getStatus() != ToolOrchestrationStepStatus.SKIPPED) {
            return ToolOrchestrationPlanValidationResult.invalid("dry-run follow-up step must be skipped");
        }
        if (plan.getMode() == ToolOrchestrationPlanMode.MULTI_STEP_CONTROLLED
                && step.getStepNo() > 1
                && step.getStatus() != ToolOrchestrationStepStatus.SKIPPED) {
            return ToolOrchestrationPlanValidationResult.invalid("controlled follow-up step is not executable in Phase 4.14");
        }
        if (!isKnownReadOnlyTool(step)) {
            return ToolOrchestrationPlanValidationResult.invalid("tool is not registered readOnly");
        }
        if (!refsOnlyPointToPreviousSteps(step)) {
            return ToolOrchestrationPlanValidationResult.invalid("stepRef can only point to previous steps");
        }
        if (containsSensitiveKeyword(step.getInputSummary()) || containsSensitiveKeyword(step.getOutputSummary())) {
            return ToolOrchestrationPlanValidationResult.invalid("summary contains sensitive keyword");
        }
        return ToolOrchestrationPlanValidationResult.ok();
    }

    private boolean isKnownReadOnlyTool(ToolOrchestrationStep step) {
        if (DRY_RUN_PLACEHOLDER_TOOL.equals(step.getToolName())) {
            return true;
        }
        return toolRegistry.findDefinition(step.getToolName())
                .map(ToolDefinition::isReadOnly)
                .orElse(false);
    }

    private boolean refsOnlyPointToPreviousSteps(ToolOrchestrationStep step) {
        if (step.getInputRefs() == null) {
            return true;
        }
        for (String inputRef : step.getInputRefs()) {
            int refStepNo = parseStepRef(inputRef);
            if (refStepNo <= 0 || refStepNo >= step.getStepNo()) {
                return false;
            }
        }
        return true;
    }

    private int parseStepRef(String inputRef) {
        if (inputRef == null || !inputRef.startsWith("step-")) {
            return -1;
        }
        int dotIndex = inputRef.indexOf('.');
        String number = dotIndex > 5 ? inputRef.substring(5, dotIndex) : inputRef.substring(5);
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private boolean containsSensitiveKeyword(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (String keyword : SENSITIVE_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
