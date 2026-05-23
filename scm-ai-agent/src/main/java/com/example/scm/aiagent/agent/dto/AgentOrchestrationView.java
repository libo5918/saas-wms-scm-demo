package com.example.scm.aiagent.agent.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** Orchestrator run 的脱敏概要。 */
@Getter
@Builder
public class AgentOrchestrationView {

    private boolean enabled;
    private String runId;
    private String planMode;
    private int stepCount;
    private List<AgentOrchestrationStepView> steps;
}
