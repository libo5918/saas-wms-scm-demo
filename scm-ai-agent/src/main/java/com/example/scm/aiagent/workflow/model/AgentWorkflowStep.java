package com.example.scm.aiagent.workflow.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

/** Workflow run 中的单个步骤记录，只保存脱敏概要。 */
@Getter
@Setter
@Builder
public class AgentWorkflowStep {

    private String stepCode;
    private String stepName;
    private int stepNo;
    private AgentWorkflowStepType stepType;
    private AgentWorkflowStepStatus status;
    private String toolName;
    private boolean inputResolved;
    private String skipReason;
    private String errorCode;
    private String errorMessage;
    private String displayTitle;
    private String displaySummary;
    private Map<String, Object> safeFields;
    private Instant startedAt;
    private Instant finishedAt;
    private long latencyMs;
}
