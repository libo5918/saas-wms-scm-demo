package com.example.scm.aiagent.agent.prompt;

import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationExecutionSummary;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationRun;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationStep;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestration 步骤摘要 Provider。
 */
@Component
public class OrchestrationPromptContextProvider implements AgentPromptContextProvider {

    @Override
    public List<AgentPromptSection> provide(AgentPromptBuildRequest request) {
        ToolOrchestrationRun run = request.getOrchestrationRun();
        if (run == null || run.getSteps() == null || run.getSteps().isEmpty()) {
            return List.of();
        }
        Map<String, Object> structuredData = new LinkedHashMap<>();
        structuredData.put("runId", run.getRunId());
        structuredData.put("planMode", run.getPlan() == null ? null : String.valueOf(run.getPlan().getMode()));
        structuredData.put("stepCount", run.getSteps().size());
        structuredData.put("steps", run.getSteps().stream().map(this::toStepContext).toList());
        return List.of(AgentPromptSection.builder()
                .type(AgentPromptContextType.ORCHESTRATION_STEPS)
                .source(AgentPromptContextSource.ORCHESTRATION)
                .title("编排步骤摘要")
                .content("以下是 Orchestrator 的脱敏步骤轨迹。")
                .structuredData(structuredData)
                .priority(40)
                .maxLength(3000)
                .included(true)
                .build());
    }

    private Map<String, Object> toStepContext(ToolOrchestrationStep step) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("stepNo", step.getStepNo());
        context.put("stepRef", step.getStepRef());
        context.put("toolName", step.getToolName());
        context.put("status", step.getStatus() == null ? null : step.getStatus().name());
        context.put("executed", step.getExecuted());
        context.put("inputResolved", step.getInputResolved());
        context.put("skipReason", step.getSkipReason());
        context.put("outputSummary", step.getOutputSummary());
        context.put("execution", toExecutionContext(step.getExecution()));
        return context;
    }

    private Map<String, Object> toExecutionContext(ToolOrchestrationExecutionSummary execution) {
        if (execution == null) {
            return Map.of();
        }
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("success", execution.isSuccess());
        context.put("toolName", execution.getToolName());
        context.put("errorCode", execution.getErrorCode());
        context.put("errorMessage", execution.getErrorMessage());
        context.put("displayTitle", execution.getDisplayTitle());
        context.put("displaySummary", execution.getDisplaySummary());
        context.put("safeFields", execution.getSafeFields() == null ? Map.of() : execution.getSafeFields());
        context.put("latencyMs", execution.getLatencyMs());
        return context;
    }
}
