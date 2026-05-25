package com.example.scm.aiagent.multiagent.model;

import lombok.Builder;
import lombok.Getter;

/** 单个 Agent 的脱敏观测指标，用于面试演示和问题排查。 */
@Getter
@Builder
public class MultiAgentAgentMetrics {

    private String agentName;
    private MultiAgentRole role;
    private int actionCount;
    private int successCount;
    private int failedCount;
    private int skippedCount;
    private long latencyMs;
}
