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

    /**
     * 当前 embedding 模式，例如 mock、dashscope、openai-compatible。
     *
     * @return embedding 模式
     */
    default String mode() {
        return "mock";
    }

    /**
     * 当前使用的 embedding 模型名称，用于日志、排查和面试讲解。
     *
     * @return embedding 模型名称
     */
    default String modelName() {
        return "mock-embedding";
    }

    /**
     * 当前 embedding 向量维度；Milvus collection 的 vector dimension 必须与该值一致。
     *
     * @return 向量维度
     */
    default int dimension() {
        return 64;
    }
}
