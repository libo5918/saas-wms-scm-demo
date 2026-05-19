package com.example.scm.aiagent.rag.service;

import com.example.scm.aiagent.config.AiAgentProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 基于 Spring AI 的真实 Embedding 适配器。
 *
 * <p>仅在 ai.agent.rag.embedding.mode=dashscope 或 openai-compatible 时启用，
 * 默认 mock 模式不会创建该 Bean，避免本地启动和单元测试依赖真实 API Key。</p>
 */
@Slf4j
@Component
@ConditionalOnExpression("'${ai.agent.rag.embedding.mode:mock}' == 'dashscope' || '${ai.agent.rag.embedding.mode:mock}' == 'openai-compatible'")
public class SpringAiRagEmbeddingClient implements RagEmbeddingClient {

    private final AiAgentProperties properties;
    private final EmbeddingModel embeddingModel;

    public SpringAiRagEmbeddingClient(AiAgentProperties properties, EmbeddingModel embeddingModel) {
        this.properties = properties;
        this.embeddingModel = embeddingModel;
    }

    /**
     * 调用真实 EmbeddingModel 生成向量。
     *
     * @param text 待向量化文本，日志中不会打印全文，避免泄露文档内容或用户问题
     * @return 真实 embedding 向量
     */
    @Override
    public float[] embed(String text) {
        long startedAt = System.nanoTime();
        String normalizedText = normalizeText(text);
        float[] vector = embeddingModel.embed(normalizedText);
        long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;
        log.info("RAG embedding generated, embeddingMode={}, embeddingModel={}, vectorDimension={}, textLength={}, latencyMs={}",
                mode(), modelName(), vector == null ? 0 : vector.length, normalizedText.length(), latencyMs);
        return vector;
    }

    @Override
    public String mode() {
        return properties.getRag().getEmbedding().getMode();
    }

    @Override
    public String modelName() {
        return properties.getRag().getEmbedding().getModel();
    }

    @Override
    public int dimension() {
        return properties.getRag().getEmbedding().getDimension();
    }

    private String normalizeText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.trim().replaceAll("\\s+", " ");
    }
}
