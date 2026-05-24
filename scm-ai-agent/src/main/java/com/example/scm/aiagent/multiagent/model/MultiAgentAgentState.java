package com.example.scm.aiagent.multiagent.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** Run 内某个 Agent 的安全状态摘要。 */
@Getter
@Setter
@Builder
public class MultiAgentAgentState {

    private String agentName;
    private MultiAgentRole role;
    private MultiAgentStepStatus status;
    private String summary;
}
