package com.example.scm.aiagent.multiagent.model;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/** Multi-Agent run 维度的轻量观测指标，只包含安全统计信息。 */
@Getter
@Builder
public class MultiAgentRunMetrics {

    private long totalLatencyMs;
    private int stepCount;
    private int agentCount;
    private boolean ragCalled;
    private long ragRetrievedCount;
    private boolean toolCalled;
    private int toolCallCount;
    private boolean reviewEnabled;
    private boolean reviewPassed;
    private boolean repairEnabled;
    private boolean repairAttempted;
    private int repairCount;
    private boolean memoryEnabled;
    private int memoryReadCount;
    private int memoryWriteCount;
    private boolean terminated;
    private String terminatedReason;
    @Builder.Default
    private List<MultiAgentAgentMetrics> agentMetrics = new ArrayList<>();
}
