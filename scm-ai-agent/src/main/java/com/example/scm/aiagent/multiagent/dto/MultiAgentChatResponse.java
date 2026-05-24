package com.example.scm.aiagent.multiagent.dto;

import com.example.scm.aiagent.multiagent.model.MultiAgentRunStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** Multi-Agent Chat 和 Run Status 共用的脱敏响应。 */
@Getter
@Builder
public class MultiAgentChatResponse {

    private String runId;
    private MultiAgentRunStatus status;
    private String answer;
    private List<MultiAgentAgentView> agents;
    private List<MultiAgentStepView> steps;
    private List<MultiAgentMessageView> messages;
    private long latencyMs;
}
