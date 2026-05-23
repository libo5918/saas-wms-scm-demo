package com.example.scm.aiagent.workflow.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/** Workflow 运行记录。 */
@Getter
@Setter
@Builder
public class AgentWorkflowRun {

    private String runId;
    private String workflowCode;
    private String workflowName;
    private Long tenantId;
    private Long userId;
    private String userMessage;
    private AgentWorkflowRunStatus status;
    private List<AgentWorkflowStep> steps;
    private String finalAnswer;
    private Instant startedAt;
    private Instant finishedAt;
    private long latencyMs;
}
