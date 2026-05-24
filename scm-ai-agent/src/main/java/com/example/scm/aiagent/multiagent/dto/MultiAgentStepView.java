package com.example.scm.aiagent.multiagent.dto;

import com.example.scm.aiagent.multiagent.model.MultiAgentActionType;
import com.example.scm.aiagent.multiagent.model.MultiAgentRole;
import com.example.scm.aiagent.multiagent.model.MultiAgentStepStatus;
import lombok.Builder;
import lombok.Getter;

/** Multi-Agent step 的脱敏返回视图。 */
@Getter
@Builder
public class MultiAgentStepView {

    private int stepNo;
    private String agentName;
    private MultiAgentRole agentRole;
    private MultiAgentActionType actionType;
    private MultiAgentStepStatus status;
    private String inputSummary;
    private String outputSummary;
    private String errorCode;
    private String errorMessage;
    private long latencyMs;
}
