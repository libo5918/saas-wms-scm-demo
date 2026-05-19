package com.example.scm.aiagent.rag.service;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.rag.dto.RagRetrievedChunk;
import com.example.scm.aiagent.rag.model.RagDocumentChunk;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.DescribeCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.collection.response.DescribeCollectionResp;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.DeleteResp;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Milvus 向量存储实现。
 *
 * <p>仅在 ai.agent.rag.vector-store.mode=milvus 时启用，负责把 RAG chunk 写入 Milvus 并按租户、知识库过滤检索。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ai.agent.rag.vector-store", name = "mode", havingValue = "milvus")
public class MilvusRagVectorStore implements RagVectorStore {

    private static final String FIELD_TENANT_ID = MilvusFilterExpressionBuilder.FIELD_TENANT_ID;
    private static final String FIELD_KNOWLEDGE_BASE_ID = MilvusFilterExpressionBuilder.FIELD_KNOWLEDGE_BASE_ID;
    private static final String FIELD_DOCUMENT_ID = MilvusFilterExpressionBuilder.FIELD_DOCUMENT_ID;
    private static final String FIELD_CHUNK_INDEX = MilvusFilterExpressionBuilder.FIELD_CHUNK_INDEX;
    private static final String FIELD_TITLE = MilvusFilterExpressionBuilder.FIELD_TITLE;
    private static final String FIELD_SOURCE = MilvusFilterExpressionBuilder.FIELD_SOURCE;
    private static final String FIELD_FILE_PATH = MilvusFilterExpressionBuilder.FIELD_FILE_PATH;
    private static final String FIELD_FILE_NAME = MilvusFilterExpressionBuilder.FIELD_FILE_NAME;
    private static final String FIELD_DIRECTORY = MilvusFilterExpressionBuilder.FIELD_DIRECTORY;
    private static final String FIELD_EXTENSION = MilvusFilterExpressionBuilder.FIELD_EXTENSION;
    private static final String FIELD_IMPORT_SOURCE = MilvusFilterExpressionBuilder.FIELD_IMPORT_SOURCE;
    private static final String FIELD_CONTENT = "content";

    private final AiAgentProperties properties;
    private final MilvusClientV2 milvusClient;
    private final MilvusFilterExpressionBuilder filterExpressionBuilder = new MilvusFilterExpressionBuilder();

    public MilvusRagVectorStore(AiAgentProperties properties) {
        this.properties = properties;
        AiAgentProperties.MilvusProperties milvus = properties.getRag().getVectorStore().getMilvus();
        ConnectConfig.ConnectConfigBuilder builder = ConnectConfig.builder()
                .uri(milvus.getUri());
        if (StringUtils.hasText(milvus.getToken())) {
            builder.token(milvus.getToken());
        }
        this.milvusClient = new MilvusClientV2(builder.build());
        ensureCollection();
    }

    @Override
    public void upsert(List<RagDocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        long startedAt = System.nanoTime();
        AiAgentProperties.MilvusProperties milvus = properties.getRag().getVectorStore().getMilvus();
        List<JsonObject> rows = chunks.stream()
                .map(chunk -> toRow(chunk, milvus.getPrimaryField(), milvus.getVectorField()))
                .toList();
        milvusClient.upsert(UpsertReq.builder()
                .collectionName(milvus.getCollectionName())
                .data(rows)
                .build());
        long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;
        RagDocumentChunk first = chunks.get(0);
        log.info("RAG Milvus chunks inserted, tenantId={}, knowledgeBaseId={}, collectionName={}, chunkCount={}, metricType={}, latencyMs={}",
                first.getTenantId(), first.getKnowledgeBaseId(), milvus.getCollectionName(), chunks.size(),
                milvus.getMetricType(), latencyMs);
    }

