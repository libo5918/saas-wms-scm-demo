package com.example.scm.aiagent.workflow.executor;

import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.workflow.engine.AgentWorkflowExecutionContext;
import com.example.scm.aiagent.workflow.model.AgentWorkflowRun;
import com.example.scm.aiagent.workflow.model.AgentWorkflowStep;
import com.example.scm.aiagent.workflow.model.AgentWorkflowStepDefinition;
import com.example.scm.aiagent.workflow.model.AgentWorkflowStepStatus;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;

/** 步骤执行器公共模板，统一处理状态、耗时和安全日志。 */
@Slf4j
abstract class AbstractWorkflowStepExecutor implements AgentWorkflowStepExecutor {

    protected AgentWorkflowStep newStep(AgentWorkflowStepDefinition definition) {
        return AgentWorkflowStep.builder()
                .stepCode(definition.getStepCode())
                .stepName(definition.getStepName())
                .stepNo(definition.getStepNo())
                .stepType(definition.getStepType())
                .toolName(definition.getToolName())
                .status(AgentWorkflowStepStatus.PENDING)
                .safeFields(Map.of())
                .build();
    }

    protected long beginStep(AgentWorkflowExecutionContext context, AgentWorkflowStep step) {
        long startedAt = System.nanoTime();
        AgentWorkflowRun run = context.getRun();
        AgentRequestContext requestContext = context.getAgentRequestContext();
        step.setStatus(AgentWorkflowStepStatus.RUNNING);
        step.setStartedAt(Instant.now());
        run.getSteps().add(step);
        log.info("AI workflow step started, tenantId={}, userId={}, runId={}, workflowCode={}, workflowName={}, stepCode={}, stepNo={}, stepType={}, executorName={}, toolName={}, status={}",
                requestContext.tenantId(), requestContext.userId(), run.getRunId(), run.getWorkflowCode(), run.getWorkflowName(),
                step.getStepCode(), step.getStepNo(), step.getStepType(), getClass().getSimpleName(), step.getToolName(), step.getStatus());
        return startedAt;
    }

    protected void skipStep(AgentWorkflowExecutionContext context, AgentWorkflowStep step, String reason, long startedAt) {
        step.setStatus(AgentWorkflowStepStatus.SKIPPED);
        step.setInputResolved(false);
        step.setSkipReason(reason);
        step.setFinishedAt(Instant.now());
        step.setLatencyMs(elapsedMs(startedAt));
        context.completeStep(step);
        logStepFinished(context, step);
    }

    protected void finishStep(AgentWorkflowExecutionContext context, AgentWorkflowStep step, long startedAt) {
        step.setFinishedAt(Instant.now());
        step.setLatencyMs(elapsedMs(startedAt));
        context.completeStep(step);
        if (step.getSafeFields() != null && step.getStatus() == AgentWorkflowStepStatus.SUCCESS) {
            context.putStepOutput(step.getStepCode(), step.getSafeFields());
        }
        logStepFinished(context, step);
    }

    protected void logStepFinished(AgentWorkflowExecutionContext context, AgentWorkflowStep step) {
        AgentWorkflowRun run = context.getRun();
        AgentRequestContext requestContext = context.getAgentRequestContext();
        log.info("AI workflow step finished, tenantId={}, userId={}, runId={}, workflowCode={}, workflowName={}, stepCode={}, stepNo={}, stepType={}, executorName={}, toolName={}, status={}, success={}, errorCode={}, latencyMs={}",
                requestContext.tenantId(), requestContext.userId(), run.getRunId(), run.getWorkflowCode(), run.getWorkflowName(),
                step.getStepCode(), step.getStepNo(), step.getStepType(), getClass().getSimpleName(), step.getToolName(),
                step.getStatus(), step.getStatus() == AgentWorkflowStepStatus.SUCCESS, step.getErrorCode(), step.getLatencyMs());
    }

    protected long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
