package com.example.scm.aiagent.rag.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * RAG 检索命中的切片 DTO。
 */
@Getter
@Builder
public class RagRetrievedChunk {

    /** 当前租户 ID。 */
    private Long tenantId;

    /** 知识库 ID。 */
    private String knowledgeBaseId;

    /** 文档 ID。 */
    private String documentId;

    /** 切片 ID。 */
    private String chunkId;

    /** 切片序号。 */
    private int chunkIndex;

    /** 文档标题。 */
    private String title;

    /** 文档来源。 */
    private String source;

    /** 命中切片内容，接口可返回用于调试和引用展示，日志中不完整打印。 */
    private String content;

    /** 相似度分数，当前 in-memory 模式使用 cosine similarity。 */
    private double score;

    /** 切片扩展元数据。 */
    private Map<String, Object> metadata;
}
