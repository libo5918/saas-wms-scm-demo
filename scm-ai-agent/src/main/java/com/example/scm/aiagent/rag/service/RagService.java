package com.example.scm.aiagent.rag.service;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.dto.ChatRequest;
import com.example.scm.aiagent.dto.ChatResponse;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.rag.dto.RagChatRequest;
import com.example.scm.aiagent.rag.dto.RagChatResponse;
import com.example.scm.aiagent.rag.dto.RagDocumentUpsertRequest;
import com.example.scm.aiagent.rag.dto.RagDocumentUpsertResponse;
import com.example.scm.aiagent.rag.dto.RagRetrieveRequest;
import com.example.scm.aiagent.rag.dto.RagRetrieveResponse;
import com.example.scm.aiagent.rag.dto.RagRetrievedChunk;
import com.example.scm.aiagent.rag.model.RagDocument;
import com.example.scm.aiagent.rag.model.RagDocumentChunk;
import com.example.scm.aiagent.service.AgentChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * RAG 搴旂敤鏈嶅姟銆? *
 * <p>璐熻矗鏂囨。鍐欏叆銆佸垏鐗囥€乵ock embedding銆佸悜閲忔绱㈠拰 RAG Chat 缂栨帓锛屾槸 Phase 3 鐨勬渶灏忛棴鐜叆鍙ｃ€?/p>
 */
@Slf4j
@Service
public class RagService {

    private final AiAgentProperties properties;
    private final RagDocumentChunker documentChunker;
    private final RagEmbeddingClient embeddingClient;
    private final RagVectorStore vectorStore;
    private final AgentChatService agentChatService;

    public RagService(AiAgentProperties properties, RagDocumentChunker documentChunker,
                      RagEmbeddingClient embeddingClient, RagVectorStore vectorStore,
                      AgentChatService agentChatService) {
        this.properties = properties;
        this.documentChunker = documentChunker;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
        this.agentChatService = agentChatService;
    }

    /**
     * 鍐欏叆鏂囨。骞剁敓鎴愬垏鐗囧悜閲忋€?     *
     * @param request 鏂囨。鍐欏叆璇锋眰
     * @param context 褰撳墠绉熸埛鍜岀敤鎴蜂笂涓嬫枃
     * @return 鍐欏叆缁撴灉
     */
    public RagDocumentUpsertResponse upsertDocument(RagDocumentUpsertRequest request, AgentRequestContext context) {
        long startedAt = System.nanoTime();
        String documentId = StringUtils.hasText(request.getDocumentId())
                ? request.getDocumentId()
                : UUID.randomUUID().toString();
        RagDocument document = RagDocument.builder()
                .tenantId(context.tenantId())
                .knowledgeBaseId(request.getKnowledgeBaseId())
                .documentId(documentId)
                .title(request.getTitle())
                .source(request.getSource())
                .content(request.getContent())
                .metadata(request.getMetadata())
                .createdAt(Instant.now())
                .build();

        long deletedCount = vectorStore.deleteByDocument(context.tenantId(), request.getKnowledgeBaseId(), documentId);
        List<RagDocumentChunk> chunks = documentChunker.chunk(document);
        vectorStore.upsert(chunks);

        RagDocumentUpsertResponse response = new RagDocumentUpsertResponse();
        response.setTenantId(context.tenantId());
        response.setKnowledgeBaseId(request.getKnowledgeBaseId());
        response.setDocumentId(documentId);
        response.setChunkCount(chunks.size());
        response.setDeletedCount(deletedCount);
        response.setVectorStoreMode(properties.getRag().getVectorStore().getMode());
        response.setEmbeddingMode(properties.getRag().getEmbedding().getMode());
        response.setCreatedAt(Instant.now());
        long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;
        log.info("RAG document upserted, tenantId={}, userId={}, knowledgeBaseId={}, documentId={}, deletedCount={}, chunkCount={}, vectorStoreMode={}, embeddingMode={}, embeddingModel={}, vectorDimension={}, latencyMs={}",
                context.tenantId(), context.userId(), request.getKnowledgeBaseId(), documentId, deletedCount, chunks.size(),
                response.getVectorStoreMode(), response.getEmbeddingMode(), embeddingClient.modelName(),
                embeddingClient.dimension(), latencyMs);
        return response;
    }

