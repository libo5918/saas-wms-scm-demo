package com.example.scm.aiagent.rag;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.rag.service.MockRagEmbeddingClient;
import com.example.scm.aiagent.rag.service.RagEmbeddingClient;
import com.example.scm.aiagent.rag.service.SpringAiRagEmbeddingClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SpringBootTest(properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.ai.model.chat=none",
        "spring.ai.model.embedding=none",
        "spring.ai.model.image=none",
        "spring.ai.model.audio.speech=none",
        "spring.ai.model.audio.transcription=none",
        "spring.ai.model.moderation=none",
        "spring.ai.model.rerank=none",
        "spring.ai.model.video=none",
        "ai.agent.rag.embedding.mode=mock",
        "ai.agent.rag.embedding.model=mock-embedding-test",
        "ai.agent.rag.embedding.provider=mock",
        "ai.agent.rag.embedding.dimension=64",
        "ai.agent.rag.embedding.api-key-env=DASHSCOPE_API_KEY"
})
class RagEmbeddingConfigurationSmokeTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RagEmbeddingClient embeddingClient;

    @Autowired
    private AiAgentProperties properties;

    @Test
    void shouldUseMockEmbeddingByDefaultAndBindProviderPropertiesWithoutApiKey() {
        assertInstanceOf(MockRagEmbeddingClient.class, embeddingClient);
        assertEquals(0, applicationContext.getBeanNamesForType(SpringAiRagEmbeddingClient.class, false, false).length);
        assertEquals("mock", properties.getRag().getEmbedding().getMode());
        assertEquals("mock-embedding-test", properties.getRag().getEmbedding().getModel());
        assertEquals("mock", properties.getRag().getEmbedding().getProvider());
        assertEquals("DASHSCOPE_API_KEY", properties.getRag().getEmbedding().getApiKeyEnv());
        assertEquals(64, properties.getRag().getEmbedding().getDimension());
    }
}
