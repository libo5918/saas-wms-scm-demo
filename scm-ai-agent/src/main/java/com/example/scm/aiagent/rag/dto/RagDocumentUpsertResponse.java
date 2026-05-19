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

    /** 本次生成并写入的 chunk 数量。 */
    private int chunkCount;

    /** 本次写入前删除的旧 chunk 数量。 */
    private long deletedCount;

    /** 当前使用的向量存储模式，例如 in-memory 或 milvus。 */
    private String vectorStoreMode;

    /** 当前使用的 embedding 模式，例如 mock、dashscope。 */
    private String embeddingMode;

    /** 当前使用的 embedding 模型名称。 */
    private String embeddingModel;

    /** 导入批次 ID，手动写入时可以为空。 */
    private String importBatchId;

    /** 文档首次导入时间。 */
    private Instant createdAt;

    /** 文档最近更新时间。 */
    private Instant updatedAt;
}
