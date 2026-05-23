package com.example.scm.aiagent.workflow.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** Workflow 定义，用于表达固定业务流程。 */
@Getter
@Builder
public class AgentWorkflowDefinition {

    private String workflowCode;
    private String workflowName;
    private String description;
    private String version;
    private boolean enabled;
    private List<AgentWorkflowStepDefinition> steps;
}
