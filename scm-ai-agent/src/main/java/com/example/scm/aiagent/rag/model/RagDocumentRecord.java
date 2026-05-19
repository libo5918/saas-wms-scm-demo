package com.example.scm.aiagent.rag.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * RAG 文档注册表记录。
 *
 * <p>该模型只保存文档治理元数据，不保存完整正文。当前阶段由内存实现承载，后续可以平滑迁移到 MySQL。</p>
 */
@Getter
@Builder(toBuilder = true)
public class RagDocumentRecord {

    /** 当前文档所属租户 ID，用于保证多租户隔离。 */
    private Long tenantId;

    /** 当前文档所属知识库 ID。 */
    private String knowledgeBaseId;

    /** 文档唯一 ID，同一租户和知识库下保持唯一。 */
    private String documentId;

    /** 文档标题，用于列表、详情和引用展示。 */
    private String title;

    /** 文档来源，例如 docs/operations/xxx.md。 */
    private String source;

    /** 文档文件路径，通常与 docs 自动导入的 source 保持一致。 */
    private String filePath;

    /** 文档文件名，例如 skywalking-integration.md。 */
    private String fileName;

    /** 文档所在目录，例如 docs/operations。 */
    private String directory;

    /** 导入来源，例如 docs-auto-import 或 manual-upsert。 */
    private String importSource;

    /** 当前版本文档切片数量。 */
    private int chunkCount;

    /** 本次写入前删除的旧 chunk 数量。 */
    private long deletedCount;

    /** 当前使用的 embedding 模式，例如 mock、dashscope。 */
    private String embeddingMode;

    /** 当前使用的 embedding 模型名称。 */
    private String embeddingModel;

    /** 当前使用的向量存储模式，例如 in-memory、milvus。 */
    private String vectorStoreMode;

    /** docs 自动导入批次 ID，手动写入时可以为空。 */
    private String importBatchId;

    /** 文档首次导入时间。 */
    private Instant importedAt;

    /** 文档最近更新时间。 */
    private Instant updatedAt;

    /** 扩展元数据，保留给后续标签、业务域、版本等治理字段。 */
    private Map<String, Object> metadata;
}
