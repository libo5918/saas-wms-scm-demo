package com.example.scm.aiagent.rag.service;

import com.example.scm.aiagent.config.AiAgentProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SpringAiRagEmbeddingClientTest {

    @Test
    void shouldDelegateToSpringAiEmbeddingModelAndNormalizeInput() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getRag().getEmbedding().setMode("dashscope");
        properties.getRag().getEmbedding().setModel("text-embedding-v3");
        properties.getRag().getEmbedding().setDimension(1024);
        AtomicReference<String> capturedText = new AtomicReference<>();

        EmbeddingModel embeddingModel = new EmbeddingModel() {
            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                throw new UnsupportedOperationException("unit test uses embed(String) directly");
            }

            @Override
            public float[] embed(String text) {
                capturedText.set(text);
                return new float[]{1.0f, 0.0f, 0.5f};
            }

            @Override
            public float[] embed(Document document) {
                return new float[]{1.0f};
            }
        };

        SpringAiRagEmbeddingClient client = new SpringAiRagEmbeddingClient(properties, embeddingModel);

        assertArrayEquals(new float[]{1.0f, 0.0f, 0.5f}, client.embed("  SkyWalking   接入说明  "));
        assertEquals("SkyWalking 接入说明", capturedText.get());
        assertEquals("dashscope", client.mode());
        assertEquals("text-embedding-v3", client.modelName());
        assertEquals(1024, client.dimension());
    }
}
