package com.example.scm.aiagent.rag.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * RAG docs 导入批次列表响应。
 */
@Getter
@Builder
public class RagImportBatchListResponse {

    /** 当前租户 ID。 */
    private Long tenantId;

    /** 批次数量。 */
    private int batchCount;

    /** 导入批次列表。 */
    private List<RagImportBatchResponse> batches;
}
