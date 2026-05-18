package com.example.scm.aiagent.rag.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * docs 目录导入 RAG 知识库的响应结果。
 */
@Getter
@Setter
public class RagDocsImportResponse {

    /** 当前租户 ID，由 gateway 透传并从 TenantContext 读取。 */
    private Long tenantId;

    /** 当前用户 ID，由 gateway 透传。 */
    private Long userId;

    /** 本次导入写入的知识库 ID。 */
    private String knowledgeBaseId;

    /** 本次实际扫描的 docs 根目录。 */
    private String scanRoot;

    /** 扫描命中的候选文件数量。 */
    private int fileCount;

    /** 成功写入 RAG 知识库的文件数量。 */
    private int importedCount;

    /** 因配置、数量限制或读取失败跳过的文件数量。 */
    private int skippedCount;

    /** 当前向量存储模式，例如 in-memory 或 milvus。 */
    private String vectorStoreMode;

    /** 当前 embedding 模式，例如 mock 或 spring-ai。 */
    private String embeddingMode;

    /** 本次导入总耗时，单位毫秒。 */
    private long latencyMs;

    /** 成功导入的文档摘要列表。 */
    private List<RagDocsImportedDocument> documents = new ArrayList<>();
}
