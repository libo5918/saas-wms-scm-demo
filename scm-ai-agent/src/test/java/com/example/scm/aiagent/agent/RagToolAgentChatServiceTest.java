package com.example.scm.aiagent.agent;

import com.example.scm.aiagent.agent.dto.AgentChatRequest;
import com.example.scm.aiagent.agent.dto.AgentChatResponse;
import com.example.scm.aiagent.agent.service.RagToolAgentChatService;
import com.example.scm.aiagent.agent.service.RagToolAnswerPromptBuilder;
import com.example.scm.aiagent.agent.service.RagToolIntentRouter;
import com.example.scm.aiagent.dto.ChatRequest;
import com.example.scm.aiagent.dto.ChatResponse;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.rag.dto.RagRetrieveResponse;
import com.example.scm.aiagent.rag.dto.RagRetrievedChunk;
import com.example.scm.aiagent.rag.service.RagService;
import com.example.scm.aiagent.service.AgentChatService;
import com.example.scm.aiagent.toolcalling.application.ToolCallingChatService;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatResponse;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecutionView;
import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayData;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolCallingOrchestratorService;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationExecutionSummary;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationRun;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationStep;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationStepStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RagToolAgentChatServiceTest {

    private RagService ragService;
    private ToolCallingChatService toolCallingChatService;
    private ToolCallingOrchestratorService orchestratorService;
    private AgentChatService agentChatService;
    private RagToolAgentChatService service;
    private AgentRequestContext context;

    @BeforeEach
    void setUp() {
        ragService = mock(RagService.class);
        toolCallingChatService = mock(ToolCallingChatService.class);
        orchestratorService = mock(ToolCallingOrchestratorService.class);
        agentChatService = mock(AgentChatService.class);
        service = new RagToolAgentChatService(new RagToolIntentRouter(), ragService, toolCallingChatService,
                orchestratorService, new RagToolAnswerPromptBuilder(new ObjectMapper()), agentChatService);
        context = new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));
        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setAnswer("组合回答");
        when(agentChatService.chat(any(), any())).thenReturn(chatResponse);
    }

    @Test
    void shouldReturnRagToolCombinedResponseAndPromptContext() {
        when(ragService.retrieve(any(), any())).thenReturn(ragResponse());
        when(toolCallingChatService.chat(any(), any())).thenReturn(toolResponse());
        when(orchestratorService.getRun("run-agent-1")).thenReturn(orchestrationRun());

        AgentChatRequest request = new AgentChatRequest();
        request.setRunId("run-agent-1");
        request.setKnowledgeBaseId("kb-scm");
        request.setMessage("按库存口径解释，并查物料 MAT-001 在仓库ID 1 的库存");
        request.setRequestedDomain("mdm");

        AgentChatResponse response = service.chat(request, context);

        assertEquals("run-agent-1", response.getRunId());
        assertEquals("RAG_TOOL", response.getIntentType());
        assertEquals(1, response.getRag().getRetrievedCount());
        assertEquals("mdm.getMaterial", response.getTool().getSelectedTool());
        assertEquals(2, response.getOrchestration().getStepCount());
        assertEquals(128, response.getOrchestration().getSteps().get(1).getSafeFields().get("availableQty"));
        assertFalse(response.getRag().getChunks().get(0).getContentSnippet().contains("rawData"));

        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(agentChatService).chat(captor.capture(), any());
        String prompt = captor.getValue().getMessage();
        assertTrue(prompt.contains("库存可用数量=现存数量-锁定数量"));
        assertTrue(prompt.contains("inventory.getBalance"));
        assertTrue(prompt.contains("availableQty"));
    }

    @Test
    void shouldSkipToolForRagOnlyIntent() {
        when(ragService.retrieve(any(), any())).thenReturn(ragResponse());

        AgentChatRequest request = new AgentChatRequest();
        request.setRunId("run-rag-only");
        request.setKnowledgeBaseId("kb-scm");
        request.setMessage("解释库存可用数量口径");

        AgentChatResponse response = service.chat(request, context);

        assertEquals("RAG_ONLY", response.getIntentType());
        assertNotNull(response.getRag());
        assertNull(response.getTool());
        verifyNoInteractions(toolCallingChatService);
    }

    @Test
    void shouldPreserveToolFailureReasonInPrompt() {
        when(toolCallingChatService.chat(any(), any())).thenReturn(ToolCallingChatResponse.builder()
                .runId("run-tool-failed")
                .selectedTool("mdm.getMaterial")
                .execution(ToolCallingExecutionView.builder()
                        .success(false)
                        .toolName("mdm.getMaterial")
                        .errorCode("404")
                        .errorMessage("Material not found")
                        .latencyMs(3)
                        .build())
                .build());

        AgentChatRequest request = new AgentChatRequest();
        request.setRunId("run-tool-failed");
        request.setMessage("帮我查物料 MAT-404");

        service.chat(request, context);

        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(agentChatService).chat(captor.capture(), any());
        assertTrue(captor.getValue().getMessage().contains("Material not found"));
    }

    private RagRetrieveResponse ragResponse() {
        RagRetrieveResponse response = new RagRetrieveResponse();
        response.setKnowledgeBaseId("kb-scm");
        response.setRetrievedCount(1);
        response.setChunks(List.of(RagRetrievedChunk.builder()
                .documentId("doc-stock-rule")
                .chunkId("chunk-1")
                .title("库存口径")
                .source("docs/examples/scm-wms-rules.md")
                .content("库存可用数量=现存数量-锁定数量。".repeat(20))
                .score(0.92)
                .build()));
        return response;
    }

    private ToolCallingChatResponse toolResponse() {
        return ToolCallingChatResponse.builder()
                .runId("run-agent-1")
                .selectedTool("mdm.getMaterial")
                .toolArguments(Map.of("materialCode", "MAT-001"))
                .execution(ToolCallingExecutionView.builder()
                        .success(true)
                        .toolName("mdm.getMaterial")
                        .data(new ToolCallingDisplayData("物料信息", "已查询到物料 MAT-001", List.of(), List.of(),
                                Map.of("id", 1001L)))
                        .latencyMs(8)
                        .build())
                .build();
    }

    private ToolOrchestrationRun orchestrationRun() {
        return ToolOrchestrationRun.builder()
                .runId("run-agent-1")
                .steps(List.of(
                        ToolOrchestrationStep.builder()
                                .stepNo(1)
                                .stepRef("step-1")
                                .toolName("mdm.getMaterial")
                                .status(ToolOrchestrationStepStatus.SUCCESS)
                                .executed(true)
                                .execution(ToolOrchestrationExecutionSummary.builder()
                                        .success(true)
                                        .toolName("mdm.getMaterial")
                                        .displayTitle("物料信息")
                                        .displaySummary("已查询到物料 MAT-001")
                                        .safeFields(Map.of("materialId", 1001L))
                                        .build())
                                .build(),
                        ToolOrchestrationStep.builder()
                                .stepNo(2)
                                .stepRef("step-2")
                                .toolName("inventory.getBalance")
                                .status(ToolOrchestrationStepStatus.SUCCESS)
                                .executed(true)
                                .inputResolved(true)
                                .execution(ToolOrchestrationExecutionSummary.builder()
                                        .success(true)
                                        .toolName("inventory.getBalance")
                                        .displayTitle("库存余额")
                                        .displaySummary("已查询到库存余额")
                                        .safeFields(Map.of("availableQty", 128, "lockedQty", 12))
                                        .build())
                                .build()))
                .build();
    }
}
