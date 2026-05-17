package com.example.scm.aiagent.rag;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.model.ChatModelResult;
import com.example.scm.aiagent.model.ModelRoute;
import com.example.scm.aiagent.rag.dto.RagChatRequest;
import com.example.scm.aiagent.rag.dto.RagChatResponse;
import com.example.scm.aiagent.rag.dto.RagDocumentUpsertRequest;
import com.example.scm.aiagent.rag.dto.RagDocumentUpsertResponse;
import com.example.scm.aiagent.rag.dto.RagRetrieveRequest;
import com.example.scm.aiagent.rag.dto.RagRetrieveResponse;
import com.example.scm.aiagent.rag.service.InMemoryRagVectorStore;
import com.example.scm.aiagent.rag.service.MockRagEmbeddingClient;
import com.example.scm.aiagent.rag.service.RagService;
import com.example.scm.aiagent.rag.service.SimpleRagDocumentChunker;
import com.example.scm.aiagent.service.AgentChatService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagServiceTest {

    @Test
    void shouldUpsertDocumentAndRetrieveChunksInMemory() {
        RagService ragService = createRagService();
        AgentRequestContext context = new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));

        RagDocumentUpsertResponse upsertResponse = ragService.upsertDocument(upsertRequest("kb-project", "doc-1"), context);
        assertEquals(1L, upsertResponse.getTenantId());
        assertEquals("kb-project", upsertResponse.getKnowledgeBaseId());
        assertEquals("doc-1", upsertResponse.getDocumentId());
        assertTrue(upsertResponse.getChunkCount() > 0);
        assertEquals("in-memory", upsertResponse.getVectorStoreMode());
        assertEquals("mock", upsertResponse.getEmbeddingMode());

        RagRetrieveRequest retrieveRequest = new RagRetrieveRequest();
        retrieveRequest.setKnowledgeBaseId("kb-project");
        retrieveRequest.setQuery("多租户隔离");
        retrieveRequest.setTopK(2);
        RagRetrieveResponse retrieveResponse = ragService.retrieve(retrieveRequest, context);

        assertEquals(1L, retrieveResponse.getTenantId());
        assertFalse(retrieveResponse.getChunks().isEmpty());
        assertTrue(retrieveResponse.getRetrievedCount() <= 2);
        assertEquals("doc-1", retrieveResponse.getChunks().get(0).getDocumentId());
    }

    @Test
    void shouldIsolateRetrievalByTenant() {
        RagService ragService = createRagService();
        AgentRequestContext tenantOne = new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));
        AgentRequestContext tenantTwo = new AgentRequestContext(2L, 20001L, "tenant2", List.of("ROLE_ADMIN"));
        ragService.upsertDocument(upsertRequest("kb-project", "doc-tenant-1"), tenantOne);

        RagRetrieveRequest retrieveRequest = new RagRetrieveRequest();
        retrieveRequest.setKnowledgeBaseId("kb-project");
        retrieveRequest.setQuery("多租户隔离");
        RagRetrieveResponse retrieveResponse = ragService.retrieve(retrieveRequest, tenantTwo);

        assertEquals(2L, retrieveResponse.getTenantId());
        assertEquals(0, retrieveResponse.getRetrievedCount());
    }

    @Test
    void shouldRunRagChatWithRetrievedContext() {
        RagService ragService = createRagService();
        AgentRequestContext context = new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));
        ragService.upsertDocument(upsertRequest("kb-project", "doc-1"), context);

        RagChatRequest chatRequest = new RagChatRequest();
        chatRequest.setKnowledgeBaseId("kb-project");
        chatRequest.setMessage("这个项目的 RAG 如何保证租户隔离？");
        chatRequest.setProviderMode("mock");
        chatRequest.setRequestedModel("qwen-plus");
        RagChatResponse response = ragService.ragChat(chatRequest, context);

        assertNotNull(response.getChat());
        assertEquals("mock rag answer", response.getChat().getAnswer());
        assertEquals("rag_qa", response.getChat().getTaskType());
        assertFalse(response.getCitations().isEmpty());
        assertTrue(response.getRetrievalCount() > 0);
    }

    private RagDocumentUpsertRequest upsertRequest(String knowledgeBaseId, String documentId) {
        RagDocumentUpsertRequest request = new RagDocumentUpsertRequest();
        request.setKnowledgeBaseId(knowledgeBaseId);
        request.setDocumentId(documentId);
        request.setTitle("AI Agent RAG 设计");
        request.setSource("docs/architecture/ai-agent-roadmap.md");
        request.setContent("RAG 必须基于当前项目资料，所有文档切片都必须携带 tenantId、knowledgeBaseId、documentId 和 chunkId。" +
                "检索时需要按租户隔离，避免不同客户之间的知识库内容互相泄露。" +
                "Milvus 作为后续真实向量数据库，当前测试使用 in-memory vector store。");
        request.setMetadata(Map.of("domain", "architecture"));
        return request;
    }

    private RagService createRagService() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getRag().getChunk().setSize(40);
        properties.getRag().getChunk().setOverlap(8);
        MockRagEmbeddingClient embeddingClient = new MockRagEmbeddingClient(properties);
        SimpleRagDocumentChunker chunker = new SimpleRagDocumentChunker(properties, embeddingClient);
        InMemoryRagVectorStore vectorStore = new InMemoryRagVectorStore();
        AgentChatService agentChatService = new AgentChatService(
                properties,
                request -> new ModelRoute("qwen-plus", "qwen-plus", "dashscope", "dashscope", "mock",
                        "task_type:rag_qa", List.of("CHAT", "RAG"), List.of("qwen-turbo")),
                invocation -> new ChatModelResult("mock rag answer")
        );
        return new RagService(properties, chunker, embeddingClient, vectorStore, agentChatService);
    }
}
