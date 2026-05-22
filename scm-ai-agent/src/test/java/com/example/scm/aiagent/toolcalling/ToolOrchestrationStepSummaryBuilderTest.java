package com.example.scm.aiagent.toolcalling;

import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationExecutionSummary;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationStep;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationStepSummaryBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolOrchestrationStepSummaryBuilderTest {

    private final ToolOrchestrationStepSummaryBuilder builder = new ToolOrchestrationStepSummaryBuilder();

    @Test
    void shouldBuildSafeOutputSummaryWithoutRawDataOrSensitiveFields() {
        ToolOrchestrationExecutionSummary execution = ToolOrchestrationExecutionSummary.builder()
                .success(true)
                .toolName("inventory.getBalance")
                .displayTitle("库存余额")
                .displaySummary("已查询到库存余额")
                .latencyMs(8)
                .build();

        String summary = builder.buildOutputSummary(execution);

        assertTrue(summary.contains("tool=inventory.getBalance"));
        assertTrue(summary.contains("displayTitle=库存余额"));
        assertFalse(summary.contains("rawData"));
        assertFalse(summary.toLowerCase().contains("token"));
        assertFalse(summary.toLowerCase().contains("authorization"));
        assertFalse(summary.toLowerCase().contains("prompt"));
    }

    @Test
    void shouldUsePreviousOutputSummaryAsNextInputSummary() {
        ToolOrchestrationStep previous = ToolOrchestrationStep.builder()
                .stepId("run-1-step-1")
                .outputSummary("tool=inventory.getBalance, success=true")
                .build();

        String summary = builder.buildInputSummary(previous);

        assertTrue(summary.contains("previousStep=run-1-step-1"));
        assertTrue(summary.contains("success=true"));
    }
}
