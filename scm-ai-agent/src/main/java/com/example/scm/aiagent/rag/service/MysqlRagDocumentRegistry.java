package com.example.scm.aiagent.rag.service;

import com.example.scm.aiagent.rag.model.RagDocumentRecord;
import com.example.scm.aiagent.rag.model.RagImportBatchRecord;
import com.example.scm.aiagent.rag.persistence.mapper.RagDocumentRegistryMapper;
import com.example.scm.aiagent.rag.persistence.po.RagDocumentRecordPO;
import com.example.scm.aiagent.rag.persistence.po.RagImportBatchPO;
import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * RAG 文档治理元数据的 MySQL 实现。
 *
 * <p>该实现通过 MyBatis Mapper 执行 SQL，只有配置 `ai.agent.rag.registry.mode=mysql` 时才启用。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ai.agent.rag.registry", name = "mode", havingValue = "mysql")
public class MysqlRagDocumentRegistry implements RagDocumentRegistry {

    private static final TypeReference<Map<String, Object>> METADATA_TYPE = new TypeReference<>() {
    };

    private final RagDocumentRegistryMapper mapper;
    private final ObjectMapper objectMapper;

    public MysqlRagDocumentRegistry(RagDocumentRegistryMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void saveDocument(RagDocumentRecord record) {
        mapper.upsertDocument(toDocumentPo(record));
        log.info("RAG MySQL document registry saved, tenantId={}, knowledgeBaseId={}, documentId={}, importBatchId={}, chunkCount={}",
                record.getTenantId(), record.getKnowledgeBaseId(), record.getDocumentId(),
                record.getImportBatchId(), record.getChunkCount());
    }

    @Override
    public List<RagDocumentRecord> listDocuments(Long tenantId, String knowledgeBaseId) {
        return mapper.selectDocuments(tenantId, knowledgeBaseId)
                .stream()
                .map(this::toDocumentRecord)
                .toList();
    }

    @Override
    public Optional<RagDocumentRecord> findDocument(Long tenantId, String knowledgeBaseId, String documentId) {
        return Optional.ofNullable(mapper.selectDocument(tenantId, knowledgeBaseId, documentId))
                .map(this::toDocumentRecord);
    }

    @Override
    public Optional<RagDocumentRecord> deleteDocument(Long tenantId, String knowledgeBaseId, String documentId) {
        Optional<RagDocumentRecord> existing = findDocument(tenantId, knowledgeBaseId, documentId);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        mapper.deleteDocument(tenantId, knowledgeBaseId, documentId);
        log.info("RAG MySQL document registry deleted, tenantId={}, knowledgeBaseId={}, documentId={}",
                tenantId, knowledgeBaseId, documentId);
        return existing;
    }

    @Override
    @Transactional(transactionManager = "ragRegistryTransactionManager")
    public void saveImportBatch(RagImportBatchRecord record) {
        mapper.upsertImportBatch(toImportBatchPo(record));
        mapper.deleteImportDocuments(record.getTenantId(), record.getImportBatchId());
        List<String> documentIds = record.getDocumentIds() == null ? List.of() : record.getDocumentIds();
        for (String documentId : documentIds) {
            mapper.insertImportDocument(record.getTenantId(), record.getImportBatchId(),
                    record.getKnowledgeBaseId(), documentId);
        }
        log.info("RAG MySQL import batch saved, tenantId={}, importBatchId={}, knowledgeBaseId={}, importedCount={}, skippedCount={}",
                record.getTenantId(), record.getImportBatchId(), record.getKnowledgeBaseId(),
                record.getImportedCount(), record.getSkippedCount());
    }

    @Override
    public List<RagImportBatchRecord> listImportBatches(Long tenantId) {
        return mapper.selectImportBatches(tenantId)
                .stream()
                .map(this::toImportBatchRecord)
                .toList();
    }

    @Override
    public Optional<RagImportBatchRecord> findImportBatch(Long tenantId, String importBatchId) {
        return Optional.ofNullable(mapper.selectImportBatch(tenantId, importBatchId))
                .map(this::toImportBatchRecord);
    }

    private RagDocumentRecordPO toDocumentPo(RagDocumentRecord record) {
        RagDocumentRecordPO po = new RagDocumentRecordPO();
        po.setTenantId(record.getTenantId());
        po.setKnowledgeBaseId(record.getKnowledgeBaseId());
        po.setDocumentId(record.getDocumentId());
        po.setTitle(record.getTitle());
        po.setSource(record.getSource());
        po.setFilePath(record.getFilePath());
        po.setFileName(record.getFileName());
        po.setDirectory(record.getDirectory());
        po.setImportSource(record.getImportSource());
        po.setChunkCount(record.getChunkCount());
        po.setDeletedCount(record.getDeletedCount());
        po.setEmbeddingMode(record.getEmbeddingMode());
        po.setEmbeddingModel(record.getEmbeddingModel());
        po.setVectorStoreMode(record.getVectorStoreMode());
        po.setImportBatchId(record.getImportBatchId());
        po.setMetadataJson(toJson(record.getMetadata()));
        po.setImportedAt(record.getImportedAt());
        po.setUpdatedAt(record.getUpdatedAt());
        return po;
    }

    private RagDocumentRecord toDocumentRecord(RagDocumentRecordPO po) {
        return RagDocumentRecord.builder()
                .tenantId(po.getTenantId())
                .knowledgeBaseId(po.getKnowledgeBaseId())
                .documentId(po.getDocumentId())
                .title(po.getTitle())
                .source(po.getSource())
                .filePath(po.getFilePath())
                .fileName(po.getFileName())
                .directory(po.getDirectory())
                .importSource(po.getImportSource())
                .chunkCount(po.getChunkCount() == null ? 0 : po.getChunkCount())
                .deletedCount(po.getDeletedCount() == null ? 0 : po.getDeletedCount())
                .embeddingMode(po.getEmbeddingMode())
                .embeddingModel(po.getEmbeddingModel())
                .vectorStoreMode(po.getVectorStoreMode())
                .importBatchId(po.getImportBatchId())
                .metadata(fromJson(po.getMetadataJson()))
                .importedAt(po.getImportedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private RagImportBatchPO toImportBatchPo(RagImportBatchRecord record) {
        RagImportBatchPO po = new RagImportBatchPO();
        po.setTenantId(record.getTenantId());
        po.setImportBatchId(record.getImportBatchId());
        po.setUserId(record.getUserId());
        po.setKnowledgeBaseId(record.getKnowledgeBaseId());
        po.setScanRoot(record.getScanRoot());
        po.setFileCount(record.getFileCount());
        po.setImportedCount(record.getImportedCount());
        po.setSkippedCount(record.getSkippedCount());
        po.setVectorStoreMode(record.getVectorStoreMode());
        po.setEmbeddingMode(record.getEmbeddingMode());
        po.setEmbeddingModel(record.getEmbeddingModel());
        po.setStartedAt(record.getStartedAt());
        po.setFinishedAt(record.getFinishedAt());
        po.setLatencyMs(record.getLatencyMs());
        return po;
    }

    private RagImportBatchRecord toImportBatchRecord(RagImportBatchPO po) {
        return RagImportBatchRecord.builder()
                .tenantId(po.getTenantId())
                .importBatchId(po.getImportBatchId())
                .userId(po.getUserId())
                .knowledgeBaseId(po.getKnowledgeBaseId())
                .scanRoot(po.getScanRoot())
                .fileCount(po.getFileCount() == null ? 0 : po.getFileCount())
                .importedCount(po.getImportedCount() == null ? 0 : po.getImportedCount())
                .skippedCount(po.getSkippedCount() == null ? 0 : po.getSkippedCount())
                .vectorStoreMode(po.getVectorStoreMode())
                .embeddingMode(po.getEmbeddingMode())
                .embeddingModel(po.getEmbeddingModel())
                .documentIds(mapper.selectImportDocumentIds(po.getTenantId(), po.getImportBatchId()))
                .startedAt(po.getStartedAt())
                .finishedAt(po.getFinishedAt())
                .latencyMs(po.getLatencyMs() == null ? 0 : po.getLatencyMs())
                .build();
    }

    private String toJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Invalid RAG document metadata");
        }
    }

    private Map<String, Object> fromJson(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(metadataJson, METADATA_TYPE);
        } catch (JsonProcessingException ex) {
            log.warn("RAG metadata json parse failed, errorType={}", ex.getClass().getSimpleName());
            return Map.of();
        }
    }
}
