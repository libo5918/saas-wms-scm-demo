package com.example.scm.aiagent.multiagent.dto;

import com.example.scm.aiagent.multiagent.model.MultiAgentRunStatus;
import com.example.scm.aiagent.multiagent.model.MultiAgentIntentType;
import com.example.scm.aiagent.multiagent.model.MultiAgentRunMetrics;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/** Multi-Agent Chat 和 Run Status 共用的脱敏响应。 */
@Getter
@Builder
public class MultiAgentChatResponse {

    private String runId;
    private String conversationId;
    private MultiAgentRunStatus status;
    private MultiAgentIntentType intentType;
    private String answer;
    private Map<String, Object> planSummary;
    private Map<String, Object> rag;
    private Map<String, Object> tool;
    private Map<String, Object> review;
    private Map<String, Object> constraints;
    private int roundCount;
    private int toolCallCount;
    private String terminatedReason;
    private String summaryMode;
    private boolean fallbackUsed;
    private boolean repairEnabled;
    private boolean repairAttempted;
    private int repairCount;
    private String repairMode;
    private boolean repairFallbackUsed;
    private Map<String, Object> reviewAfterRepair;
    private boolean memoryEnabled;
    private int memoryReadCount;
    private int memoryWriteCount;
    private Map<String, Object> memory;
    private MultiAgentRunMetrics metrics;
    private String traceSummary;
    private List<MultiAgentAgentView> agents;
    private List<MultiAgentStepView> steps;
    private List<MultiAgentMessageView> messages;
    private long latencyMs;
}
