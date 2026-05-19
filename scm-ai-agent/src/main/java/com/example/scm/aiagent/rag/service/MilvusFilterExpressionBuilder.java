package com.example.scm.aiagent.rag.service;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Milvus metadata filter 表达式构建器。
 *
 * <p>只允许白名单字段参与过滤，避免调用方把任意字符串拼进 Milvus filter expression。</p>
 */
class MilvusFilterExpressionBuilder {

    static final String FIELD_TENANT_ID = "tenant_id";
    static final String FIELD_KNOWLEDGE_BASE_ID = "knowledge_base_id";
    static final String FIELD_DOCUMENT_ID = "document_id";
    static final String FIELD_CHUNK_ID = "chunk_id";
    static final String FIELD_CHUNK_INDEX = "chunk_index";
    static final String FIELD_TITLE = "title";
    static final String FIELD_SOURCE = "source";
    static final String FIELD_FILE_PATH = "file_path";
    static final String FIELD_FILE_NAME = "file_name";
    static final String FIELD_DIRECTORY = "directory";
    static final String FIELD_EXTENSION = "extension";
    static final String FIELD_IMPORT_SOURCE = "import_source";

    private static final Map<String, FieldSpec> ALLOWED_FILTER_FIELDS = allowedFilterFields();

    /**
     * 构建 Milvus filter expression，基础租户和知识库过滤始终强制添加。
     */
    String build(Long tenantId, String knowledgeBaseId, Map<String, Object> filters) {
        if (tenantId == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "tenantId must not be null");
        }
        if (!StringUtils.hasText(knowledgeBaseId)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "knowledgeBaseId must not be blank");
        }
        StringBuilder expression = new StringBuilder();
        expression.append(FIELD_TENANT_ID).append(" == ").append(tenantId);
        expression.append(" and ").append(FIELD_KNOWLEDGE_BASE_ID).append(" == \"")
                .append(escape(knowledgeBaseId)).append("\"");
        if (filters == null || filters.isEmpty()) {
            return expression.toString();
        }
        filters.forEach((key, value) -> appendFilter(expression, key, value));
        return expression.toString();
    }

    /**
     * 构建文档级删除表达式，始终强制包含租户、知识库和 documentId。
     */
    String buildDocumentExpression(Long tenantId, String knowledgeBaseId, String documentId) {
        if (!StringUtils.hasText(documentId)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "documentId must not be blank");
        }
        return build(tenantId, knowledgeBaseId, Map.of("documentId", documentId));
    }

    private void appendFilter(StringBuilder expression, String key, Object value) {
        if (!StringUtils.hasText(key) || value == null) {
            return;
        }
        FieldSpec fieldSpec = ALLOWED_FILTER_FIELDS.get(normalizeKey(key));
        if (fieldSpec == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Unsupported Milvus metadata filter: " + key);
        }
        expression.append(" and ").append(fieldSpec.fieldName()).append(" == ");
        if (fieldSpec.numeric()) {
            expression.append(parseLong(key, value));
            return;
        }
        expression.append("\"").append(escape(String.valueOf(value))).append("\"");
    }

    private long parseLong(String key, Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Milvus numeric filter must be a number: " + key);
        }
    }

    private String normalizeKey(String key) {
        return key.trim().replace("-", "").replace("_", "").toLowerCase(Locale.ROOT);
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static Map<String, FieldSpec> allowedFilterFields() {
        Map<String, FieldSpec> fields = new LinkedHashMap<>();
        add(fields, "documentId", FIELD_DOCUMENT_ID, false);
        add(fields, "document_id", FIELD_DOCUMENT_ID, false);
        add(fields, "chunkId", FIELD_CHUNK_ID, false);
        add(fields, "chunk_id", FIELD_CHUNK_ID, false);
        add(fields, "chunkIndex", FIELD_CHUNK_INDEX, true);
        add(fields, "chunk_index", FIELD_CHUNK_INDEX, true);
        add(fields, "title", FIELD_TITLE, false);
        add(fields, "source", FIELD_SOURCE, false);
        add(fields, "filePath", FIELD_FILE_PATH, false);
        add(fields, "file_path", FIELD_FILE_PATH, false);
        add(fields, "fileName", FIELD_FILE_NAME, false);
        add(fields, "file_name", FIELD_FILE_NAME, false);
        add(fields, "directory", FIELD_DIRECTORY, false);
        add(fields, "extension", FIELD_EXTENSION, false);
        add(fields, "importSource", FIELD_IMPORT_SOURCE, false);
        add(fields, "import_source", FIELD_IMPORT_SOURCE, false);
        return fields;
    }

    private static void add(Map<String, FieldSpec> fields, String key, String fieldName, boolean numeric) {
        fields.put(key.replace("_", "").toLowerCase(Locale.ROOT), new FieldSpec(fieldName, numeric));
    }

    /**
     * Milvus 过滤字段定义。
     */
    private record FieldSpec(String fieldName, boolean numeric) {
    }
}
