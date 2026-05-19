package com.example.scm.aiagent.rag.service;

import com.example.scm.common.core.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MilvusFilterExpressionBuilderTest {

    private final MilvusFilterExpressionBuilder builder = new MilvusFilterExpressionBuilder();

    @Test
    void shouldBuildTenantKnowledgeBaseAndMetadataFilterExpression() {
        String expression = builder.build(1L, "kb-project-docs", Map.of(
                "directory", "docs/operations",
                "filePath", "docs/operations/skywalking-integration.md",
                "importSource", "docs-auto-import",
                "chunkIndex", 2
        ));

        assertTrue(expression.contains("tenant_id == 1"));
        assertTrue(expression.contains("knowledge_base_id == \"kb-project-docs\""));
        assertTrue(expression.contains("directory == \"docs/operations\""));
        assertTrue(expression.contains("file_path == \"docs/operations/skywalking-integration.md\""));
        assertTrue(expression.contains("import_source == \"docs-auto-import\""));
        assertTrue(expression.contains("chunk_index == 2"));
    }

    @Test
    void shouldEscapeStringFilterValue() {
        String expression = builder.build(1L, "kb\"x", Map.of("source", "docs\\operations\\a\"b.md"));

        assertEquals("tenant_id == 1 and knowledge_base_id == \"kb\\\"x\" and source == \"docs\\\\operations\\\\a\\\"b.md\"",
                expression);
    }

    @Test
    void shouldBuildDocumentDeleteExpression() {
        String expression = builder.buildDocumentExpression(1L, "kb-project-docs", "doc-001");

        assertEquals("tenant_id == 1 and knowledge_base_id == \"kb-project-docs\" and document_id == \"doc-001\"",
                expression);
    }

    @Test
    void shouldRejectUnsupportedFilterField() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> builder.build(1L, "kb-project-docs", Map.of("tenant_id or 1 == 1", "x")));

        assertTrue(exception.getMessage().contains("Unsupported Milvus metadata filter"));
    }

    @Test
    void shouldRejectNonNumericChunkIndex() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> builder.build(1L, "kb-project-docs", Map.of("chunkIndex", "abc")));

        assertTrue(exception.getMessage().contains("numeric filter"));
    }
}
