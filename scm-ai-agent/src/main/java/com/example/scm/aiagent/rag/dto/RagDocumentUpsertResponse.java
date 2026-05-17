package com.example.scm.aiagent.rag.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * RAG 文档写入响应。
 */
@Getter
@Setter
public class RagDocumentUpsertResponse {

    /** 当前租户 ID。 */
    private Long tenantId;

    /** 写入的知识库 ID。 */
    private String knowledgeBaseId;

    /** 写入的文档 ID。 */
    private String documentId;

    /** 本次生成的切片数量。 */
    private int chunkCount;

    /** 当前使用的向量存储模式，例如 in-memory 或 milvus。 */
    private String vectorStoreMode;

    /** 当前使用的 embedding 模式，例如 mock 或 spring-ai。 */
    private String embeddingMode;

    /** 写入完成时间。 */
    private Instant createdAt;
}
