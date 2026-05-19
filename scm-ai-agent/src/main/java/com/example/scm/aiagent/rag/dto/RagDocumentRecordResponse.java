package com.example.scm.aiagent.rag.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * RAG 文档治理元数据响应。
 */
@Getter
@Builder
public class RagDocumentRecordResponse {

    /** 当前文档所属租户 ID。 */
    private Long tenantId;

    /** 当前文档所属知识库 ID。 */
    private String knowledgeBaseId;

    /** 文档唯一 ID。 */
    private String documentId;

    /** 文档标题。 */
    private String title;

    /** 文档来源。 */
    private String source;

    /** 文档文件路径。 */
    private String filePath;

    /** 文档文件名。 */
    private String fileName;

    /** 文档所在目录。 */
    private String directory;

    /** 导入来源。 */
    private String importSource;

    /** 当前文档切片数量。 */
    private int chunkCount;

    /** 最近一次写入前删除的旧 chunk 数量。 */
    private long deletedCount;

    /** 当前 embedding 模式。 */
    private String embeddingMode;

    /** 当前 embedding 模型。 */
    private String embeddingModel;

    /** 当前向量存储模式。 */
    private String vectorStoreMode;

    /** 导入批次 ID。 */
    private String importBatchId;

    /** 首次导入时间。 */
    private Instant importedAt;

    /** 最近更新时间。 */
    private Instant updatedAt;

    /** 扩展元数据。 */
    private Map<String, Object> metadata;
}
