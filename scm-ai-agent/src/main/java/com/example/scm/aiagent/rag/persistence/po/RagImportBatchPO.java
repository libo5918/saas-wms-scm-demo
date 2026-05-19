package com.example.scm.aiagent.rag.persistence.po;

import lombok.Data;

import java.time.Instant;

/**
 * RAG docs 导入批次持久化对象，对应表 `rag_import_batch`。
 */
@Data
public class RagImportBatchPO {

    /** 租户 ID。 */
    private Long tenantId;

    /** 导入批次 ID。 */
    private String importBatchId;

    /** 触发导入的用户 ID。 */
    private Long userId;

    /** 知识库 ID。 */
    private String knowledgeBaseId;

    /** 扫描根目录。 */
    private String scanRoot;

    /** 扫描命中文件数量。 */
    private Integer fileCount;

    /** 成功导入数量。 */
    private Integer importedCount;

    /** 跳过数量。 */
    private Integer skippedCount;

    /** 向量存储模式。 */
    private String vectorStoreMode;

    /** Embedding 模式。 */
    private String embeddingMode;

    /** Embedding 模型。 */
    private String embeddingModel;

    /** 导入开始时间。 */
    private Instant startedAt;

    /** 导入完成时间。 */
    private Instant finishedAt;

    /** 导入耗时毫秒。 */
    private Long latencyMs;
}
