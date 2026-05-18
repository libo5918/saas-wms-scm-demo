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
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * Milvus 向量存储实现。
 *
 * <p>仅在 ai.agent.rag.vector-store.mode=milvus 时启用，负责把 RAG chunk 写入 Milvus 并按租户、知识库过滤检索。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ai.agent.rag.vector-store", name = "mode", havingValue = "milvus")
public class MilvusRagVectorStore implements RagVectorStore {

    private static final String FIELD_TENANT_ID = "tenant_id";
    private static final String FIELD_KNOWLEDGE_BASE_ID = "knowledge_base_id";
    private static final String FIELD_DOCUMENT_ID = "document_id";
    private static final String FIELD_CHUNK_INDEX = "chunk_index";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_SOURCE = "source";
    private static final String FIELD_CONTENT = "content";

    private final AiAgentProperties properties;
    private final MilvusClientV2 milvusClient;

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
        milvusClient.insert(InsertReq.builder()
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
    public List<RagRetrievedChunk> search(Long tenantId, String knowledgeBaseId, float[] queryEmbedding, int topK,
                                          Map<String, Object> filters) {
        long startedAt = System.nanoTime();
        AiAgentProperties.MilvusProperties milvus = properties.getRag().getVectorStore().getMilvus();
        List<String> outputFields = List.of(
                milvus.getPrimaryField(), FIELD_TENANT_ID, FIELD_KNOWLEDGE_BASE_ID, FIELD_DOCUMENT_ID,
                FIELD_CHUNK_INDEX, FIELD_TITLE, FIELD_SOURCE, FIELD_CONTENT
        );
        SearchResp searchResp = milvusClient.search(SearchReq.builder()
                .collectionName(milvus.getCollectionName())
                .data(List.of(new FloatVec(queryEmbedding)))
                .annsField(milvus.getVectorField())
                .filter(buildFilter(tenantId, knowledgeBaseId, filters))
                .topK(Math.max(1, topK))
                .outputFields(outputFields)
                .build());
        List<RagRetrievedChunk> chunks = toRetrievedChunks(searchResp, milvus.getPrimaryField());
        long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;
        log.info("RAG Milvus search finished, tenantId={}, knowledgeBaseId={}, collectionName={}, topK={}, metricType={}, retrievedCount={}, latencyMs={}",
                tenantId, knowledgeBaseId, milvus.getCollectionName(), topK, milvus.getMetricType(), chunks.size(), latencyMs);
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
            log.info("RAG Milvus collection already exists, collectionName={}, vectorField={}, metricType={}",
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
        row.addProperty(FIELD_CONTENT, safe(chunk.getContent()));
        JsonArray vector = new JsonArray();
        for (float value : chunk.getEmbedding()) {
            vector.add(value);
        }
        row.add(vectorField, vector);
        return row;
    }

    private String buildFilter(Long tenantId, String knowledgeBaseId, Map<String, Object> filters) {
        StringBuilder expression = new StringBuilder();
        expression.append(FIELD_TENANT_ID).append(" == ").append(tenantId);
        expression.append(" and ").append(FIELD_KNOWLEDGE_BASE_ID).append(" == \"").append(escape(knowledgeBaseId)).append("\"");
        if (filters == null || filters.isEmpty()) {
            return expression.toString();
        }
        filters.forEach((key, value) -> expression.append(" and ").append(key)
                .append(" == \"").append(escape(String.valueOf(value))).append("\""));
        return expression.toString();
    }

    private List<RagRetrievedChunk> toRetrievedChunks(SearchResp searchResp, String primaryField) {
        if (searchResp == null || searchResp.getSearchResults() == null || searchResp.getSearchResults().isEmpty()) {
            return List.of();
        }
        return searchResp.getSearchResults().get(0).stream()
                .map(result -> {
                    Map<String, Object> entity = result.getEntity();
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
                            .metadata(Map.of())
                            .build();
                })
                .toList();
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

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
