package com.example.scm.aiagent.workflow.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/** Workflow 步骤脱敏展示视图。 */
@Getter
@Builder
public class AgentWorkflowStepView {

    private String stepCode;
    private String stepName;
    private int stepNo;
    private String stepType;
    private String status;
    private String toolName;
    private boolean inputResolved;
    private String skipReason;
    private String errorCode;
    private String errorMessage;
    private String displayTitle;
    private String displaySummary;
    private Map<String, Object> safeFields;
    private long latencyMs;
}
