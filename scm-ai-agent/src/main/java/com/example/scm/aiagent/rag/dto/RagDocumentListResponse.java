package com.example.scm.aiagent.rag.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * RAG 文档列表响应。
 */
@Getter
@Builder
public class RagDocumentListResponse {

    /** 当前租户 ID。 */
    private Long tenantId;

    /** 当前查询的知识库 ID。 */
    private String knowledgeBaseId;

    /** 文档数量。 */
    private int documentCount;

    /** 文档治理元数据列表。 */
    private List<RagDocumentRecordResponse> documents;
}