    /**
     * 鍩轰簬 query 妫€绱㈢浉鍏冲垏鐗囥€?     *
     * @param request 妫€绱㈣姹?     * @param context 褰撳墠绉熸埛鍜岀敤鎴蜂笂涓嬫枃
     * @return 妫€绱㈢粨鏋?     */
    public RagRetrieveResponse retrieve(RagRetrieveRequest request, AgentRequestContext context) {
        long startedAt = System.nanoTime();
        int topK = resolveTopK(request.getTopK());
        double scoreThreshold = resolveScoreThreshold(request.getScoreThreshold());
        String normalizedQuery = normalizeQuery(request.getQuery());
        float[] queryEmbedding = embeddingClient.embed(normalizedQuery);
        List<RagRetrievedChunk> chunks = applyScoreThreshold(vectorStore.search(context.tenantId(), request.getKnowledgeBaseId(),
                queryEmbedding, topK, request.getFilters()), scoreThreshold);

        RagRetrieveResponse response = new RagRetrieveResponse();
        response.setTenantId(context.tenantId());
        response.setKnowledgeBaseId(request.getKnowledgeBaseId());
        response.setRetrievedCount(chunks.size());
        response.setChunks(chunks);
        response.setLatencyMs((System.nanoTime() - startedAt) / 1_000_000);
        log.info("RAG retrieve finished, tenantId={}, userId={}, knowledgeBaseId={}, embeddingMode={}, embeddingModel={}, vectorDimension={}, topK={}, scoreThreshold={}, retrievedCount={}, latencyMs={}",
                context.tenantId(), context.userId(), request.getKnowledgeBaseId(), embeddingClient.mode(),
                embeddingClient.modelName(), queryEmbedding == null ? 0 : queryEmbedding.length, topK, scoreThreshold, chunks.size(),
                response.getLatencyMs());
        return response;
    }

    /**
     * 鎵ц RAG Chat锛氬厛妫€绱㈠垏鐗囷紝鍐嶆嫾鎺ヤ笂涓嬫枃骞跺鐢ㄧ幇鏈?AgentChatService 璋冪敤妯″瀷銆?     *
     * @param request RAG Chat 璇锋眰
     * @param context 褰撳墠绉熸埛鍜岀敤鎴蜂笂涓嬫枃
     * @return RAG Chat 鍝嶅簲
     */
    public RagChatResponse ragChat(RagChatRequest request, AgentRequestContext context) {
        long startedAt = System.nanoTime();
        RagRetrieveRequest retrieveRequest = new RagRetrieveRequest();
        retrieveRequest.setKnowledgeBaseId(request.getKnowledgeBaseId());
        retrieveRequest.setQuery(request.getMessage());
        retrieveRequest.setTopK(request.getTopK());
        retrieveRequest.setScoreThreshold(request.getScoreThreshold());
        retrieveRequest.setFilters(request.getFilters());
        RagRetrieveResponse retrieveResponse = retrieve(retrieveRequest, context);

        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setMessage(buildAugmentedPrompt(request.getMessage(), retrieveResponse.getChunks()));
        chatRequest.setConversationId(request.getConversationId());
        chatRequest.setTaskType(StringUtils.hasText(request.getTaskType()) ? request.getTaskType() : "rag_qa");
        chatRequest.setRequestedModel(request.getRequestedModel());
        chatRequest.setProviderMode(request.getProviderMode());
        chatRequest.setRequiredCapabilities(resolveRagCapabilities(request.getRequiredCapabilities()));
        chatRequest.setCostLevel(request.getCostLevel());
        chatRequest.setMaxLatencyMs(request.getMaxLatencyMs());
        chatRequest.setMetadata(request.getMetadata());
        ChatResponse chatResponse = agentChatService.chat(chatRequest, context);

        RagChatResponse response = new RagChatResponse();
        response.setChat(chatResponse);
        response.setCitations(retrieveResponse.getChunks());
        response.setRetrievalCount(retrieveResponse.getRetrievedCount());
        response.setLatencyMs((System.nanoTime() - startedAt) / 1_000_000);
        log.info("RAG chat finished, tenantId={}, userId={}, knowledgeBaseId={}, topK={}, scoreThreshold={}, retrievedCount={}, contextChunkLimit={}, contextChunkLengthLimit={}, modelName={}, provider={}, latencyMs={}",
                context.tenantId(), context.userId(), request.getKnowledgeBaseId(), resolveTopK(request.getTopK()),
                resolveScoreThreshold(request.getScoreThreshold()), response.getRetrievalCount(),
                properties.getRag().getRetrieval().getMaxContextChunks(),
                properties.getRag().getRetrieval().getMaxContextChunkLength(),
                chatResponse.getModelName(), chatResponse.getProvider(), response.getLatencyMs());
        return response;
    }

