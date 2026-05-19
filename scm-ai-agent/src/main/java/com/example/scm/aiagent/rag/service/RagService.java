package com.example.scm.aiagent.rag.service;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.dto.ChatRequest;
import com.example.scm.aiagent.dto.ChatResponse;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.rag.dto.RagChatRequest;
import com.example.scm.aiagent.rag.dto.RagChatResponse;
import com.example.scm.aiagent.rag.dto.RagDocumentDeleteResponse;
import com.example.scm.aiagent.rag.dto.RagDocumentListResponse;
import com.example.scm.aiagent.rag.dto.RagDocumentRecordResponse;
import com.example.scm.aiagent.rag.dto.RagDocumentUpsertRequest;
import com.example.scm.aiagent.rag.dto.RagDocumentUpsertResponse;
import com.example.scm.aiagent.rag.dto.RagImportBatchListResponse;
import com.example.scm.aiagent.rag.dto.RagImportBatchResponse;
import com.example.scm.aiagent.rag.dto.RagRetrieveRequest;
import com.example.scm.aiagent.rag.dto.RagRetrieveResponse;
import com.example.scm.aiagent.rag.dto.RagRetrievedChunk;
import com.example.scm.aiagent.rag.model.RagDocument;
import com.example.scm.aiagent.rag.model.RagDocumentChunk;
import com.example.scm.aiagent.rag.model.RagDocumentRecord;
import com.example.scm.aiagent.rag.model.RagImportBatchRecord;
import com.example.scm.aiagent.service.AgentChatService;
import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
 * <p>负责文档写入、旧 chunk 清理、文档切片、Embedding、向量检索、RAG Chat 编排和文档治理元数据登记。</p>
 */
@Slf4j
@Service
public class RagService {

    private final AiAgentProperties properties;
    private final RagDocumentChunker documentChunker;
    private final RagEmbeddingClient embeddingClient;
    private final RagVectorStore vectorStore;
    private final AgentChatService agentChatService;
    private final RagDocumentRegistry documentRegistry;

    @Autowired
    public RagService(AiAgentProperties properties, RagDocumentChunker documentChunker,
                      RagEmbeddingClient embeddingClient, RagVectorStore vectorStore,
                      AgentChatService agentChatService, RagDocumentRegistry documentRegistry) {
        this.properties = properties;
        this.documentChunker = documentChunker;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
        this.agentChatService = agentChatService;
        this.documentRegistry = documentRegistry;
    }

    public RagService(AiAgentProperties properties, RagDocumentChunker documentChunker,
                      RagEmbeddingClient embeddingClient, RagVectorStore vectorStore,
                      AgentChatService agentChatService) {
        this.properties = properties;
        this.documentChunker = documentChunker;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
        this.agentChatService = agentChatService;
        this.documentRegistry = new InMemoryRagDocumentRegistry();
    }

    /**
     * 写入文档并生成向量切片。
     *
     * <p>写入前会先按租户、知识库、文档 ID 删除旧 chunk，避免重复导入和旧切片残留。</p>
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

        long deletedCount = vectorStore.deleteByDocument(context.tenantId(), request.getKnowledgeBaseId(), documentId);
        List<RagDocumentChunk> chunks = documentChunker.chunk(document);
        vectorStore.upsert(chunks);
        Instant now = Instant.now();
        RagDocumentRecord existingRecord = documentRegistry
                .findDocument(context.tenantId(), request.getKnowledgeBaseId(), documentId)
                .orElse(null);
        RagDocumentRecord record = buildDocumentRecord(request, context, documentId, chunks.size(), deletedCount,
                existingRecord, now);
        documentRegistry.saveDocument(record);

        RagDocumentUpsertResponse response = new RagDocumentUpsertResponse();
        response.setTenantId(context.tenantId());
        response.setKnowledgeBaseId(request.getKnowledgeBaseId());
        response.setDocumentId(documentId);
        response.setChunkCount(chunks.size());
        response.setDeletedCount(deletedCount);
        response.setVectorStoreMode(properties.getRag().getVectorStore().getMode());
        response.setEmbeddingMode(properties.getRag().getEmbedding().getMode());
        response.setEmbeddingModel(embeddingClient.modelName());
        response.setImportBatchId(request.getImportBatchId());
        response.setCreatedAt(record.getImportedAt());
        response.setUpdatedAt(record.getUpdatedAt());
        long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;
        log.info("RAG document upserted, tenantId={}, userId={}, knowledgeBaseId={}, documentId={}, importBatchId={}, deletedCount={}, chunkCount={}, registryMode={}, vectorStoreMode={}, embeddingMode={}, embeddingModel={}, vectorDimension={}, latencyMs={}",
                context.tenantId(), context.userId(), request.getKnowledgeBaseId(), documentId, request.getImportBatchId(),
                deletedCount, chunks.size(), properties.getRag().getRegistry().getMode(),
                response.getVectorStoreMode(), response.getEmbeddingMode(),
                embeddingClient.modelName(), embeddingClient.dimension(), latencyMs);
        return response;
    }

    /**
     * 查询知识库文档列表。
     *
     * @param knowledgeBaseId 知识库 ID
     * @param context 当前租户和用户上下文
     * @return 文档列表响应
     */
    public RagDocumentListResponse listDocuments(String knowledgeBaseId, AgentRequestContext context) {
        List<RagDocumentRecordResponse> documents = documentRegistry
                .listDocuments(context.tenantId(), knowledgeBaseId)
                .stream()
                .map(this::toDocumentResponse)
                .toList();
        log.info("RAG document list queried, tenantId={}, userId={}, knowledgeBaseId={}, documentCount={}",
                context.tenantId(), context.userId(), knowledgeBaseId, documents.size());
        return RagDocumentListResponse.builder()
                .tenantId(context.tenantId())
                .knowledgeBaseId(knowledgeBaseId)
                .documentCount(documents.size())
                .documents(documents)
                .build();
    }

