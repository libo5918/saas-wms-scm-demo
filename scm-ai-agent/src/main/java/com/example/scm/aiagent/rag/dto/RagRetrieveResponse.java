package com.example.scm.aiagent.rag.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 检索响应。
 */
@Getter
@Setter
public class RagRetrieveResponse {

    /** 当前租户 ID。 */
    private Long tenantId;

    /** 本次检索的知识库 ID。 */
    private String knowledgeBaseId;

    /** 本次检索返回的切片数量。 */
    private int retrievedCount;

    /** 检索耗时，单位毫秒。 */
    private long latencyMs;

    /** 检索命中的切片列表。 */
    private List<RagRetrievedChunk> chunks = new ArrayList<>();
}
