package com.example.scm.aiagent.toolcalling;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.tool.dto.ToolResponse;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatRequest;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatResponse;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecuteResponse;
import com.example.scm.aiagent.toolcalling.model.ToolCallingPlan;
import com.example.scm.aiagent.toolcalling.service.MockToolPlanner;
import com.example.scm.aiagent.toolcalling.service.SpringAiToolCallingService;
import com.example.scm.aiagent.toolcalling.service.SpringAiToolPlanner;
import com.example.scm.aiagent.toolcalling.service.ToolCallingAnswerBuilder;
import com.example.scm.aiagent.toolcalling.service.ToolCallingChatService;
import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ToolCallingChatServiceTest {

    private SpringAiToolPlanner springAiToolPlanner;
    private SpringAiToolCallingService springAiToolCallingService;
    private ToolCallingChatService service;
    private AgentRequestContext context;

    @BeforeEach
    void setUp() {
        springAiToolPlanner = mock(SpringAiToolPlanner.class);
        springAiToolCallingService = mock(SpringAiToolCallingService.class);
        AiAgentProperties properties = new AiAgentProperties();
        properties.getToolCalling().setPlannerMode("spring-ai");
        properties.getToolCalling().getSpringAiPlanner().setEnabled(true);
        properties.getToolCalling().getSpringAiPlanner().setFallbackToMock(true);
        service = new ToolCallingChatService(
                properties,
                new MockToolPlanner(),
                springAiToolPlanner,
                springAiToolCallingService,
                new ToolCallingAnswerBuilder());
        context = new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));
    }

    @Test
    void shouldUseRequestedToolFirst() {
        when(springAiToolCallingService.execute(any(), eq(context))).thenReturn(ToolCallingExecuteResponse.builder()
                .success(true)
                .toolName("mdm.getMaterial")
                .arguments(Map.of("materialCode", "MAT-001"))
                .toolResponse(ToolResponse.builder()
                        .success(true)
                        .toolName("mdm.getMaterial")
                        .runId("run-chat-1")
                        .data(Map.of(
                                "materialCode", "MAT-001",
                                "materialName", "标准零件",
                                "status", "ENABLED"))
                        .latencyMs(5)
                        .build())
                .latencyMs(8)
                .build());

        ToolCallingChatRequest request = new ToolCallingChatRequest();
        request.setMessage("帮我查库存");
        request.setRequestedTool("mdm.getMaterial");
        request.setToolArguments(Map.of("materialCode", "MAT-001"));
        request.setRunId("run-chat-1");
        request.setPlannerMode("spring-ai");

        ToolCallingChatResponse response = service.chat(request, context);

        assertEquals("spring-ai", response.getPlannerMode());
        assertEquals("requested", response.getPlanningSource());
        assertFalse(response.isFallbackUsed());
        assertEquals("mdm.getMaterial", response.getSelectedTool());
        assertEquals("MAT-001", response.getToolArguments().get("materialCode"));
        assertTrue(response.getExecution().isSuccess());
        assertEquals("mdm.getMaterial", response.getExecution().getToolName());
        assertTrue(response.getAnswer().contains("标准零件"));
        verify(springAiToolCallingService).execute(any(), eq(context));
        verifyNoInteractions(springAiToolPlanner);
    }

    @Test
    void shouldUseSpringAiPlannerWhenRequestedToolMissing() {
        when(springAiToolPlanner.plan(any(), eq(context), eq("run-chat-2"))).thenReturn(ToolCallingPlan.builder()
                .plannerMode("spring-ai")
                .planningSource("spring-ai")
                .fallbackUsed(false)
                .selectedTool("sales.getOrder")
                .toolArguments(Map.of("orderNo", "SO-20260520-001"))
                .reason("model_plan")
                .build());
        when(springAiToolCallingService.execute(any(), eq(context))).thenReturn(ToolCallingExecuteResponse.builder()
                .success(true)
                .toolName("sales.getOrder")
                .arguments(Map.of("orderNo", "SO-20260520-001"))
                .toolResponse(ToolResponse.builder()
                        .success(true)
                        .toolName("sales.getOrder")
                        .runId("run-chat-2")
                        .data(Map.of(
                                "orderNo", "SO-20260520-001",
                                "status", "ALLOCATED",
                                "customerName", "测试客户",
                                "items", List.of(Map.of("materialId", 1001L, "qty", 10))))
                        .latencyMs(5)
                        .build())
                .latencyMs(7)
                .build());

        ToolCallingChatRequest request = new ToolCallingChatRequest();
        request.setMessage("帮我查一下销售订单");
        request.setPlannerMode("spring-ai");
        request.setRunId("run-chat-2");

        ToolCallingChatResponse response = service.chat(request, context);

        assertEquals("spring-ai", response.getPlannerMode());
        assertEquals("spring-ai", response.getPlanningSource());
        assertEquals("sales.getOrder", response.getSelectedTool());
        assertFalse(response.isFallbackUsed());
        assertEquals("model_plan", response.getPlanningReason());
        assertTrue(response.getAnswer().contains("销售订单"));
        assertTrue(response.getAnswer().contains("测试客户"));
    }

    @Test
    void shouldFallbackToMockWhenSpringAiPlannerFails() {
        when(springAiToolPlanner.plan(any(), eq(context), eq("run-chat-3"))).thenThrow(
                new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "planner failed"));
        when(springAiToolCallingService.execute(any(), eq(context))).thenReturn(ToolCallingExecuteResponse.builder()
                .success(true)
                .toolName("inventory.getBalance")
                .arguments(Map.of("materialId", 1001L, "warehouseId", 1L))
                .toolResponse(ToolResponse.builder()
                        .success(true)
                        .toolName("inventory.getBalance")
                        .runId("run-chat-3")
                        .data(Map.of(
                                "materialId", 1001L,
                                "warehouseId", 1L,
                                "availableQty", 128,
                                "lockedQty", 12,
                                "unit", "PCS"))
                        .latencyMs(5)
                        .build())
                .latencyMs(6)
                .build());

        ToolCallingChatRequest request = new ToolCallingChatRequest();
        request.setMessage("帮我查库存");
        request.setPlannerMode("spring-ai");
        request.setRunId("run-chat-3");

        ToolCallingChatResponse response = service.chat(request, context);

        assertEquals("mock-fallback", response.getPlanningSource());
        assertTrue(response.isFallbackUsed());
        assertEquals("inventory.getBalance", response.getSelectedTool());
        assertTrue(response.getAnswer().contains("库存余额"));
    }

    @Test
    void shouldKeepFailureReasonInAnswer() {
        when(springAiToolCallingService.execute(any(), eq(context))).thenReturn(ToolCallingExecuteResponse.builder()
                .success(false)
                .toolName("mdm.getMaterial")
                .arguments(Map.of("materialCode", "MAT-404"))
                .toolResponse(ToolResponse.builder()
                        .success(false)
                        .toolName("mdm.getMaterial")
                        .runId("run-chat-4")
                        .errorCode("404")
                        .errorMessage("MDM service failed: Material not found")
                        .latencyMs(9)
                        .build())
                .latencyMs(10)
                .build());

        ToolCallingChatRequest request = new ToolCallingChatRequest();
        request.setRequestedTool("mdm.getMaterial");
        request.setToolArguments(Map.of("materialCode", "MAT-404"));
        request.setRunId("run-chat-4");
        request.setPlannerMode("spring-ai");

        ToolCallingChatResponse response = service.chat(request, context);

        assertFalse(response.getExecution().isSuccess());
        assertEquals("404", response.getExecution().getErrorCode());
        assertTrue(response.getAnswer().contains("Material not found"));
    }
}
