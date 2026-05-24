package com.example.scm.aiagent.multiagent.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Multi-Agent 运行过程中的一个脱敏步骤记录。 */
@Getter
@Setter
@Builder
public class MultiAgentStep {

    private String stepId;
    private int stepNo;
    private String agentName;
    private MultiAgentRole agentRole;
    private MultiAgentActionType actionType;
    private MultiAgentStepStatus status;
    private String inputSummary;
    private String outputSummary;
    private String errorCode;
    private String errorMessage;
    private Instant startedAt;
    private Instant finishedAt;
    private long latencyMs;
}
