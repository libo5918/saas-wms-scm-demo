package com.example.scm.aiagent.rag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * RAG 文档写入请求。
 */
@Getter
@Setter
public class RagDocumentUpsertRequest {

    /** 知识库 ID，不同知识库之间默认不互相检索。 */
    @NotBlank(message = "knowledgeBaseId must not be blank")
    private String knowledgeBaseId;

    /** 文档 ID，不传时由服务端自动生成。 */
    private String documentId;

    /** 文档标题，用于检索引用展示。 */
    @NotBlank(message = "title must not be blank")
    private String title;

    /** 文档来源，例如 docs/architecture/ai-agent-roadmap.md。 */
    private String source;

    /** 原始文档正文，服务端会按配置切片。 */
    @NotBlank(message = "content must not be blank")
    private String content;

    /** 文档扩展元数据，后续可用于 Milvus metadata filter。 */
    private Map<String, Object> metadata = new HashMap<>();
}