    /**
     * 查询文档治理元数据详情。
     *
     * @param knowledgeBaseId 知识库 ID
     * @param documentId 文档 ID
     * @param context 当前租户和用户上下文
     * @return 文档详情响应
     */
    public RagDocumentRecordResponse getDocument(String knowledgeBaseId, String documentId, AgentRequestContext context) {
        RagDocumentRecord record = documentRegistry.findDocument(context.tenantId(), knowledgeBaseId, documentId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "RAG document not found"));
        log.info("RAG document detail queried, tenantId={}, userId={}, knowledgeBaseId={}, documentId={}",
                context.tenantId(), context.userId(), knowledgeBaseId, documentId);
        return toDocumentResponse(record);
    }

    /**
     * 删除文档治理记录，并联动删除 VectorStore 中对应文档的 chunk。
     *
     * @param knowledgeBaseId 知识库 ID
     * @param documentId 文档 ID
     * @param context 当前租户和用户上下文
     * @return 删除结果
     */
    public RagDocumentDeleteResponse deleteDocument(String knowledgeBaseId, String documentId, AgentRequestContext context) {
        long startedAt = System.nanoTime();
        long deletedChunkCount = vectorStore.deleteByDocument(context.tenantId(), knowledgeBaseId, documentId);
        boolean registryDeleted = documentRegistry.deleteDocument(context.tenantId(), knowledgeBaseId, documentId).isPresent();
        long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;
        log.info("RAG document deleted, tenantId={}, userId={}, knowledgeBaseId={}, documentId={}, registryDeleted={}, deletedCount={}, registryMode={}, vectorStoreMode={}, latencyMs={}",
                context.tenantId(), context.userId(), knowledgeBaseId, documentId, registryDeleted, deletedChunkCount,
                properties.getRag().getRegistry().getMode(), properties.getRag().getVectorStore().getMode(), latencyMs);
        return RagDocumentDeleteResponse.builder()
                .tenantId(context.tenantId())
                .knowledgeBaseId(knowledgeBaseId)
                .documentId(documentId)
                .registryDeleted(registryDeleted)
                .deletedChunkCount(deletedChunkCount)
                .build();
    }

    /**
     * 保存 docs 导入批次记录。
     *
     * @param record 导入批次记录
     */
    public void saveImportBatch(RagImportBatchRecord record) {
        documentRegistry.saveImportBatch(record);
    }

    /**
     * 查询当前租户的导入批次列表。
     *
     * @param context 当前租户和用户上下文
     * @return 批次列表响应
     */
    public RagImportBatchListResponse listImportBatches(AgentRequestContext context) {
        List<RagImportBatchResponse> batches = documentRegistry.listImportBatches(context.tenantId())
                .stream()
                .map(this::toImportBatchResponse)
                .toList();
        log.info("RAG import batch list queried, tenantId={}, userId={}, registryMode={}, batchCount={}",
                context.tenantId(), context.userId(), properties.getRag().getRegistry().getMode(), batches.size());
        return RagImportBatchListResponse.builder()
                .tenantId(context.tenantId())
                .batchCount(batches.size())
                .batches(batches)
                .build();
    }

    /**
     * 查询导入批次详情。
     *
     * @param importBatchId 导入批次 ID
     * @param context 当前租户和用户上下文
     * @return 批次详情响应
     */
    public RagImportBatchResponse getImportBatch(String importBatchId, AgentRequestContext context) {
        RagImportBatchRecord record = documentRegistry.findImportBatch(context.tenantId(), importBatchId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "RAG import batch not found"));
        log.info("RAG import batch detail queried, tenantId={}, userId={}, importBatchId={}",
                context.tenantId(), context.userId(), importBatchId);
        return toImportBatchResponse(record);
    }

    /**
     * 基于 query 检索知识库中的相关切片。
     *
     * @param request 检索请求
     * @param context 当前租户和用户上下文
     * @return 检索结果
     */
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
                embeddingClient.modelName(), queryEmbedding == null ? 0 : queryEmbedding.length, topK, scoreThreshold,
                chunks.size(), response.getLatencyMs());
        return response;
    }

    /**
     * 执行 RAG Chat：先检索切片，再把引用上下文拼接到提示词中，最后复用 AgentChatService 调用模型。
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

    private RagDocumentRecord buildDocumentRecord(RagDocumentUpsertRequest request, AgentRequestContext context,
                                                  String documentId, int chunkCount, long deletedCount,
                                                  RagDocumentRecord existingRecord, Instant now) {
        return RagDocumentRecord.builder()
                .tenantId(context.tenantId())
                .knowledgeBaseId(request.getKnowledgeBaseId())
                .documentId(documentId)
                .title(request.getTitle())
                .source(request.getSource())
                .filePath(metadataValue(request, "filePath", request.getSource()))
                .fileName(metadataValue(request, "fileName", null))
                .directory(metadataValue(request, "directory", null))
                .importSource(metadataValue(request, "importSource", "manual-upsert"))
                .chunkCount(chunkCount)
                .deletedCount(deletedCount)
                .embeddingMode(properties.getRag().getEmbedding().getMode())
                .embeddingModel(embeddingClient.modelName())
                .vectorStoreMode(properties.getRag().getVectorStore().getMode())
                .importBatchId(request.getImportBatchId())
                .importedAt(existingRecord == null ? now : existingRecord.getImportedAt())
                .updatedAt(now)
                .metadata(request.getMetadata())
                .build();
    }

    private String metadataValue(RagDocumentUpsertRequest request, String key, String defaultValue) {
        if (request.getMetadata() == null || request.getMetadata().get(key) == null) {
            return defaultValue;
        }
        return String.valueOf(request.getMetadata().get(key));
    }

    private RagDocumentRecordResponse toDocumentResponse(RagDocumentRecord record) {
        return RagDocumentRecordResponse.builder()
                .tenantId(record.getTenantId())
                .knowledgeBaseId(record.getKnowledgeBaseId())
                .documentId(record.getDocumentId())
                .title(record.getTitle())
                .source(record.getSource())
                .filePath(record.getFilePath())
                .fileName(record.getFileName())
                .directory(record.getDirectory())
                .importSource(record.getImportSource())
                .chunkCount(record.getChunkCount())
                .deletedCount(record.getDeletedCount())
                .embeddingMode(record.getEmbeddingMode())
                .embeddingModel(record.getEmbeddingModel())
                .vectorStoreMode(record.getVectorStoreMode())
                .importBatchId(record.getImportBatchId())
                .importedAt(record.getImportedAt())
                .updatedAt(record.getUpdatedAt())
                .metadata(record.getMetadata())
                .build();
    }

    private RagImportBatchResponse toImportBatchResponse(RagImportBatchRecord record) {
        return RagImportBatchResponse.builder()
                .tenantId(record.getTenantId())
                .userId(record.getUserId())
                .importBatchId(record.getImportBatchId())
                .knowledgeBaseId(record.getKnowledgeBaseId())
                .scanRoot(record.getScanRoot())
                .fileCount(record.getFileCount())
                .importedCount(record.getImportedCount())
                .skippedCount(record.getSkippedCount())
                .vectorStoreMode(record.getVectorStoreMode())
                .embeddingMode(record.getEmbeddingMode())
                .embeddingModel(record.getEmbeddingModel())
                .documentIds(record.getDocumentIds())
                .startedAt(record.getStartedAt())
                .finishedAt(record.getFinishedAt())
                .latencyMs(record.getLatencyMs())
                .build();
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
