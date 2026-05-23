package com.example.scm.aiagent.workflow.service;

import com.example.scm.aiagent.workflow.dto.AgentWorkflowDefinitionView;
import com.example.scm.aiagent.workflow.dto.AgentWorkflowRunResponse;
import com.example.scm.aiagent.workflow.dto.AgentWorkflowStepDefinitionView;
import com.example.scm.aiagent.workflow.dto.AgentWorkflowStepView;
import com.example.scm.aiagent.workflow.model.AgentWorkflowDefinition;
import com.example.scm.aiagent.workflow.model.AgentWorkflowRun;
import com.example.scm.aiagent.workflow.model.AgentWorkflowStep;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** Workflow 对外视图转换器，统一保证脱敏输出。 */
@Component
public class AgentWorkflowViewMapper {

    public AgentWorkflowDefinitionView toDefinitionView(AgentWorkflowDefinition definition) {
        return AgentWorkflowDefinitionView.builder()
                .workflowCode(definition.getWorkflowCode())
                .workflowName(definition.getWorkflowName())
                .description(definition.getDescription())
                .version(definition.getVersion())
                .enabled(definition.isEnabled())
                .steps(definition.getSteps() == null ? List.of() : definition.getSteps().stream()
                        .map(step -> AgentWorkflowStepDefinitionView.builder()
                                .stepCode(step.getStepCode())
                                .stepName(step.getStepName())
                                .stepNo(step.getStepNo())
                                .stepType(step.getStepType() == null ? null : step.getStepType().name())
                                .toolName(step.getToolName())
                                .description(step.getDescription())
                                .build())
                        .toList())
                .build();
    }

    public AgentWorkflowRunResponse toRunResponse(AgentWorkflowRun run) {
        return AgentWorkflowRunResponse.builder()
                .runId(run.getRunId())
                .workflowCode(run.getWorkflowCode())
                .workflowName(run.getWorkflowName())
                .status(run.getStatus() == null ? null : run.getStatus().name())
                .steps(run.getSteps() == null ? List.of() : run.getSteps().stream().map(this::toStepView).toList())
                .finalAnswer(run.getFinalAnswer())
                .latencyMs(run.getLatencyMs())
                .build();
    }

    private AgentWorkflowStepView toStepView(AgentWorkflowStep step) {
        return AgentWorkflowStepView.builder()
                .stepCode(step.getStepCode())
                .stepName(step.getStepName())
                .stepNo(step.getStepNo())
                .stepType(step.getStepType() == null ? null : step.getStepType().name())
                .status(step.getStatus() == null ? null : step.getStatus().name())
                .toolName(step.getToolName())
                .inputResolved(step.isInputResolved())
                .skipReason(step.getSkipReason())
                .errorCode(step.getErrorCode())
                .errorMessage(step.getErrorMessage())
                .displayTitle(step.getDisplayTitle())
                .displaySummary(step.getDisplaySummary())
                .safeFields(step.getSafeFields() == null ? Map.of() : step.getSafeFields())
                .latencyMs(step.getLatencyMs())
                .build();
    }
}
