package com.example.scm.aiagent.toolcalling.orchestrator;

import org.springframework.stereotype.Component;

/**
 * 构造步骤间传递的安全上下文摘要。
 *
 * <p>摘要只使用 execution 概要字段，不读取完整 rawData、prompt、模型响应或请求头。</p>
 */
@Component
public class ToolOrchestrationStepSummaryBuilder {

    /**
     * 根据脱敏执行概要生成输出摘要。
     */
    public String buildOutputSummary(ToolOrchestrationExecutionSummary execution) {
        if (execution == null) {
            return "";
        }
        StringBuilder summary = new StringBuilder();
        summary.append("tool=").append(nullToEmpty(execution.getToolName()));
        summary.append(", success=").append(execution.isSuccess());
        if (hasText(execution.getErrorCode())) {
            summary.append(", errorCode=").append(execution.getErrorCode());
        }
        if (hasText(execution.getErrorMessage())) {
            summary.append(", errorMessage=").append(execution.getErrorMessage());
        }
        if (hasText(execution.getDisplayTitle())) {
            summary.append(", displayTitle=").append(execution.getDisplayTitle());
        }
        if (hasText(execution.getDisplaySummary())) {
            summary.append(", displaySummary=").append(execution.getDisplaySummary());
        }
        summary.append(", latencyMs=").append(execution.getLatencyMs());
        return summary.toString();
    }

    /**
     * 后续步骤读取前置步骤的安全输出摘要作为输入摘要。
     */
    public String buildInputSummary(ToolOrchestrationStep previousStep) {
        if (previousStep == null || !hasText(previousStep.getOutputSummary())) {
            return "";
        }
        String inputRefs = previousStep.getStepRef() == null ? "" : previousStep.getStepRef() + ".outputSummary";
        return "inputRefs=[" + inputRefs + "], previousStep=" + previousStep.getStepRef()
                + ", outputSummary=" + previousStep.getOutputSummary();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
