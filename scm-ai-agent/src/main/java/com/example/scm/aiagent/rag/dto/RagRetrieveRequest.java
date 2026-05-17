package com.example.scm.aiagent.rag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * RAG 检索请求。
 */
@Getter
@Setter
public class RagRetrieveRequest {

    /** 知识库 ID，检索时只查询该知识库下的切片。 */
    @NotBlank(message = "knowledgeBaseId must not be blank")
    private String knowledgeBaseId;

    /** 用户查询文本，用于生成 query embedding。 */
    @NotBlank(message = "query must not be blank")
    private String query;

    /** 返回最相似切片数量，不传时使用系统默认值。 */
    private Integer topK;

    /** 预留 metadata 过滤条件，后续映射到 Milvus scalar filter。 */
    private Map<String, Object> filters = new HashMap<>();
}
