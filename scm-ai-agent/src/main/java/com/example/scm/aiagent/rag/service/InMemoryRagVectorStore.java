package com.example.scm.aiagent.rag.service;

import com.example.scm.aiagent.rag.dto.RagRetrievedChunk;
import com.example.scm.aiagent.rag.model.RagDocumentChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地内存向量存储。
 *
 * <p>用于 Phase 3 最小闭环和单元测试，按 tenantId + knowledgeBaseId 分桶，模拟 Milvus 的 collection/filter 语义。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ai.agent.rag.vector-store", name = "mode", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryRagVectorStore implements RagVectorStore {

    private final Map<String, Map<String, RagDocumentChunk>> chunksByScope = new ConcurrentHashMap<>();

    @Override
    public void upsert(List<RagDocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        RagDocumentChunk first = chunks.get(0);
        String scope = scopeKey(first.getTenantId(), first.getKnowledgeBaseId());
        Map<String, RagDocumentChunk> scopedChunks = chunksByScope.computeIfAbsent(scope, key -> new ConcurrentHashMap<>());
        chunks.forEach(chunk -> scopedChunks.put(chunk.getChunkId(), chunk));
        log.info("RAG in-memory chunks upserted, tenantId={}, knowledgeBaseId={}, chunkCount={}",
                first.getTenantId(), first.getKnowledgeBaseId(), chunks.size());
    }

    @Override
    public List<RagRetrievedChunk> search(Long tenantId, String knowledgeBaseId, float[] queryEmbedding, int topK,
                                          Map<String, Object> filters) {
        Map<String, RagDocumentChunk> scopedChunks = chunksByScope.getOrDefault(scopeKey(tenantId, knowledgeBaseId), Map.of());
        return scopedChunks.values().stream()
                .filter(chunk -> matchesFilters(chunk, filters))
                .map(chunk -> toRetrievedChunk(chunk, cosine(queryEmbedding, chunk.getEmbedding())))
                .sorted(Comparator.comparingDouble(RagRetrievedChunk::getScore).reversed())
                .limit(Math.max(1, topK))
                .toList();
    }

    private RagRetrievedChunk toRetrievedChunk(RagDocumentChunk chunk, double score) {
        return RagRetrievedChunk.builder()
                .tenantId(chunk.getTenantId())
                .knowledgeBaseId(chunk.getKnowledgeBaseId())
                .documentId(chunk.getDocumentId())
                .chunkId(chunk.getChunkId())
                .chunkIndex(chunk.getChunkIndex())
                .title(chunk.getTitle())
                .source(chunk.getSource())
                .content(chunk.getContent())
                .score(score)
                .metadata(chunk.getMetadata())
                .build();
    }

    private boolean matchesFilters(RagDocumentChunk chunk, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        Map<String, Object> metadata = chunk.getMetadata();
        if (metadata == null || metadata.isEmpty()) {
            return false;
        }
        return filters.entrySet().stream()
                .allMatch(entry -> String.valueOf(entry.getValue()).equals(String.valueOf(metadata.get(entry.getKey()))));
    }

    private double cosine(float[] left, float[] right) {
        if (left == null || right == null || left.length == 0 || right.length == 0) {
            return 0.0;
        }
        int length = Math.min(left.length, right.length);
        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int i = 0; i < length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private String scopeKey(Long tenantId, String knowledgeBaseId) {
        return tenantId + ":" + knowledgeBaseId;
    }
}
