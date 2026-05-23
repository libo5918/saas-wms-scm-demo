package com.example.scm.aiagent.workflow.dto;

import lombok.Builder;
import lombok.Getter;

/** Workflow 步骤定义概要视图。 */
@Getter
@Builder
public class AgentWorkflowStepDefinitionView {

    private String stepCode;
    private String stepName;
    private int stepNo;
    private String stepType;
    private String toolName;
    private String description;
}
