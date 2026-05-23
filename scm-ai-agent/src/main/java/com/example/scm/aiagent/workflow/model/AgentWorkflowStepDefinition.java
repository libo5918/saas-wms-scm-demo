package com.example.scm.aiagent.workflow.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/** Workflow 步骤定义。 */
@Getter
@Builder
public class AgentWorkflowStepDefinition {

    private String stepCode;
    private String stepName;
    private int stepNo;
    private AgentWorkflowStepType stepType;
    private String toolName;
    private Map<String, String> inputMapping;
    private List<String> dependsOnStepCodes;
    private String condition;
    private String description;
}
