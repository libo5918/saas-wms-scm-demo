package com.example.scm.aiagent.rag.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * RAG docs 导入批次响应。
 */
@Getter
@Builder
public class RagImportBatchResponse {

    /** 当前租户 ID。 */
    private Long tenantId;

    /** 触发导入的用户 ID。 */
    private Long userId;

    /** 导入批次 ID。 */
    private String importBatchId;

    /** 本次导入写入的知识库 ID。 */
    private String knowledgeBaseId;

    /** 本次扫描根目录。 */
    private String scanRoot;

    /** 扫描命中的候选文件数量。 */
    private int fileCount;

    /** 成功导入数量。 */
    private int importedCount;

    /** 跳过数量。 */
    private int skippedCount;

    /** 当前向量存储模式。 */
    private String vectorStoreMode;

    /** 当前 embedding 模式。 */
    private String embeddingMode;

    /** 当前 embedding 模型。 */
    private String embeddingModel;

    /** 成功导入的文档 ID 列表。 */
    private List<String> documentIds;

    /** 批次开始时间。 */
    private Instant startedAt;

    /** 批次完成时间。 */
    private Instant finishedAt;

    /** 批次耗时，单位毫秒。 */
    private long latencyMs;
}