    private int resolveTopK(Integer topK) {
        if (topK == null || topK <= 0) {
            return properties.getRag().getRetrieval().getDefaultTopK();
        }
        return Math.min(topK, properties.getRag().getRetrieval().getMaxTopK());
    }

    private double resolveScoreThreshold(Double requestScoreThreshold) {
        if (requestScoreThreshold != null) {
            return Math.max(0.0, requestScoreThreshold);
        }
        return Math.max(0.0, properties.getRag().getRetrieval().getScoreThreshold());
    }

    private String normalizeQuery(String query) {
        return query == null ? "" : query.trim().replaceAll("\\s+", " ");
    }

    private List<RagRetrievedChunk> applyScoreThreshold(List<RagRetrievedChunk> chunks, double scoreThreshold) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        if (scoreThreshold <= 0) {
            return chunks;
        }
        return chunks.stream()
                .filter(chunk -> chunk.getScore() >= scoreThreshold)
                .toList();
    }

    private List<String> resolveRagCapabilities(List<String> requestedCapabilities) {
        LinkedHashSet<String> capabilities = new LinkedHashSet<>();
        capabilities.add("CHAT");
        capabilities.add("RAG");
        if (requestedCapabilities != null) {
            capabilities.addAll(requestedCapabilities);
        }
        return new ArrayList<>(capabilities);
    }

    private String buildAugmentedPrompt(String userMessage, List<RagRetrievedChunk> chunks) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是 SCM/WMS 项目的知识问答助手。请只基于下面的检索上下文回答，无法从上下文确认时要说明不确定。\n\n");
        prompt.append("检索上下文：\n");
        int maxContextChunks = Math.max(1, properties.getRag().getRetrieval().getMaxContextChunks());
        int maxContextChunkLength = Math.max(100, properties.getRag().getRetrieval().getMaxContextChunkLength());
        List<RagRetrievedChunk> contextChunks = chunks == null ? List.of() : chunks.stream()
                .limit(maxContextChunks)
                .toList();
        for (int i = 0; i < contextChunks.size(); i++) {
            RagRetrievedChunk chunk = contextChunks.get(i);
            prompt.append("[引用").append(i + 1).append("] ")
                    .append("title=").append(nullToBlank(chunk.getTitle()))
                    .append(", source=").append(nullToBlank(chunk.getSource()))
                    .append(", chunkId=").append(chunk.getChunkId())
                    .append("\n")
                    .append(clipContent(chunk.getContent(), maxContextChunkLength))
                    .append("\n\n");
        }
        prompt.append("用户问题：\n").append(userMessage);
        return prompt.toString();
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private String clipContent(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content == null ? "" : content;
        }
        return content.substring(0, maxLength) + "...";
    }
}
