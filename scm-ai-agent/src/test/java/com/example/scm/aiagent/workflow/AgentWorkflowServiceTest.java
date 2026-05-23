package com.example.scm.aiagent.workflow;

import com.example.scm.aiagent.dto.ChatResponse;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.service.AgentChatService;
import com.example.scm.aiagent.tool.dto.ToolResponse;
import com.example.scm.aiagent.tool.service.ToolInvocationService;
import com.example.scm.aiagent.toolcalling.display.ToolCallingDisplaySchemaBuilder;
import com.example.scm.aiagent.workflow.dto.AgentWorkflowRunRequest;
import com.example.scm.aiagent.workflow.dto.AgentWorkflowRunResponse;
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
import static org.mockito.Mockito.when;

class AgentWorkflowServiceTest {

    private ToolInvocationService toolInvocationService;
    private AgentChatService agentChatService;
    private AgentWorkflowService workflowService;
    private AgentRequestContext context;

    @BeforeEach
    void setUp() {
        toolInvocationService = mock(ToolInvocationService.class);
        agentChatService = mock(AgentChatService.class);
        workflowService = new AgentWorkflowService(
                new AgentWorkflowDefinitionRegistry(),
                new AgentWorkflowRunStore(),
                new AgentWorkflowParameterResolver(),
                new AgentWorkflowViewMapper(),
                toolInvocationService,
                new ToolCallingDisplaySchemaBuilder(),
                agentChatService);
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
}
