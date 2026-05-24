package com.example.scm.aiagent.multiagent.dto;

import com.example.scm.aiagent.multiagent.model.MultiAgentRole;
import com.example.scm.aiagent.multiagent.model.MultiAgentStepStatus;
import lombok.Builder;
import lombok.Getter;

/** 状态接口返回的 Agent 脱敏视图。 */
@Getter
@Builder
public class MultiAgentAgentView {

    private String agentName;
    private MultiAgentRole role;
    private MultiAgentStepStatus status;
    private String summary;
}
