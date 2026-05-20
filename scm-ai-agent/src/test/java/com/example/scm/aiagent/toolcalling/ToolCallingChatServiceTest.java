package com.example.scm.aiagent.toolcalling;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatRequest;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatResponse;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecuteResponse;
import com.example.scm.aiagent.toolcalling.model.ToolCallingPlan;
import com.example.scm.aiagent.toolcalling.service.MockToolPlanner;
import com.example.scm.aiagent.toolcalling.service.SpringAiToolCallingService;
import com.example.scm.aiagent.toolcalling.service.SpringAiToolPlanner;
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
        service = new ToolCallingChatService(properties, new MockToolPlanner(), springAiToolPlanner, springAiToolCallingService);
        context = new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));
    }

    @Test
    void shouldUseRequestedToolFirst() {
        when(springAiToolCallingService.execute(any(), eq(context))).thenReturn(ToolCallingExecuteResponse.builder()
                .success(true)
                .toolName("mdm.getMaterial")
                .arguments(Map.of("materialCode", "MAT-001"))
                .latencyMs(5)
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
                .latencyMs(5)
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
    }

    @Test
    void shouldFallbackToMockWhenSpringAiPlannerFails() {
        when(springAiToolPlanner.plan(any(), eq(context), eq("run-chat-3"))).thenThrow(
                new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "planner failed"));
        when(springAiToolCallingService.execute(any(), eq(context))).thenReturn(ToolCallingExecuteResponse.builder()
                .success(true)
                .toolName("inventory.getBalance")
                .arguments(Map.of("materialId", 1001L, "warehouseId", 1L))
                .latencyMs(5)
                .build());

        ToolCallingChatRequest request = new ToolCallingChatRequest();
        request.setMessage("帮我查库存");
        request.setPlannerMode("spring-ai");
        request.setRunId("run-chat-3");

        ToolCallingChatResponse response = service.chat(request, context);

        assertEquals("mock-fallback", response.getPlanningSource());
        assertTrue(response.isFallbackUsed());
        assertEquals("inventory.getBalance", response.getSelectedTool());
    }
}
