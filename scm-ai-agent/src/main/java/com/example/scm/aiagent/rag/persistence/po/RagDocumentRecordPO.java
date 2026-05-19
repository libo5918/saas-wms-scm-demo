package com.example.scm.aiagent.rag.persistence.po;

import lombok.Data;

import java.time.Instant;

/**
 * RAG 文档治理元数据持久化对象，对应表 `rag_document_registry`。
 */
@Data
public class RagDocumentRecordPO {

    /** 租户 ID。 */
    private Long tenantId;

    /** 知识库 ID。 */
    private String knowledgeBaseId;

    /** 文档 ID。 */
    private String documentId;

    /** 文档标题。 */
    private String title;

    /** 文档来源。 */
    private String source;

    /** 文件路径。 */
    private String filePath;

    /** 文件名。 */
    private String fileName;

    /** 所在目录。 */
    private String directory;

    /** 导入来源。 */
    private String importSource;

    /** 当前文档 chunk 数量。 */
    private Integer chunkCount;

    /** 最近一次写入前删除的旧 chunk 数量。 */
    private Long deletedCount;

    /** Embedding 模式。 */
    private String embeddingMode;

    /** Embedding 模型。 */
    private String embeddingModel;

    /** 向量存储模式。 */
    private String vectorStoreMode;

    /** 最近一次导入批次 ID。 */
    private String importBatchId;

    /** 扩展元数据 JSON。 */
    private String metadataJson;

    /** 首次导入时间。 */
    private Instant importedAt;

    /** 最近更新时间。 */
    private Instant updatedAt;

    /** 是否已逻辑删除，0 表示有效，1 表示已删除。 */
    private Integer deleted;

    /** 逻辑删除时间。 */
    private Instant deletedAt;

    /** 执行逻辑删除的用户 ID，当前接口未透传时可为空。 */
    private Long deletedBy;
}
