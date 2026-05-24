package com.example.scm.aiagent.workflow;

import com.example.scm.aiagent.dto.ChatResponse;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.rag.dto.RagRetrieveResponse;
import com.example.scm.aiagent.rag.dto.RagRetrievedChunk;
import com.example.scm.aiagent.rag.service.RagService;
import com.example.scm.aiagent.service.AgentChatService;
import com.example.scm.aiagent.tool.dto.ToolResponse;
import com.example.scm.aiagent.tool.service.ToolInvocationService;
import com.example.scm.aiagent.toolcalling.display.ToolCallingDisplaySchemaBuilder;
import com.example.scm.aiagent.workflow.dto.AgentWorkflowRunRequest;
import com.example.scm.aiagent.workflow.dto.AgentWorkflowRunResponse;
import com.example.scm.aiagent.workflow.engine.AgentWorkflowEngine;
import com.example.scm.aiagent.workflow.executor.AgentWorkflowStepExecutorRegistry;
import com.example.scm.aiagent.workflow.executor.SummaryWorkflowStepExecutor;
import com.example.scm.aiagent.workflow.executor.ToolWorkflowStepExecutor;
import com.example.scm.aiagent.workflow.service.AgentWorkflowDefinitionRegistry;
import com.example.scm.aiagent.workflow.service.AgentWorkflowParameterResolver;
import com.example.scm.aiagent.workflow.service.AgentWorkflowRunStore;
import com.example.scm.aiagent.workflow.service.AgentWorkflowService;
import com.example.scm.aiagent.workflow.service.AgentWorkflowViewMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AgentWorkflowServiceTest {

    private ToolInvocationService toolInvocationService;
    private AgentChatService agentChatService;
    private RagService ragService;
    private AgentWorkflowService workflowService;
    private AgentWorkflowRunStore runStore;
    private AgentRequestContext context;

    @BeforeEach
    void setUp() {
        toolInvocationService = mock(ToolInvocationService.class);
        agentChatService = mock(AgentChatService.class);
        ragService = mock(RagService.class);
        runStore = new AgentWorkflowRunStore();
        ToolWorkflowStepExecutor toolExecutor = new ToolWorkflowStepExecutor(
                new AgentWorkflowParameterResolver(),
                toolInvocationService,
                new ToolCallingDisplaySchemaBuilder());
        SummaryWorkflowStepExecutor summaryExecutor = new SummaryWorkflowStepExecutor(agentChatService, ragService);
        AgentWorkflowEngine workflowEngine = new AgentWorkflowEngine(
                runStore,
                new AgentWorkflowStepExecutorRegistry(List.of(toolExecutor, summaryExecutor)));
        workflowService = new AgentWorkflowService(
                new AgentWorkflowDefinitionRegistry(),
                runStore,
                new AgentWorkflowViewMapper(),
                workflowEngine);
        context = new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));
        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setAnswer("补货建议草案：当前库存可用数量为 12，请人工确认是否补货。");
        when(agentChatService.chat(any(), any())).thenReturn(chatResponse);
    }

    @Test
    void shouldListStockReplenishmentDefinitionWithThreeSteps() {
        var definitions = workflowService.listDefinitions();

        assertEquals(1, definitions.size());
        assertEquals("scm_stock_replenishment_advice", definitions.get(0).getWorkflowCode());
        assertEquals(3, definitions.get(0).getSteps().size());
    }

    @Test
    void shouldRunReadOnlyWorkflowSuccessfully() {
        when(toolInvocationService.invoke(any(), any()))
                .thenReturn(materialResponse())
                .thenReturn(inventoryResponse());

        AgentWorkflowRunResponse response = workflowService.run("scm_stock_replenishment_advice", request(), context);

        assertEquals("run-workflow-1", response.getRunId());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(3, response.getSteps().size());
        assertEquals("SUCCESS", response.getSteps().get(0).getStatus());
        assertEquals("SUCCESS", response.getSteps().get(1).getStatus());
        assertEquals("SUCCESS", response.getSteps().get(2).getStatus());
        assertEquals(1001L, response.getSteps().get(0).getSafeFields().get("materialId"));
        assertFalse(response.getSteps().get(0).getSafeFields().containsKey("rawData"));
        assertTrue(response.getFinalAnswer().contains("补货建议草案"));

        ArgumentCaptor<com.example.scm.aiagent.tool.dto.ToolInvokeRequest> captor =
                ArgumentCaptor.forClass(com.example.scm.aiagent.tool.dto.ToolInvokeRequest.class);
        verify(toolInvocationService, times(2)).invoke(captor.capture(), any());
        assertEquals("mdm.getMaterial", captor.getAllValues().get(0).getToolName());
        assertEquals("inventory.getBalance", captor.getAllValues().get(1).getToolName());
        assertEquals(1001L, captor.getAllValues().get(1).getParameters().get("materialId"));
        assertEquals(1L, captor.getAllValues().get(1).getParameters().get("warehouseId"));
        assertEquals(2L, captor.getAllValues().get(1).getParameters().get("locationId"));
        verifyNoInteractions(ragService);
    }

    @Test
    void shouldRetrieveRagWhenKnowledgeBaseConfigured() {
        when(toolInvocationService.invoke(any(), any()))
                .thenReturn(materialResponse())
                .thenReturn(inventoryResponse());
        when(ragService.retrieve(any(), any())).thenReturn(ragResponse(1));
        AgentWorkflowRunRequest request = request();
        request.setKnowledgeBaseId("kb-scm-demo");
        request.setTopK(3);
        request.setScoreThreshold(0.1);
        request.setFilters(Map.of("source", "rules"));

        AgentWorkflowRunResponse response = workflowService.run("scm_stock_replenishment_advice", request, context);

        assertEquals("SUCCESS", response.getStatus());
        Map<String, Object> summarySafeFields = response.getSteps().get(2).getSafeFields();
        assertTrue(summarySafeFields.containsKey("rag"));
        Map<?, ?> rag = (Map<?, ?>) summarySafeFields.get("rag");
        assertEquals("kb-scm-demo", rag.get("knowledgeBaseId"));
        assertEquals(1, rag.get("retrievedCount"));

        ArgumentCaptor<com.example.scm.aiagent.rag.dto.RagRetrieveRequest> captor =
                ArgumentCaptor.forClass(com.example.scm.aiagent.rag.dto.RagRetrieveRequest.class);
        verify(ragService).retrieve(captor.capture(), any());
        assertEquals("kb-scm-demo", captor.getValue().getKnowledgeBaseId());
        assertEquals(3, captor.getValue().getTopK());
        assertTrue(captor.getValue().getQuery().contains("MAT-001"));
        assertTrue(captor.getValue().getQuery().contains("库存"));
    }

    @Test
    void shouldKeepRagSummaryWhenNoChunksRetrieved() {
        when(toolInvocationService.invoke(any(), any()))
                .thenReturn(materialResponse())
                .thenReturn(inventoryResponse());
        when(ragService.retrieve(any(), any())).thenReturn(ragResponse(0));
        AgentWorkflowRunRequest request = request();
        request.setKnowledgeBaseId("kb-empty");

        AgentWorkflowRunResponse response = workflowService.run("scm_stock_replenishment_advice", request, context);

        Map<?, ?> rag = (Map<?, ?>) response.getSteps().get(2).getSafeFields().get("rag");
        assertEquals("kb-empty", rag.get("knowledgeBaseId"));
        assertEquals(0, rag.get("retrievedCount"));
    }

    @Test
    void shouldFailWhenInventoryParametersMissing() {
        when(toolInvocationService.invoke(any(), any())).thenReturn(materialResponse());

        AgentWorkflowRunRequest request = new AgentWorkflowRunRequest();
        request.setRunId("run-missing");
        request.setMessage("帮我生成物料 MAT-001 的补货建议");

        AgentWorkflowRunResponse response = workflowService.run("scm_stock_replenishment_advice", request, context);

        assertEquals("FAILED", response.getStatus());
        assertEquals("SKIPPED", response.getSteps().get(1).getStatus());
        assertTrue(response.getFinalAnswer().contains("缺少库存查询参数"));
        verify(toolInvocationService, times(1)).invoke(any(), any());
    }

    @Test
    void shouldMarkToolFailureAsFailedStep() {
        when(toolInvocationService.invoke(any(), any())).thenReturn(ToolResponse.builder()
                .success(false)
                .toolName("mdm.getMaterial")
                .runId("run-workflow-1")
                .errorCode("403")
                .errorMessage("Tool permission denied")
                .build());

        AgentWorkflowRunResponse response = workflowService.run("scm_stock_replenishment_advice", request(), context);

        assertEquals("FAILED", response.getStatus());
        assertEquals("FAILED", response.getSteps().get(0).getStatus());
        assertEquals("403", response.getSteps().get(0).getErrorCode());
        assertTrue(response.getFinalAnswer().contains("物料查询失败"));
    }

    private AgentWorkflowRunRequest request() {
        AgentWorkflowRunRequest request = new AgentWorkflowRunRequest();
        request.setRunId("run-workflow-1");
        request.setMessage("帮我生成物料 MAT-001 在仓库ID 1、库位ID 2 的补货建议草案");
        return request;
    }

    private ToolResponse materialResponse() {
        return ToolResponse.builder()
                .success(true)
                .toolName("mdm.getMaterial")
                .runId("run-workflow-1")
                .data(Map.of(
                        "id", 1001L,
                        "materialCode", "MAT-001",
                        "materialName", "螺丝",
                        "status", 1,
                        "category", "RAW",
                        "unit", "PCS"))
                .build();
    }

    private ToolResponse inventoryResponse() {
        return ToolResponse.builder()
                .success(true)
                .toolName("inventory.getBalance")
                .runId("run-workflow-1")
                .data(Map.of(
                        "materialId", 1001L,
                        "warehouseId", 1L,
                        "locationId", 2L,
                        "availableQty", 12,
                        "lockedQty", 3,
                        "unit", "PCS"))
                .build();
    }

    private RagRetrieveResponse ragResponse(int retrievedCount) {
        RagRetrieveResponse response = new RagRetrieveResponse();
        response.setKnowledgeBaseId(retrievedCount == 0 ? "kb-empty" : "kb-scm-demo");
        response.setRetrievedCount(retrievedCount);
        if (retrievedCount > 0) {
            response.setChunks(List.of(RagRetrievedChunk.builder()
                    .documentId("doc-rule")
                    .chunkId("chunk-1")
                    .title("库存规则")
                    .source("docs/examples/scm-wms-rules.md")
                    .content("库存可用数量通常等于现存数量减去锁定数量。".repeat(20))
                    .score(0.91)
                    .build()));
        }
        return response;
    }
}
