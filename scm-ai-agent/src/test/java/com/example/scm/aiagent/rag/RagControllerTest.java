package com.example.scm.aiagent.rag;

import com.example.scm.aiagent.config.AiAgentSecurityConfig;
import com.example.scm.aiagent.rag.controller.RagController;
import com.example.scm.aiagent.rag.dto.RagDocumentUpsertResponse;
import com.example.scm.aiagent.rag.dto.RagRetrieveResponse;
import com.example.scm.aiagent.rag.service.RagService;
import com.example.scm.common.security.GatewayHeaders;
import com.example.scm.common.web.GlobalExceptionHandler;
import com.example.scm.common.web.TenantHeaderInterceptor;
import com.example.scm.common.web.WebMvcConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RagController.class)
@Import({GlobalExceptionHandler.class, TenantHeaderInterceptor.class, WebMvcConfiguration.class, AiAgentSecurityConfig.class})
class RagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RagService ragService;

    @Test
    void shouldUpsertDocumentWithTenantAndUserContext() throws Exception {
        RagDocumentUpsertResponse response = new RagDocumentUpsertResponse();
        response.setTenantId(1L);
        response.setKnowledgeBaseId("kb-project");
        response.setDocumentId("doc-1");
        response.setChunkCount(2);
        response.setVectorStoreMode("in-memory");
        response.setEmbeddingMode("mock");
        response.setCreatedAt(Instant.now());
        when(ragService.upsertDocument(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/ai/rag/documents")
                        .header(GatewayHeaders.TENANT_ID, "1")
                        .header(GatewayHeaders.USER_ID, "10001")
                        .header(GatewayHeaders.USERNAME, "admin")
                        .header(GatewayHeaders.USER_ROLES, "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "knowledgeBaseId": "kb-project",
                                  "documentId": "doc-1",
                                  "title": "AI Agent RAG 设计",
                                  "source": "docs/architecture/ai-agent-roadmap.md",
                                  "content": "RAG 文档内容"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tenantId").value(1))
                .andExpect(jsonPath("$.data.knowledgeBaseId").value("kb-project"))
                .andExpect(jsonPath("$.data.chunkCount").value(2));
    }

    @Test
    void shouldRetrieveChunksWithTenantContext() throws Exception {
        RagRetrieveResponse response = new RagRetrieveResponse();
        response.setTenantId(1L);
        response.setKnowledgeBaseId("kb-project");
        response.setRetrievedCount(0);
        response.setLatencyMs(3);
        when(ragService.retrieve(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/ai/rag/retrieve")
                        .header(GatewayHeaders.TENANT_ID, "1")
                        .header(GatewayHeaders.USER_ID, "10001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "knowledgeBaseId": "kb-project",
                                  "query": "多租户隔离",
                                  "topK": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tenantId").value(1))
                .andExpect(jsonPath("$.data.knowledgeBaseId").value("kb-project"));
    }
}