    @Override
    public long deleteByDocument(Long tenantId, String knowledgeBaseId, String documentId) {
        long startedAt = System.nanoTime();
        AiAgentProperties.MilvusProperties milvus = properties.getRag().getVectorStore().getMilvus();
        String filterExpression = filterExpressionBuilder.buildDocumentExpression(tenantId, knowledgeBaseId, documentId);
        DeleteResp deleteResp = milvusClient.delete(DeleteReq.builder()
                .collectionName(milvus.getCollectionName())
                .filter(filterExpression)
                .build());
        long deletedCount = deleteResp == null ? 0 : deleteResp.getDeleteCnt();
        long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;
        log.info("RAG Milvus chunks deleted, tenantId={}, knowledgeBaseId={}, documentId={}, collectionName={}, filterExpression={}, deletedCount={}, latencyMs={}",
                tenantId, knowledgeBaseId, documentId, milvus.getCollectionName(), filterExpression, deletedCount, latencyMs);
        return deletedCount;
    }

    @Override
    public List<RagRetrievedChunk> search(Long tenantId, String knowledgeBaseId, float[] queryEmbedding, int topK,
                                          Map<String, Object> filters) {
        long startedAt = System.nanoTime();
        AiAgentProperties.MilvusProperties milvus = properties.getRag().getVectorStore().getMilvus();
        List<String> outputFields = List.of(
                milvus.getPrimaryField(), FIELD_TENANT_ID, FIELD_KNOWLEDGE_BASE_ID, FIELD_DOCUMENT_ID,
                FIELD_CHUNK_INDEX, FIELD_TITLE, FIELD_SOURCE, FIELD_FILE_PATH, FIELD_FILE_NAME, FIELD_DIRECTORY,
                FIELD_EXTENSION, FIELD_IMPORT_SOURCE, FIELD_CONTENT
        );
        String filterExpression = filterExpressionBuilder.build(tenantId, knowledgeBaseId, filters);
        SearchResp searchResp = milvusClient.search(SearchReq.builder()
                .collectionName(milvus.getCollectionName())
                .data(List.of(new FloatVec(queryEmbedding)))
                .annsField(milvus.getVectorField())
                .filter(filterExpression)
                .topK(Math.max(1, topK))
                .outputFields(outputFields)
                .build());
        List<RagRetrievedChunk> chunks = toRetrievedChunks(searchResp, milvus.getPrimaryField());
        long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;
        log.info("RAG Milvus search finished, tenantId={}, knowledgeBaseId={}, collectionName={}, topK={}, filterExpression={}, metricType={}, retrievedCount={}, latencyMs={}",
                tenantId, knowledgeBaseId, milvus.getCollectionName(), topK, filterExpression,
                milvus.getMetricType(), chunks.size(), latencyMs);
        return chunks;
    }

