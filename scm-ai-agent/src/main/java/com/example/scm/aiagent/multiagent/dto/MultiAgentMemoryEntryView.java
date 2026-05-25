package com.example.scm.aiagent.multiagent.dto;

import com.example.scm.aiagent.multiagent.model.MultiAgentMemoryType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/** Memory 查询接口返回的脱敏条目视图。 */
@Getter
@Builder
public class MultiAgentMemoryEntryView {

    private String memoryId;
    private String conversationId;
    private String runId;
    private MultiAgentMemoryType type;
    private String contentSummary;
    private Map<String, Object> structuredData;
    private Instant createdAt;
}
