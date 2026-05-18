package com.example.scm.aiagent.rag;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.rag.service.InMemoryRagVectorStore;
import com.example.scm.aiagent.rag.service.MilvusRagVectorStore;
import com.example.scm.aiagent.rag.service.RagVectorStore;
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
        "ai.agent.rag.vector-store.mode=in-memory",
        "ai.agent.rag.vector-store.milvus.uri=http://localhost:19530",
        "ai.agent.rag.vector-store.milvus.collection-name=scm_ai_rag_chunks_test",
        "ai.agent.rag.vector-store.milvus.primary-field=chunk_id",
        "ai.agent.rag.vector-store.milvus.vector-field=embedding",
        "ai.agent.rag.vector-store.milvus.metric-type=COSINE",
        "ai.agent.rag.vector-store.milvus.index-type=AUTOINDEX"
})
class RagMilvusConfigurationSmokeTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RagVectorStore ragVectorStore;

    @Autowired
    private AiAgentProperties properties;

    @Test
    void shouldKeepInMemoryDefaultAndBindMilvusPropertiesWithoutConnectingMilvus() {
        assertInstanceOf(InMemoryRagVectorStore.class, ragVectorStore);
        assertEquals(0, applicationContext.getBeanNamesForType(MilvusRagVectorStore.class, false, false).length);
        assertEquals("in-memory", properties.getRag().getVectorStore().getMode());
        assertEquals("http://localhost:19530", properties.getRag().getVectorStore().getMilvus().getUri());
        assertEquals("scm_ai_rag_chunks_test", properties.getRag().getVectorStore().getMilvus().getCollectionName());
        assertEquals("chunk_id", properties.getRag().getVectorStore().getMilvus().getPrimaryField());
        assertEquals("embedding", properties.getRag().getVectorStore().getMilvus().getVectorField());
        assertEquals("COSINE", properties.getRag().getVectorStore().getMilvus().getMetricType());
        assertEquals("AUTOINDEX", properties.getRag().getVectorStore().getMilvus().getIndexType());
    }
}