    /**
     * 初始化 collection schema 和索引，后续可以迁移到独立初始化任务。
     */
    private void ensureCollection() {
        AiAgentProperties.MilvusProperties milvus = properties.getRag().getVectorStore().getMilvus();
        boolean exists = milvusClient.hasCollection(HasCollectionReq.builder()
                .collectionName(milvus.getCollectionName())
                .build());
        if (exists) {
            validateExistingCollection(milvus);
            loadCollection(milvus.getCollectionName());
            log.info("RAG Milvus collection already exists, collectionName={}, vectorField={}, metricType={}, metadataFieldsReady=true",
                    milvus.getCollectionName(), milvus.getVectorField(), milvus.getMetricType());
            return;
        }

        CreateCollectionReq.CollectionSchema schema = milvusClient.createSchema();
        schema.addField(AddFieldReq.builder()
                .fieldName(milvus.getPrimaryField())
                .dataType(DataType.VarChar)
                .isPrimaryKey(true)
                .autoID(false)
                .maxLength(256)
                .build());
        schema.addField(AddFieldReq.builder().fieldName(FIELD_TENANT_ID).dataType(DataType.Int64).build());
        schema.addField(AddFieldReq.builder().fieldName(FIELD_KNOWLEDGE_BASE_ID).dataType(DataType.VarChar).maxLength(256).build());
        schema.addField(AddFieldReq.builder().fieldName(FIELD_DOCUMENT_ID).dataType(DataType.VarChar).maxLength(256).build());
        schema.addField(AddFieldReq.builder().fieldName(FIELD_CHUNK_INDEX).dataType(DataType.Int32).build());
        schema.addField(AddFieldReq.builder().fieldName(FIELD_TITLE).dataType(DataType.VarChar).maxLength(512).build());
        schema.addField(AddFieldReq.builder().fieldName(FIELD_SOURCE).dataType(DataType.VarChar).maxLength(1024).build());
        schema.addField(AddFieldReq.builder().fieldName(FIELD_FILE_PATH).dataType(DataType.VarChar).maxLength(1024).build());
        schema.addField(AddFieldReq.builder().fieldName(FIELD_FILE_NAME).dataType(DataType.VarChar).maxLength(512).build());
        schema.addField(AddFieldReq.builder().fieldName(FIELD_DIRECTORY).dataType(DataType.VarChar).maxLength(512).build());
        schema.addField(AddFieldReq.builder().fieldName(FIELD_EXTENSION).dataType(DataType.VarChar).maxLength(64).build());
        schema.addField(AddFieldReq.builder().fieldName(FIELD_IMPORT_SOURCE).dataType(DataType.VarChar).maxLength(128).build());
        schema.addField(AddFieldReq.builder().fieldName(FIELD_CONTENT).dataType(DataType.VarChar).maxLength(8192).build());
        schema.addField(AddFieldReq.builder()
                .fieldName(milvus.getVectorField())
                .dataType(DataType.FloatVector)
                .dimension(properties.getRag().getEmbedding().getDimension())
                .build());

        IndexParam indexParam = IndexParam.builder()
                .fieldName(milvus.getVectorField())
                .indexType(IndexParam.IndexType.valueOf(milvus.getIndexType()))
                .metricType(IndexParam.MetricType.valueOf(milvus.getMetricType()))
                .build();
        milvusClient.createCollection(CreateCollectionReq.builder()
                .collectionName(milvus.getCollectionName())
                .collectionSchema(schema)
                .indexParams(List.of(indexParam))
                .build());
        loadCollection(milvus.getCollectionName());
        log.info("RAG Milvus collection created, collectionName={}, primaryField={}, vectorField={}, dimension={}, indexType={}, metricType={}",
                milvus.getCollectionName(), milvus.getPrimaryField(), milvus.getVectorField(),
                properties.getRag().getEmbedding().getDimension(), milvus.getIndexType(), milvus.getMetricType());
    }

    private JsonObject toRow(RagDocumentChunk chunk, String primaryField, String vectorField) {
        JsonObject row = new JsonObject();
        row.addProperty(primaryField, chunk.getChunkId());
        row.addProperty(FIELD_TENANT_ID, chunk.getTenantId());
        row.addProperty(FIELD_KNOWLEDGE_BASE_ID, chunk.getKnowledgeBaseId());
        row.addProperty(FIELD_DOCUMENT_ID, chunk.getDocumentId());
        row.addProperty(FIELD_CHUNK_INDEX, chunk.getChunkIndex());
        row.addProperty(FIELD_TITLE, safe(chunk.getTitle()));
        row.addProperty(FIELD_SOURCE, safe(chunk.getSource()));
        row.addProperty(FIELD_FILE_PATH, safe(metadataValue(chunk, "filePath", chunk.getSource())));
        row.addProperty(FIELD_FILE_NAME, safe(metadataValue(chunk, "fileName", "")));
        row.addProperty(FIELD_DIRECTORY, safe(metadataValue(chunk, "directory", "")));
        row.addProperty(FIELD_EXTENSION, safe(metadataValue(chunk, "extension", "")));
        row.addProperty(FIELD_IMPORT_SOURCE, safe(metadataValue(chunk, "importSource", "")));
        row.addProperty(FIELD_CONTENT, safe(chunk.getContent()));
        JsonArray vector = new JsonArray();
        for (float value : chunk.getEmbedding()) {
            vector.add(value);
        }
        row.add(vectorField, vector);
        return row;
    }

