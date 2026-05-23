package com.example.scm.aiagent.workflow.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** Workflow 定义概要视图。 */
@Getter
@Builder
public class AgentWorkflowDefinitionView {

    private String workflowCode;
    private String workflowName;
    private String description;
    private String version;
    private boolean enabled;
    private List<AgentWorkflowStepDefinitionView> steps;
}
