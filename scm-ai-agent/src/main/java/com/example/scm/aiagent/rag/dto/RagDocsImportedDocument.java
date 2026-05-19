package com.example.scm.aiagent.rag.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * docs 自动导入后单个文档的导入结果。
 */
@Getter
@Builder
public class RagDocsImportedDocument {

    /** 稳定生成的文档 ID，后续重复导入同一文件会保持一致。 */
    private String documentId;

    /** 文档标题，优先取 Markdown 一级标题。 */
    private String title;

    /** 文档来源路径，使用相对路径方便在响应和引用中展示。 */
    private String source;

    /** 当前文档切片后写入的 chunk 数量。 */
    private int chunkCount;

    /** 本次写入前删除的旧 chunk 数量。 */
    private long deletedCount;

    /** 本文档所属导入批次 ID。 */
    private String importBatchId;
}
