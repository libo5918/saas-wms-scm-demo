package com.example.scm.aiagent.multiagent.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
    private String finalAnswer;
    private boolean success;
    private Instant createdAt;
    private Instant finishedAt;
    private long latencyMs;
}
