package com.example.scm.aiagent.toolcalling.orchestrator;

/**
 * Orchestration plan 安全校验结果。
 */
public record ToolOrchestrationPlanValidationResult(boolean valid, String reason) {

    public static ToolOrchestrationPlanValidationResult ok() {
        return new ToolOrchestrationPlanValidationResult(true, "ok");
    }

    public static ToolOrchestrationPlanValidationResult invalid(String reason) {
        return new ToolOrchestrationPlanValidationResult(false, reason);
    }
}
