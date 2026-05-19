package com.example.scm.aiagent.rag.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * RAG 文档删除响应。
 */
@Getter
@Builder
public class RagDocumentDeleteResponse {

    /** 当前租户 ID。 */
    private Long tenantId;

    /** 当前知识库 ID。 */
    private String knowledgeBaseId;

    /** 被删除的文档 ID。 */
    private String documentId;

    /** 是否删除了 Document Registry 记录。 */
    private boolean registryDeleted;

    /** 从 VectorStore 删除的 chunk 数量。 */
    private long deletedChunkCount;
}
