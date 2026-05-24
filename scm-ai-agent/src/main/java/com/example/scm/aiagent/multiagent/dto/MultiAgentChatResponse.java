package com.example.scm.aiagent.multiagent.dto;

import com.example.scm.aiagent.multiagent.model.MultiAgentRunStatus;
import com.example.scm.aiagent.multiagent.model.MultiAgentIntentType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/** Multi-Agent Chat 和 Run Status 共用的脱敏响应。 */
@Getter
@Builder
public class MultiAgentChatResponse {

    private String runId;
    private MultiAgentRunStatus status;
    private MultiAgentIntentType intentType;
    private String answer;
    private Map<String, Object> planSummary;
    private Map<String, Object> rag;
    private Map<String, Object> tool;
    private Map<String, Object> review;
    private List<MultiAgentAgentView> agents;
    private List<MultiAgentStepView> steps;
    private List<MultiAgentMessageView> messages;
    private long latencyMs;
}
