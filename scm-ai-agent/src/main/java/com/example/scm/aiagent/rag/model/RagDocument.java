package com.example.scm.aiagent.rag.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * RAG 原始文档模型。
 *
 * <p>用于描述一次导入的文档内容和来源信息，当前阶段由内存存储承载，后续 metadata 进入 MySQL。</p>
 */
@Getter
@Builder
public class RagDocument {

    /** 当前文档所属租户，用于保证知识库数据隔离。 */
    private Long tenantId;

    /** 文档所属知识库 ID。 */
    private String knowledgeBaseId;

    /** 文档 ID，调用方可传入，不传时服务端自动生成。 */
    private String documentId;

    /** 文档标题，用于检索引用展示。 */
    private String title;

    /** 文档来源，例如 docs/architecture/xxx.md 或业务系统来源。 */
    private String source;

    /** 原始文档正文，仅用于切片，不应完整写入日志。 */
    private String content;

    /** 文档扩展元数据，例如业务域、版本、标签等。 */
    private Map<String, Object> metadata;

    /** 文档导入时间。 */
    private Instant createdAt;
}
