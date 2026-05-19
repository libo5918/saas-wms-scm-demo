package com.example.scm.aiagent.rag.service;

import com.example.scm.aiagent.rag.model.RagDocumentRecord;
import com.example.scm.aiagent.rag.model.RagImportBatchRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * RAG 文档治理元数据的内存实现。
 *
 * <p>该实现用于本地开发和单元测试，不依赖 MySQL。生产环境后续可替换为持久化实现。</p>
 */
@Slf4j
@Component
public class InMemoryRagDocumentRegistry implements RagDocumentRegistry {

    private final ConcurrentMap<String, RagDocumentRecord> documents = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RagImportBatchRecord> importBatches = new ConcurrentHashMap<>();

    @Override
    public void saveDocument(RagDocumentRecord record) {
        documents.put(documentKey(record.getTenantId(), record.getKnowledgeBaseId(), record.getDocumentId()), record);
        log.info("RAG document registry saved, tenantId={}, knowledgeBaseId={}, documentId={}, importBatchId={}, chunkCount={}",
                record.getTenantId(), record.getKnowledgeBaseId(), record.getDocumentId(),
                record.getImportBatchId(), record.getChunkCount());
    }

    @Override
    public List<RagDocumentRecord> listDocuments(Long tenantId, String knowledgeBaseId) {
        return documents.values().stream()
                .filter(record -> same(tenantId, record.getTenantId()))
                .filter(record -> same(knowledgeBaseId, record.getKnowledgeBaseId()))
                .sorted(Comparator.comparing(RagDocumentRecord::getUpdatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public Optional<RagDocumentRecord> findDocument(Long tenantId, String knowledgeBaseId, String documentId) {
        return Optional.ofNullable(documents.get(documentKey(tenantId, knowledgeBaseId, documentId)));
    }

    @Override
    public Optional<RagDocumentRecord> deleteDocument(Long tenantId, String knowledgeBaseId, String documentId) {
        RagDocumentRecord removed = documents.remove(documentKey(tenantId, knowledgeBaseId, documentId));
        if (removed != null) {
            log.info("RAG document registry deleted, tenantId={}, knowledgeBaseId={}, documentId={}",
                    tenantId, knowledgeBaseId, documentId);
        }
        return Optional.ofNullable(removed);
    }

    @Override
    public void saveImportBatch(RagImportBatchRecord record) {
        importBatches.put(batchKey(record.getTenantId(), record.getImportBatchId()), record);
        log.info("RAG import batch registry saved, tenantId={}, importBatchId={}, knowledgeBaseId={}, importedCount={}, skippedCount={}",
                record.getTenantId(), record.getImportBatchId(), record.getKnowledgeBaseId(),
                record.getImportedCount(), record.getSkippedCount());
    }

    @Override
    public List<RagImportBatchRecord> listImportBatches(Long tenantId) {
        return importBatches.values().stream()
                .filter(record -> same(tenantId, record.getTenantId()))
                .sorted(Comparator.comparing(RagImportBatchRecord::getStartedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public Optional<RagImportBatchRecord> findImportBatch(Long tenantId, String importBatchId) {
        return Optional.ofNullable(importBatches.get(batchKey(tenantId, importBatchId)));
    }

    private String documentKey(Long tenantId, String knowledgeBaseId, String documentId) {
        return tenantId + ":" + knowledgeBaseId + ":" + documentId;
    }

    private String batchKey(Long tenantId, String importBatchId) {
        return tenantId + ":" + importBatchId;
    }

    private boolean same(Object expected, Object actual) {
        return expected != null && expected.equals(actual);
    }
}
