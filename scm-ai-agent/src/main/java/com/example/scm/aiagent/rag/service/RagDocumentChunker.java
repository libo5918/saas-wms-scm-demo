package com.example.scm.aiagent.rag.service;

import com.example.scm.aiagent.rag.model.RagDocument;
import com.example.scm.aiagent.rag.model.RagDocumentChunk;

import java.util.List;

/**
 * 文档切片器抽象。
 */
public interface RagDocumentChunker {

    /**
     * 将原始文档切分为可向量化的 chunk。
     *
     * @param document 原始文档
     * @return 切片列表
     */
    List<RagDocumentChunk> chunk(RagDocument document);
}
