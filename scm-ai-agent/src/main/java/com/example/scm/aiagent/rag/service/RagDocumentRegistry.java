package com.example.scm.aiagent.rag.service;

import com.example.scm.aiagent.rag.model.RagDocumentRecord;
import com.example.scm.aiagent.rag.model.RagImportBatchRecord;

import java.util.List;
import java.util.Optional;

/**
 * RAG 文档治理元数据仓库。
 *
 * <p>当前提供内存实现，后续迁移 MySQL 时保持接口语义不变即可。</p>
 */
public interface RagDocumentRegistry {

    /**
     * 保存或更新文档记录。
     *
     * @param record 文档治理元数据
     */
    void saveDocument(RagDocumentRecord record);

    /**
     * 查询知识库下的文档列表。
     *
     * @param tenantId 当前租户 ID
     * @param knowledgeBaseId 知识库 ID
     * @return 文档记录列表
     */
    List<RagDocumentRecord> listDocuments(Long tenantId, String knowledgeBaseId);

    /**
     * 查询单个文档详情。
     *
     * @param tenantId 当前租户 ID
     * @param knowledgeBaseId 知识库 ID
     * @param documentId 文档 ID
     * @return 文档记录
     */
    Optional<RagDocumentRecord> findDocument(Long tenantId, String knowledgeBaseId, String documentId);

    /**
     * 删除文档记录。
     *
     * @param tenantId 当前租户 ID
     * @param knowledgeBaseId 知识库 ID
     * @param documentId 文档 ID
     * @return 被删除的文档记录
     */
    Optional<RagDocumentRecord> deleteDocument(Long tenantId, String knowledgeBaseId, String documentId);

    /**
     * 保存导入批次记录。
     *
     * @param record 导入批次记录
     */
    void saveImportBatch(RagImportBatchRecord record);

    /**
     * 查询当前租户的导入批次列表。
     *
     * @param tenantId 当前租户 ID
     * @return 导入批次列表
     */
    List<RagImportBatchRecord> listImportBatches(Long tenantId);

    /**
     * 查询导入批次详情。
     *
     * @param tenantId 当前租户 ID
     * @param importBatchId 导入批次 ID
     * @return 导入批次记录
     */
    Optional<RagImportBatchRecord> findImportBatch(Long tenantId, String importBatchId);
}
