package com.example.scm.aiagent.agent.dto;

import lombok.Builder;
import lombok.Getter;

/** 单个 RAG chunk 的短摘要视图。 */
@Getter
@Builder
public class AgentRagChunkView {

    private String documentId;
    private String chunkId;
    private String title;
    private String source;
    private String contentSnippet;
    private double score;
}
