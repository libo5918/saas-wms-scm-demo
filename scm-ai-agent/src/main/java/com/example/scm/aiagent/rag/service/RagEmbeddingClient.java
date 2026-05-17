package com.example.scm.aiagent.rag.service;

/**
 * Embedding 客户端抽象。
 *
 * <p>当前默认实现为 mock embedding，后续可替换为 Spring AI EmbeddingModel 或 DashScope Embedding。</p>
 */
public interface RagEmbeddingClient {

    /**
     * 将文本转换为向量。
     *
     * @param text 待向量化文本，调用方不得把完整文本写入日志
     * @return embedding 向量
     */
    float[] embed(String text);
}
