package com.example.scm.aiagent.rag;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.model.ChatModelResult;
import com.example.scm.aiagent.model.ModelRoute;
import com.example.scm.aiagent.rag.dto.RagChatRequest;
import com.example.scm.aiagent.rag.dto.RagChatResponse;
import com.example.scm.aiagent.rag.dto.RagDocumentDeleteResponse;
import com.example.scm.aiagent.rag.dto.RagDocumentListResponse;
import com.example.scm.aiagent.rag.dto.RagDocumentRecordResponse;
import com.example.scm.aiagent.rag.dto.RagDocumentUpsertRequest;
import com.example.scm.aiagent.rag.dto.RagDocumentUpsertResponse;
import com.example.scm.aiagent.rag.dto.RagRetrieveRequest;
import com.example.scm.aiagent.rag.dto.RagRetrieveResponse;
import com.example.scm.aiagent.rag.model.RagDocumentChunk;
import com.example.scm.aiagent.rag.service.InMemoryRagVectorStore;
import com.example.scm.aiagent.rag.service.MockRagEmbeddingClient;
import com.example.scm.aiagent.rag.service.RagService;
import com.example.scm.aiagent.rag.service.SimpleRagDocumentChunker;
import com.example.scm.aiagent.service.AgentChatService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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
        assertEquals(0, upsertResponse.getDeletedCount());
        assertEquals("in-memory", upsertResponse.getVectorStoreMode());
        assertEquals("mock", upsertResponse.getEmbeddingMode());

        RagRetrieveRequest retrieveRequest = new RagRetrieveRequest();
        retrieveRequest.setKnowledgeBaseId("kb-project");
        retrieveRequest.setQuery("tenant isolation");
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
        retrieveRequest.setQuery("tenant isolation");
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
        chatRequest.setMessage("How does RAG keep tenant data isolated?");
        chatRequest.setProviderMode("mock");
        chatRequest.setRequestedModel("qwen-plus");
        RagChatResponse response = ragService.ragChat(chatRequest, context);

        assertNotNull(response.getChat());
        assertEquals("mock rag answer", response.getChat().getAnswer());
        assertEquals("rag_qa", response.getChat().getTaskType());
        assertFalse(response.getCitations().isEmpty());
        assertTrue(response.getRetrievalCount() > 0);
    }

    @Test
    void shouldCleanOldChunksWhenReimportingSameDocument() {
        RagService ragService = createRagService();
        AgentRequestContext context = new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));

        RagDocumentUpsertResponse first = ragService.upsertDocument(upsertRequest("kb-project", "doc-reimport"), context);
        RagDocumentUpsertResponse second = ragService.upsertDocument(shortUpsertRequest("kb-project", "doc-reimport"), context);

        assertEquals(first.getChunkCount(), second.getDeletedCount());
        RagRetrieveRequest retrieveRequest = new RagRetrieveRequest();
        retrieveRequest.setKnowledgeBaseId("kb-project");
        retrieveRequest.setQuery("short version");
        retrieveRequest.setTopK(10);
        RagRetrieveResponse retrieveResponse = ragService.retrieve(retrieveRequest, context);

        assertEquals(second.getChunkCount(), retrieveResponse.getRetrievedCount());
        assertTrue(retrieveResponse.getChunks().stream()
                .noneMatch(chunk -> chunk.getContent().contains("old-only-token")));
    }

    @Test
    void shouldRegisterDocumentMetadataAfterUpsertAndUpdateOnRepeatWrite() {
        RagService ragService = createRagService();
        AgentRequestContext context = new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));

        ragService.upsertDocument(upsertRequest("kb-project", "doc-registry"), context);
        RagDocumentRecordResponse first = ragService.getDocument("kb-project", "doc-registry", context);
        ragService.upsertDocument(shortUpsertRequest("kb-project", "doc-registry"), context);
        RagDocumentRecordResponse second = ragService.getDocument("kb-project", "doc-registry", context);

        assertEquals("doc-registry", first.getDocumentId());
        assertEquals("architecture", first.getMetadata().get("domain"));
        assertTrue(second.getDeletedCount() > 0);
        assertEquals(1, second.getChunkCount());
        assertEquals(first.getImportedAt(), second.getImportedAt());
        assertTrue(!second.getUpdatedAt().isBefore(first.getUpdatedAt()));
    }

    @Test
    void shouldListAndDeleteDocumentsWithTenantIsolation() {
        RagService ragService = createRagService();
        AgentRequestContext tenantOne = new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));
        AgentRequestContext tenantTwo = new AgentRequestContext(2L, 20001L, "tenant2", List.of("ROLE_ADMIN"));
        ragService.upsertDocument(upsertRequest("kb-project", "doc-delete"), tenantOne);
        ragService.upsertDocument(upsertRequest("kb-project", "doc-delete"), tenantTwo);

        RagDocumentListResponse tenantOneList = ragService.listDocuments("kb-project", tenantOne);
        RagDocumentDeleteResponse deleteResponse = ragService.deleteDocument("kb-project", "doc-delete", tenantOne);
        RagDocumentListResponse tenantOneAfterDelete = ragService.listDocuments("kb-project", tenantOne);
        RagDocumentListResponse tenantTwoAfterDelete = ragService.listDocuments("kb-project", tenantTwo);

        assertEquals(1, tenantOneList.getDocumentCount());
        assertTrue(deleteResponse.isRegistryDeleted());
        assertTrue(deleteResponse.getDeletedChunkCount() > 0);
        assertEquals(0, tenantOneAfterDelete.getDocumentCount());
        assertEquals(1, tenantTwoAfterDelete.getDocumentCount());
    }

    @Test
    void shouldDeleteByDocumentWithTenantIsolation() {
        InMemoryRagVectorStore vectorStore = new InMemoryRagVectorStore();
        vectorStore.upsert(List.of(chunk(1L, "kb-project", "doc-shared", "chunk-1", "tenant one content")));
        vectorStore.upsert(List.of(chunk(2L, "kb-project", "doc-shared", "chunk-2", "tenant two content")));

        long deletedCount = vectorStore.deleteByDocument(1L, "kb-project", "doc-shared");

        assertEquals(1, deletedCount);
        assertTrue(vectorStore.search(1L, "kb-project", new float[]{1.0f, 0.0f, 0.0f}, 10, Map.of()).isEmpty());
        assertEquals(1, vectorStore.search(2L, "kb-project", new float[]{1.0f, 0.0f, 0.0f}, 10, Map.of()).size());
    }

    @Test
    void shouldFilterLowScoreResultsByThreshold() {
        RagService ragService = createRagService();
        AgentRequestContext context = new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));
        ragService.upsertDocument(upsertRequest("kb-project", "doc-threshold"), context);

        RagRetrieveRequest retrieveRequest = new RagRetrieveRequest();
        retrieveRequest.setKnowledgeBaseId("kb-project");
        retrieveRequest.setQuery("anything");
        retrieveRequest.setTopK(10);
        retrieveRequest.setScoreThreshold(2.0);
        RagRetrieveResponse retrieveResponse = ragService.retrieve(retrieveRequest, context);

        assertEquals(0, retrieveResponse.getRetrievedCount());
        assertTrue(retrieveResponse.getChunks().isEmpty());
    }

    @Test
    void shouldClipRagChatContextBeforeCallingModel() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getRag().getChunk().setSize(220);
        properties.getRag().getChunk().setOverlap(0);
        properties.getRag().getRetrieval().setMaxContextChunks(1);
        properties.getRag().getRetrieval().setMaxContextChunkLength(100);
        AtomicReference<String> promptRef = new AtomicReference<>();
        RagService ragService = createRagService(properties, new InMemoryRagVectorStore(), promptRef);
        AgentRequestContext context = new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));
        ragService.upsertDocument(longUpsertRequest("kb-project", "doc-clip"), context);

        RagChatRequest chatRequest = new RagChatRequest();
        chatRequest.setKnowledgeBaseId("kb-project");
        chatRequest.setMessage("clip context?");
        chatRequest.setTopK(5);
        ragService.ragChat(chatRequest, context);

        assertNotNull(promptRef.get());
        assertTrue(promptRef.get().contains("..."));
        assertFalse(promptRef.get().contains("[引用2]"));
    }

    private RagDocumentUpsertRequest upsertRequest(String knowledgeBaseId, String documentId) {
        RagDocumentUpsertRequest request = new RagDocumentUpsertRequest();
        request.setKnowledgeBaseId(knowledgeBaseId);
        request.setDocumentId(documentId);
        request.setTitle("AI Agent RAG design");
        request.setSource("docs/architecture/ai-agent-roadmap.md");
        request.setContent("RAG must carry tenantId knowledgeBaseId documentId and chunkId. "
                + "Retrieval must filter by tenant to avoid leaking knowledge between customers. "
                + "Milvus is the later real vector database, while unit tests use in-memory vector store. "
                + "old-only-token");
        request.setMetadata(Map.of("domain", "architecture"));
        return request;
    }

    private RagDocumentUpsertRequest shortUpsertRequest(String knowledgeBaseId, String documentId) {
        RagDocumentUpsertRequest request = new RagDocumentUpsertRequest();
        request.setKnowledgeBaseId(knowledgeBaseId);
        request.setDocumentId(documentId);
        request.setTitle("AI Agent RAG design");
        request.setSource("docs/architecture/ai-agent-roadmap.md");
        request.setContent("short version");
        request.setMetadata(Map.of("domain", "architecture"));
        return request;
    }

    private RagDocumentUpsertRequest longUpsertRequest(String knowledgeBaseId, String documentId) {
        RagDocumentUpsertRequest request = new RagDocumentUpsertRequest();
        request.setKnowledgeBaseId(knowledgeBaseId);
        request.setDocumentId(documentId);
        request.setTitle("AI Agent RAG design");
        request.setSource("docs/architecture/ai-agent-roadmap.md");
        request.setContent("clip-token ".repeat(40));
        request.setMetadata(Map.of("domain", "architecture"));
        return request;
    }

    private RagDocumentChunk chunk(Long tenantId, String knowledgeBaseId, String documentId, String chunkId, String content) {
        return RagDocumentChunk.builder()
                .tenantId(tenantId)
                .knowledgeBaseId(knowledgeBaseId)
                .documentId(documentId)
                .chunkId(chunkId)
                .chunkIndex(0)
                .content(content)
                .embedding(new float[]{1.0f, 0.0f, 0.0f})
                .title("title")
                .source("source")
                .metadata(Map.of())
                .createdAt(Instant.now())
                .build();
    }

    private RagService createRagService() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getRag().getChunk().setSize(40);
        properties.getRag().getChunk().setOverlap(8);
        return createRagService(properties, new InMemoryRagVectorStore(), new AtomicReference<>());
    }

    private RagService createRagService(AiAgentProperties properties, InMemoryRagVectorStore vectorStore,
                                        AtomicReference<String> promptRef) {
        MockRagEmbeddingClient embeddingClient = new MockRagEmbeddingClient(properties);
        SimpleRagDocumentChunker chunker = new SimpleRagDocumentChunker(properties, embeddingClient);
        AgentChatService agentChatService = new AgentChatService(
                properties,
                request -> new ModelRoute("qwen-plus", "qwen-plus", "dashscope", "dashscope", "mock",
                        "task_type:rag_qa", List.of("CHAT", "RAG"), List.of("qwen-turbo")),
                invocation -> {
                    promptRef.set(invocation.message());
                    return new ChatModelResult("mock rag answer");
                }
        );
        return new RagService(properties, chunker, embeddingClient, vectorStore, agentChatService);
    }
}
