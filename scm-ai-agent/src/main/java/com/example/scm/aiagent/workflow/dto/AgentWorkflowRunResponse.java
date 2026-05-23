package com.example.scm.aiagent.workflow.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** Workflow 运行响应。 */
@Getter
@Builder
public class AgentWorkflowRunResponse {

    private String runId;
    private String workflowCode;
    private String workflowName;
    private String status;
    private List<AgentWorkflowStepView> steps;
    private String finalAnswer;
    private long latencyMs;
}
