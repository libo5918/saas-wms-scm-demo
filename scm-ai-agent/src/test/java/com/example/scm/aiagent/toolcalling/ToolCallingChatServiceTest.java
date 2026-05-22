package com.example.scm.aiagent.toolcalling;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.tool.dto.ToolResponse;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatRequest;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatResponse;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecuteResponse;
import com.example.scm.aiagent.toolcalling.model.ToolCallingAnswerSummaryResult;
import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayData;
import com.example.scm.aiagent.toolcalling.model.ToolCallingPlan;
import com.example.scm.aiagent.toolcalling.planning.MockToolPlanner;
import com.example.scm.aiagent.toolcalling.application.SpringAiToolCallingService;
import com.example.scm.aiagent.toolcalling.planning.SpringAiToolPlanner;
import com.example.scm.aiagent.toolcalling.answer.ToolCallingAnswerSummaryService;
import com.example.scm.aiagent.toolcalling.application.ToolCallingChatService;
import com.example.scm.aiagent.toolcalling.display.ToolCallingDisplaySchemaBuilder;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolCallingOrchestratorService;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationRunStore;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationStepSummaryBuilder;
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
    private ToolCallingAnswerSummaryService answerSummaryService;
    private ToolCallingChatService service;
    private AgentRequestContext context;

    @BeforeEach
    void setUp() {
        springAiToolPlanner = mock(SpringAiToolPlanner.class);
        springAiToolCallingService = mock(SpringAiToolCallingService.class);
        answerSummaryService = mock(ToolCallingAnswerSummaryService.class);

        AiAgentProperties properties = new AiAgentProperties();
        properties.getToolCalling().setPlannerMode("spring-ai");
        properties.getToolCalling().setAnswerMode("spring-ai");
        properties.getToolCalling().getSpringAiPlanner().setEnabled(true);
        properties.getToolCalling().getSpringAiPlanner().setFallbackToMock(true);

        service = new ToolCallingChatService(
                properties,
                new MockToolPlanner(),
                springAiToolPlanner,
                springAiToolCallingService,
                answerSummaryService,
                new ToolCallingDisplaySchemaBuilder(),
                new ToolCallingOrchestratorService(properties, new ToolOrchestrationRunStore(properties),
                        new ToolOrchestrationStepSummaryBuilder())
        );
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
                        .data(Map.of("materialCode", "MAT-001"))
                        .latencyMs(5)
                        .build())
                .latencyMs(8)
                .build());
        when(answerSummaryService.summarize(any(), eq(context), any(), any(), eq("run-chat-1")))
                .thenReturn(ToolCallingAnswerSummaryResult.builder()
                        .answer("已查询到物料 MAT-001。")
                        .answerMode("spring-ai")
                        .fallbackUsed(false)
                        .build());

        ToolCallingChatRequest request = new ToolCallingChatRequest();
        request.setMessage("帮我查物料");
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
        ToolCallingDisplayData displayData = (ToolCallingDisplayData) response.getExecution().getData();
        assertEquals("物料信息", displayData.displayTitle());
        assertEquals("MAT-001", ((Map<?, ?>) displayData.rawData()).get("materialCode"));
        assertEquals("已查询到物料 MAT-001。", response.getAnswer());
        verify(springAiToolCallingService).execute(any(), eq(context));
        verify(answerSummaryService).summarize(any(), eq(context), any(), any(), eq("run-chat-1"));
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
                        .data(Map.of("orderNo", "SO-20260520-001", "status", "ALLOCATED"))
                        .latencyMs(5)
                        .build())
                .latencyMs(7)
                .build());
        when(answerSummaryService.summarize(any(), eq(context), any(), any(), eq("run-chat-2")))
                .thenReturn(ToolCallingAnswerSummaryResult.builder()
                        .answer("销售订单 SO-20260520-001 当前状态为 ALLOCATED。")
                        .answerMode("spring-ai")
                        .fallbackUsed(false)
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
        assertTrue(response.getExecution().isSuccess());
        ToolCallingDisplayData displayData = (ToolCallingDisplayData) response.getExecution().getData();
        assertEquals("销售订单", displayData.displayTitle());
        assertEquals("SO-20260520-001", ((Map<?, ?>) displayData.rawData()).get("orderNo"));
        assertEquals("销售订单 SO-20260520-001 当前状态为 ALLOCATED。", response.getAnswer());
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
                        .data(Map.of("materialId", 1001L, "warehouseId", 1L, "availableQty", 128))
                        .latencyMs(5)
                        .build())
                .latencyMs(6)
                .build());
        when(answerSummaryService.summarize(any(), eq(context), any(), any(), eq("run-chat-3")))
                .thenReturn(ToolCallingAnswerSummaryResult.builder()
                        .answer("库存可用数量为 128。")
                        .answerMode("template")
                        .fallbackUsed(true)
                        .build());

        ToolCallingChatRequest request = new ToolCallingChatRequest();
        request.setMessage("帮我查库存");
        request.setPlannerMode("spring-ai");
        request.setRunId("run-chat-3");

        ToolCallingChatResponse response = service.chat(request, context);

        assertEquals("mock-fallback", response.getPlanningSource());
        assertTrue(response.isFallbackUsed());
        assertEquals("inventory.getBalance", response.getSelectedTool());
        ToolCallingDisplayData displayData = (ToolCallingDisplayData) response.getExecution().getData();
        assertEquals("库存余额", displayData.displayTitle());
        assertEquals(128, ((Map<?, ?>) displayData.rawData()).get("availableQty"));
        assertEquals("库存可用数量为 128。", response.getAnswer());
    }

    @Test
    void shouldKeepFailureExecutionStable() {
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
        when(answerSummaryService.summarize(any(), eq(context), any(), any(), eq("run-chat-4")))
                .thenReturn(ToolCallingAnswerSummaryResult.builder()
                        .answer("未查询到物料，原因是 MDM service failed: Material not found。")
                        .answerMode("spring-ai")
                        .fallbackUsed(false)
                        .build());

        ToolCallingChatRequest request = new ToolCallingChatRequest();
        request.setRequestedTool("mdm.getMaterial");
        request.setToolArguments(Map.of("materialCode", "MAT-404"));
        request.setRunId("run-chat-4");
        request.setPlannerMode("spring-ai");

        ToolCallingChatResponse response = service.chat(request, context);

        assertFalse(response.getExecution().isSuccess());
        assertEquals("404", response.getExecution().getErrorCode());
        assertEquals("mdm.getMaterial", response.getExecution().getToolName());
        assertEquals(null, response.getExecution().getData());
        assertTrue(response.getAnswer().contains("Material not found"));
    }
}
