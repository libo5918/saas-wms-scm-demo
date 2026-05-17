package com.example.scm.aiagent.rag.service;

import com.example.scm.aiagent.rag.dto.RagRetrievedChunk;
import com.example.scm.aiagent.rag.model.RagDocumentChunk;

import java.util.List;
import java.util.Map;

/**
 * RAG 向量存储抽象。
 *
 * <p>当前默认实现为 in-memory，后续 Milvus 实现需要保持 tenantId 和 metadata filter 语义一致。</p>
 */
public interface RagVectorStore {

    /**
     * 写入一个文档的所有切片。
     *
     * @param chunks 文档切片列表
     */
    void upsert(List<RagDocumentChunk> chunks);

    /**
     * 按租户、知识库和 query vector 检索相似切片。
     *
     * @param tenantId 当前租户 ID
     * @param knowledgeBaseId 知识库 ID
     * @param queryEmbedding 查询向量
     * @param topK 返回数量
     * @param filters metadata 过滤条件
     * @return 命中的切片列表
     */
    List<RagRetrievedChunk> search(Long tenantId, String knowledgeBaseId, float[] queryEmbedding, int topK,
                                   Map<String, Object> filters);
}
