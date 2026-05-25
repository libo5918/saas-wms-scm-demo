package com.example.scm.aiagent.multiagent.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 一次 Multi-Agent 协作运行的脱敏状态聚合。 */
@Getter
@Setter
@Builder
public class MultiAgentRun {

    private String runId;
    private Long tenantId;
    private Long userId;
    private String userMessage;
    private MultiAgentRunStatus status;
    @Builder.Default
    private List<MultiAgentAgentState> agents = new ArrayList<>();
    @Builder.Default
    private List<MultiAgentStep> steps = new ArrayList<>();
    @Builder.Default
    private List<MultiAgentMessage> messages = new ArrayList<>();
    private MultiAgentIntentType intentType;
    @Builder.Default
    private Map<String, Object> planSummary = new HashMap<>();
    @Builder.Default
    private Map<String, Object> rag = new HashMap<>();
    @Builder.Default
    private Map<String, Object> tool = new HashMap<>();
    @Builder.Default
    private Map<String, Object> review = new HashMap<>();
    @Builder.Default
    private Map<String, Object> constraints = new HashMap<>();
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
    @Builder.Default
    private Map<String, Object> reviewAfterRepair = new HashMap<>();
    private String finalAnswer;
    private boolean success;
    private Instant createdAt;
    private Instant finishedAt;
    private long latencyMs;
}
