package com.example.scm.aiagent.multiagent;

import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.multiagent.dto.MultiAgentChatRequest;
import com.example.scm.aiagent.multiagent.model.MultiAgentIntentType;
import com.example.scm.aiagent.multiagent.model.MultiAgentPlan;
import com.example.scm.aiagent.multiagent.service.MultiAgentKnowledgeService;
import com.example.scm.aiagent.rag.dto.RagRetrieveRequest;
import com.example.scm.aiagent.rag.dto.RagRetrieveResponse;
import com.example.scm.aiagent.rag.dto.RagRetrievedChunk;
import com.example.scm.aiagent.rag.service.RagService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MultiAgentKnowledgeServiceTest {

    @Test
    void shouldRetrieveWhenRagRequiredAndKnowledgeBaseProvided() {
        RagService ragService = mock(RagService.class);
        RagRetrieveResponse response = new RagRetrieveResponse();
        response.setKnowledgeBaseId("kb-scm-demo");
        response.setRetrievedCount(1);
        response.setLatencyMs(12);
        response.setChunks(List.of(RagRetrievedChunk.builder()
                .documentId("doc-1")
                .chunkId("chunk-1")
                .title("库存规则")
                .source("docs/examples/scm-wms-rules.md")
                .content("库存可用数量等于现存数量减去锁定数量")
                .score(0.91)
                .build()));
        when(ragService.retrieve(any(RagRetrieveRequest.class), any())).thenReturn(response);

        MultiAgentKnowledgeService service = new MultiAgentKnowledgeService(ragService);
        MultiAgentChatRequest request = new MultiAgentChatRequest();
        request.setMessage("解释库存口径");
        request.setKnowledgeBaseId("kb-scm-demo");
        Map<String, Object> result = service.retrieve(request, context(), ragPlan(), true);

        assertEquals("SUCCESS", result.get("status"));
        assertEquals(1, result.get("retrievedCount"));
        verify(ragService).retrieve(any(RagRetrieveRequest.class), any());
    }

    @Test
    void shouldSkipWhenKnowledgeBaseMissing() {
        RagService ragService = mock(RagService.class);
        MultiAgentKnowledgeService service = new MultiAgentKnowledgeService(ragService);
        MultiAgentChatRequest request = new MultiAgentChatRequest();
        request.setMessage("解释库存口径");

        Map<String, Object> result = service.retrieve(request, context(), ragPlan(), true);

        assertEquals("SKIPPED", result.get("status"));
        verify(ragService, never()).retrieve(any(), any());
    }

    private MultiAgentPlan ragPlan() {
        return MultiAgentPlan.builder()
                .intentType(MultiAgentIntentType.RAG_ONLY)
                .needRag(true)
                .needReview(true)
                .reason("知识库解释")
                .build();
    }

    private AgentRequestContext context() {
        return new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));
    }
}
