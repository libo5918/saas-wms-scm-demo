package com.example.scm.aiagent.workflow.engine;

import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.workflow.dto.AgentWorkflowRunRequest;
import com.example.scm.aiagent.workflow.model.AgentWorkflowDefinition;
import com.example.scm.aiagent.workflow.model.AgentWorkflowRun;
import com.example.scm.aiagent.workflow.model.AgentWorkflowStep;
import com.example.scm.aiagent.workflow.model.AgentWorkflowStepStatus;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Workflow 执行上下文。
 *
 * <p>仅保存步骤安全摘要和必要中间变量，禁止放入完整 rawData、prompt、模型响应或敏感凭证。</p>
 */
@Getter
public class AgentWorkflowExecutionContext {

    private final AgentWorkflowDefinition definition;
    private final AgentWorkflowRunRequest request;
    private final AgentRequestContext agentRequestContext;
    private final AgentWorkflowRun run;
    private final Map<String, AgentWorkflowStep> completedSteps = new LinkedHashMap<>();
    private final Map<String, Map<String, Object>> stepOutputs = new LinkedHashMap<>();
    private Map<String, Object> ragSummary = Map.of();
    private String finalAnswer;

    public AgentWorkflowExecutionContext(AgentWorkflowDefinition definition,
                                         AgentWorkflowRunRequest request,
                                         AgentRequestContext agentRequestContext,
                                         AgentWorkflowRun run) {
        this.definition = definition;
        this.request = request;
        this.agentRequestContext = agentRequestContext;
        this.run = run;
    }

    /** 记录步骤执行结果，供后续步骤读取安全摘要。 */
    public void completeStep(AgentWorkflowStep step) {
        completedSteps.put(step.getStepCode(), step);
    }

    public Optional<AgentWorkflowStep> getCompletedStep(String stepCode) {
        return Optional.ofNullable(completedSteps.get(stepCode));
    }

    public boolean isStepSuccess(String stepCode) {
        return getCompletedStep(stepCode)
                .map(step -> step.getStatus() == AgentWorkflowStepStatus.SUCCESS)
                .orElse(false);
    }

    public void putStepOutput(String stepCode, Map<String, Object> safeFields) {
        stepOutputs.put(stepCode, safeFields == null ? Map.of() : safeFields);
    }

    public Map<String, Object> getStepOutput(String stepCode) {
        return stepOutputs.getOrDefault(stepCode, Map.of());
    }

    public void setRagSummary(Map<String, Object> ragSummary) {
        this.ragSummary = ragSummary == null ? Map.of() : ragSummary;
    }

    public void setFinalAnswer(String finalAnswer) {
        this.finalAnswer = finalAnswer;
        run.setFinalAnswer(finalAnswer);
    }
}
