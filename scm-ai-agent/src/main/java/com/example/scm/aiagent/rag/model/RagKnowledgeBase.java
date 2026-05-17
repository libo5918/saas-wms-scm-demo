package com.example.scm.aiagent.rag.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * RAG 知识库元数据。
 *
 * <p>当前阶段暂不落库，先作为文档写入和检索时的业务边界对象，后续可映射到 MySQL 表。</p>
 */
@Getter
@Builder
public class RagKnowledgeBase {

    /** 当前知识库所属租户，所有写入和检索都必须按租户隔离。 */
    private Long tenantId;

    /** 知识库 ID，用于区分项目文档、运维文档、业务手册等不同知识范围。 */
    private String knowledgeBaseId;

    /** 知识库名称，便于后续管理端展示。 */
    private String name;

    /** 知识库扩展属性，后续可承载业务域、权限标签等信息。 */
    private Map<String, Object> metadata;

    /** 知识库创建时间。 */
    private Instant createdAt;
}
