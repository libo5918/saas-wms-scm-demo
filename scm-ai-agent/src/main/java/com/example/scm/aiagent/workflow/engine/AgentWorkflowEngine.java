package com.example.scm.aiagent.workflow.engine;

import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.workflow.dto.AgentWorkflowRunRequest;
import com.example.scm.aiagent.workflow.executor.AgentWorkflowStepExecutorRegistry;
import com.example.scm.aiagent.workflow.model.AgentWorkflowDefinition;
import com.example.scm.aiagent.workflow.model.AgentWorkflowRun;
import com.example.scm.aiagent.workflow.model.AgentWorkflowRunStatus;
import com.example.scm.aiagent.workflow.model.AgentWorkflowStep;
import com.example.scm.aiagent.workflow.model.AgentWorkflowStepDefinition;
import com.example.scm.aiagent.workflow.model.AgentWorkflowStepStatus;
import com.example.scm.aiagent.workflow.service.AgentWorkflowRunStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;

/**
 * Workflow 最小执行引擎。
 *
 * <p>负责按 definition.steps 顺序调度步骤执行器，不承担具体 Tool 或 Summary 业务逻辑。</p>
 */
@Slf4j
@Service
public class AgentWorkflowEngine {

    private final AgentWorkflowRunStore runStore;
    private final AgentWorkflowStepExecutorRegistry executorRegistry;

    public AgentWorkflowEngine(AgentWorkflowRunStore runStore,
                               AgentWorkflowStepExecutorRegistry executorRegistry) {
        this.runStore = runStore;
        this.executorRegistry = executorRegistry;
    }

    public AgentWorkflowRun execute(AgentWorkflowDefinition definition,
                                    AgentWorkflowRunRequest request,
                                    AgentRequestContext context) {
        long startedAt = System.nanoTime();
        String runId = StringUtils.hasText(request.getRunId()) ? request.getRunId() : UUID.randomUUID().toString();
        AgentWorkflowRun run = AgentWorkflowRun.builder()
                .runId(runId)
                .workflowCode(definition.getWorkflowCode())
                .workflowName(definition.getWorkflowName())
                .tenantId(context.tenantId())
                .userId(context.userId())
                .userMessage(request.getMessage())
                .status(AgentWorkflowRunStatus.RUNNING)
                .steps(new ArrayList<>())
                .startedAt(Instant.now())
                .build();
        runStore.save(run);

        log.info("AI workflow run started, tenantId={}, userId={}, runId={}, workflowCode={}, workflowName={}",
                context.tenantId(), context.userId(), runId, definition.getWorkflowCode(), definition.getWorkflowName());

        AgentWorkflowExecutionContext executionContext =
                new AgentWorkflowExecutionContext(definition, request, context, run);
        definition.getSteps().stream()
                .sorted(Comparator.comparingInt(AgentWorkflowStepDefinition::getStepNo))
                .forEach(stepDefinition -> executorRegistry.find(stepDefinition).execute(executionContext, stepDefinition));

        boolean success = run.getSteps().stream().allMatch(step -> step.getStatus() == AgentWorkflowStepStatus.SUCCESS);
        run.setStatus(success ? AgentWorkflowRunStatus.SUCCESS : AgentWorkflowRunStatus.FAILED);
        run.setFinishedAt(Instant.now());
        run.setLatencyMs(elapsedMs(startedAt));
        runStore.save(run);
        log.info("AI workflow run finished, tenantId={}, userId={}, runId={}, workflowCode={}, status={}, latencyMs={}",
                context.tenantId(), context.userId(), runId, definition.getWorkflowCode(), run.getStatus(), run.getLatencyMs());
        return run;
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
