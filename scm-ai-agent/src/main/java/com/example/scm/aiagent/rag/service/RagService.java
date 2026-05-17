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
 * RAG 应用服务。
 *
 * <p>负责文档写入、切片、mock embedding、向量检索和 RAG Chat 编排，是 Phase 3 的最小闭环入口。</p>
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
     * 写入文档并生成切片向量。
     *
     * @param request 文档写入请求
     * @param context 当前租户和用户上下文
     * @return 写入结果
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

        List<RagDocumentChunk> chunks = documentChunker.chunk(document);
        vectorStore.upsert(chunks);

        RagDocumentUpsertResponse response = new RagDocumentUpsertResponse();
        response.setTenantId(context.tenantId());
        response.setKnowledgeBaseId(request.getKnowledgeBaseId());
        response.setDocumentId(documentId);
        response.setChunkCount(chunks.size());
        response.setVectorStoreMode(properties.getRag().getVectorStore().getMode());
        response.setEmbeddingMode(properties.getRag().getEmbedding().getMode());
        response.setCreatedAt(Instant.now());
        long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;
        log.info("RAG document upserted, tenantId={}, userId={}, knowledgeBaseId={}, documentId={}, chunkCount={}, vectorStoreMode={}, embeddingMode={}, latencyMs={}",
                context.tenantId(), context.userId(), request.getKnowledgeBaseId(), documentId, chunks.size(),
                response.getVectorStoreMode(), response.getEmbeddingMode(), latencyMs);
        return response;
    }

    /**
     * 基于 query 检索相关切片。
     *
     * @param request 检索请求
     * @param context 当前租户和用户上下文
     * @return 检索结果
     */
    public RagRetrieveResponse retrieve(RagRetrieveRequest request, AgentRequestContext context) {
        long startedAt = System.nanoTime();
        int topK = resolveTopK(request.getTopK());
        float[] queryEmbedding = embeddingClient.embed(request.getQuery());
        List<RagRetrievedChunk> chunks = vectorStore.search(context.tenantId(), request.getKnowledgeBaseId(),
                queryEmbedding, topK, request.getFilters());

        RagRetrieveResponse response = new RagRetrieveResponse();
        response.setTenantId(context.tenantId());
        response.setKnowledgeBaseId(request.getKnowledgeBaseId());
        response.setRetrievedCount(chunks.size());
        response.setChunks(chunks);
        response.setLatencyMs((System.nanoTime() - startedAt) / 1_000_000);
        log.info("RAG retrieve finished, tenantId={}, userId={}, knowledgeBaseId={}, topK={}, retrievedCount={}, latencyMs={}",
                context.tenantId(), context.userId(), request.getKnowledgeBaseId(), topK, chunks.size(), response.getLatencyMs());
        return response;
    }

    /**
     * 执行 RAG Chat：先检索切片，再拼接上下文并复用现有 AgentChatService 调用模型。
     *
     * @param request RAG Chat 请求
     * @param context 当前租户和用户上下文
     * @return RAG Chat 响应
     */
    public RagChatResponse ragChat(RagChatRequest request, AgentRequestContext context) {
        long startedAt = System.nanoTime();
        RagRetrieveRequest retrieveRequest = new RagRetrieveRequest();
        retrieveRequest.setKnowledgeBaseId(request.getKnowledgeBaseId());
        retrieveRequest.setQuery(request.getMessage());
        retrieveRequest.setTopK(request.getTopK());
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
        log.info("RAG chat finished, tenantId={}, userId={}, knowledgeBaseId={}, retrievedCount={}, modelName={}, provider={}, latencyMs={}",
                context.tenantId(), context.userId(), request.getKnowledgeBaseId(), response.getRetrievalCount(),
                chatResponse.getModelName(), chatResponse.getProvider(), response.getLatencyMs());
        return response;
    }

    private int resolveTopK(Integer topK) {
        if (topK == null || topK <= 0) {
            return properties.getRag().getRetrieval().getDefaultTopK();
        }
        return Math.min(topK, properties.getRag().getRetrieval().getMaxTopK());
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
        for (int i = 0; i < chunks.size(); i++) {
            RagRetrievedChunk chunk = chunks.get(i);
            prompt.append("[引用").append(i + 1).append("] ")
                    .append("title=").append(nullToBlank(chunk.getTitle()))
                    .append(", source=").append(nullToBlank(chunk.getSource()))
                    .append(", chunkId=").append(chunk.getChunkId())
                    .append("\n")
                    .append(chunk.getContent())
                    .append("\n\n");
        }
        prompt.append("用户问题：\n").append(userMessage);
        return prompt.toString();
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