    private List<RagRetrievedChunk> toRetrievedChunks(SearchResp searchResp, String primaryField) {
        if (searchResp == null || searchResp.getSearchResults() == null || searchResp.getSearchResults().isEmpty()) {
            return List.of();
        }
        return searchResp.getSearchResults().get(0).stream()
                .map(result -> {
                    Map<String, Object> entity = result.getEntity();
                    Map<String, Object> metadata = Map.of(
                            "filePath", Objects.toString(entity.get(FIELD_FILE_PATH), ""),
                            "fileName", Objects.toString(entity.get(FIELD_FILE_NAME), ""),
                            "directory", Objects.toString(entity.get(FIELD_DIRECTORY), ""),
                            "extension", Objects.toString(entity.get(FIELD_EXTENSION), ""),
                            "importSource", Objects.toString(entity.get(FIELD_IMPORT_SOURCE), "")
                    );
                    return RagRetrievedChunk.builder()
                            .tenantId(asLong(entity.get(FIELD_TENANT_ID)))
                            .knowledgeBaseId(asString(entity.get(FIELD_KNOWLEDGE_BASE_ID)))
                            .documentId(asString(entity.get(FIELD_DOCUMENT_ID)))
                            .chunkId(asString(entity.get(primaryField)))
                            .chunkIndex(asInteger(entity.get(FIELD_CHUNK_INDEX)))
                            .title(asString(entity.get(FIELD_TITLE)))
                            .source(asString(entity.get(FIELD_SOURCE)))
                            .content(asString(entity.get(FIELD_CONTENT)))
                            .score(result.getScore())
                            .metadata(metadata)
                            .build();
                })
                .toList();
    }

    private void validateExistingCollection(AiAgentProperties.MilvusProperties milvus) {
        DescribeCollectionResp description = milvusClient.describeCollection(DescribeCollectionReq.builder()
                .collectionName(milvus.getCollectionName())
                .build());
        List<String> requiredFields = List.of(
                milvus.getPrimaryField(), FIELD_TENANT_ID, FIELD_KNOWLEDGE_BASE_ID, FIELD_DOCUMENT_ID,
                FIELD_CHUNK_INDEX, FIELD_TITLE, FIELD_SOURCE, FIELD_FILE_PATH, FIELD_FILE_NAME, FIELD_DIRECTORY,
                FIELD_EXTENSION, FIELD_IMPORT_SOURCE, FIELD_CONTENT, milvus.getVectorField()
        );
        List<String> fieldNames = description.getFieldNames();
        List<String> missingFields = requiredFields.stream()
                .filter(field -> fieldNames == null || !fieldNames.contains(field))
                .toList();
        if (!missingFields.isEmpty()) {
            throw new IllegalStateException("Milvus collection schema is incompatible, collectionName="
                    + milvus.getCollectionName() + ", missingFields=" + missingFields
                    + ". Please drop the old local collection or use another MILVUS_COLLECTION_NAME.");
        }
        log.info("RAG Milvus collection schema checked, collectionName={}, fieldCount={}, vectorField={}, primaryField={}",
                milvus.getCollectionName(), fieldNames.size(), milvus.getVectorField(), milvus.getPrimaryField());
    }

    private void loadCollection(String collectionName) {
        milvusClient.loadCollection(LoadCollectionReq.builder()
                .collectionName(collectionName)
                .build());
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? null : Long.parseLong(String.valueOf(value));
    }

    private int asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return value == null ? 0 : Integer.parseInt(String.valueOf(value));
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String metadataValue(RagDocumentChunk chunk, String key, String defaultValue) {
        if (chunk.getMetadata() == null || !chunk.getMetadata().containsKey(key)) {
            return defaultValue;
        }
        Object value = chunk.getMetadata().get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }
}
