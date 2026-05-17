package com.example.scm.aiagent.rag.service;

import com.example.scm.aiagent.config.AiAgentProperties;
import org.springframework.stereotype.Component;

/**
 * 本地 mock embedding 实现。
 *
 * <p>通过字符哈希生成稳定向量，用于本地开发和单元测试，避免依赖真实 Embedding API。</p>
 */
@Component
public class MockRagEmbeddingClient implements RagEmbeddingClient {

    private final AiAgentProperties properties;

    public MockRagEmbeddingClient(AiAgentProperties properties) {
        this.properties = properties;
    }

    @Override
    public float[] embed(String text) {
        int dimension = Math.max(8, properties.getRag().getEmbedding().getDimension());
        float[] vector = new float[dimension];
        if (text == null || text.isBlank()) {
            return vector;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = Character.toLowerCase(text.charAt(i));
            int index = Math.floorMod(c * 31 + i, dimension);
            vector[index] += 1.0f;
        }
        normalize(vector);
        return vector;
    }

    private void normalize(float[] vector) {
        double sum = 0.0;
        for (float value : vector) {
            sum += value * value;
        }
        if (sum == 0.0) {
            return;
        }
        float norm = (float) Math.sqrt(sum);
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / norm;
        }
    }
}
