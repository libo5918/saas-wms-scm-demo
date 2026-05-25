package com.example.scm.aiagent.multiagent.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** 会话级 Memory 条目，禁止保存 prompt/rawData/token 等敏感原文。 */
@Getter
@Setter
@Builder
public class MultiAgentMemoryEntry {

    private String memoryId;
    private String conversationId;
    private String runId;
    private Long tenantId;
    private Long userId;
    private MultiAgentMemoryType type;
    private String contentSummary;
    @Builder.Default
    private Map<String, Object> structuredData = new HashMap<>();
    private Instant createdAt;
}
