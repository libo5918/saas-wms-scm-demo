package com.example.scm.aiagent.rag.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * RAG 文档切片模型。
 *
 * <p>切片是向量检索的最小单元，必须携带 tenantId、knowledgeBaseId 和 documentId 以支持租户隔离和引用追踪。</p>
 */
@Getter
@Builder
public class RagDocumentChunk {

    /** 当前切片所属租户。 */
    private Long tenantId;

    /** 当前切片所属知识库 ID。 */
    private String knowledgeBaseId;

    /** 当前切片所属文档 ID。 */
    private String documentId;

    /** 切片 ID，通常由 documentId 和序号组合生成。 */
    private String chunkId;

    /** 切片在原文中的序号，从 0 开始。 */
    private int chunkIndex;

    /** 切片文本内容，用于拼接 RAG 上下文，不完整打印到日志。 */
    private String content;

    /** 切片对应的 mock embedding 向量，后续可替换为真实 Embedding 模型输出。 */
    private float[] embedding;

    /** 文档标题冗余，便于检索结果直接展示引用。 */
    private String title;

    /** 文档来源冗余，便于返回引用来源。 */
    private String source;

    /** 切片级扩展元数据，后续写入 Milvus scalar fields 或 MySQL metadata。 */
    private Map<String, Object> metadata;

    /** 切片创建时间。 */
    private Instant createdAt;
}
