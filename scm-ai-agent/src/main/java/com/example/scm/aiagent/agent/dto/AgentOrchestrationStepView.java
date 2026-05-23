package com.example.scm.aiagent.agent.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/** Orchestration step 的安全概要视图。 */
@Getter
@Builder
public class AgentOrchestrationStepView {

    private int stepNo;
    private String stepRef;
    private String toolName;
    private String status;
    private Boolean executed;
    private Boolean inputResolved;
    private String skipReason;
    private String displayTitle;
    private String displaySummary;
    private Map<String, Object> safeFields;
}
