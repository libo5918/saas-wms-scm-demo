package com.example.scm.aiagent.agent.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** RAG 检索结果的脱敏展示视图。 */
@Getter
@Builder
public class AgentRagView {

    private String knowledgeBaseId;
    private int retrievedCount;
    private long latencyMs;
    private List<AgentRagChunkView> chunks;
}
