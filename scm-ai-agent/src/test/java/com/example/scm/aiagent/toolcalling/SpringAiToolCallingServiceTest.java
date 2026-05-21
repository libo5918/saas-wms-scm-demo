package com.example.scm.aiagent.toolcalling;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.tool.dto.ToolResponse;
import com.example.scm.aiagent.tool.model.ToolDefinition;
import com.example.scm.aiagent.tool.service.ToolInvocationAuditService;
import com.example.scm.aiagent.tool.service.ToolInvocationService;
import com.example.scm.aiagent.tool.service.ToolRegistry;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecuteRequest;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecuteResponse;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingSchemaListResponse;
import com.example.scm.aiagent.toolcalling.application.SpringAiToolCallingService;
import com.example.scm.aiagent.toolcalling.schema.ToolSchemaConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpringAiToolCallingServiceTest {

    private ToolRegistry toolRegistry;
    private ToolInvocationService toolInvocationService;
    private ToolInvocationAuditService toolInvocationAuditService;
    private SpringAiToolCallingService service;
    private AgentRequestContext context;

    @BeforeEach
    void setUp() {
        toolRegistry = mock(ToolRegistry.class);
        toolInvocationService = mock(ToolInvocationService.class);
        toolInvocationAuditService = mock(ToolInvocationAuditService.class);
        AiAgentProperties properties = new AiAgentProperties();
        properties.getTools().setAdapterMode("mock");
        service = new SpringAiToolCallingService(toolRegistry, new ToolSchemaConverter(),
                toolInvocationService, toolInvocationAuditService, properties);
        context = new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));
    }

    @Test
    void shouldListSchemas() {
        when(toolRegistry.listDefinitions()).thenReturn(List.of(ToolDefinition.builder()
                .name("inventory.getBalance")
                .domain("inventory")
                .description("查询库存")
                .readOnly(true)
                .parameters(Map.of("materialId", "物料 ID"))
                .requiredParameters(List.of("materialId"))
                .oneOfRequiredGroups(List.of())
                .build()));

        ToolCallingSchemaListResponse response = service.listSchemas(context);

        assertEquals(1, response.getToolCount());
        assertEquals("inventory.getBalance", response.getTools().get(0).getToolName());
        assertEquals(List.of("materialId"), response.getTools().get(0).getInputSchema().getRequired());
    }

    @Test
    void shouldRejectMissingRequiredArguments() {
        when(toolRegistry.findDefinition("inventory.getBalance")).thenReturn(Optional.of(ToolDefinition.builder()
                .name("inventory.getBalance")
                .domain("inventory")
                .description("查询库存")
                .readOnly(true)
                .parameters(Map.of("materialId", "物料 ID"))
                .requiredParameters(List.of("materialId"))
                .oneOfRequiredGroups(List.of())
                .build()));

        ToolCallingExecuteRequest request = new ToolCallingExecuteRequest();
        request.setToolName("inventory.getBalance");
        request.setRunId("run-tool-calling-1");

        ToolCallingExecuteResponse response = service.execute(request, context);

        assertFalse(response.isSuccess());
        assertEquals("400", response.getToolResponse().getErrorCode());
        verify(toolInvocationService, never()).invoke(any(), any());
        verify(toolInvocationAuditService).record(eq(context), eq("run-tool-calling-1"),
                eq("inventory.getBalance"), eq("mock"), eq(false), eq("400"), any(Long.class));
    }

    @Test
    void shouldExecuteToolCallingByDelegatingToToolInvocationService() {
        when(toolRegistry.findDefinition("sales.getOrder")).thenReturn(Optional.of(ToolDefinition.builder()
                .name("sales.getOrder")
                .domain("sales")
                .description("查询销售订单")
                .readOnly(true)
                .parameters(Map.of("orderId", "订单 ID", "orderNo", "订单号"))
                .requiredParameters(List.of())
                .oneOfRequiredGroups(List.of(List.of("orderId", "orderNo")))
                .build()));
        when(toolInvocationService.invoke(any(), eq(context))).thenReturn(ToolResponse.builder()
                .success(true)
                .toolName("sales.getOrder")
                .runId("run-tool-calling-2")
                .data(Map.of("adapterMode", "mock"))
                .latencyMs(5)
                .build());

        ToolCallingExecuteRequest request = new ToolCallingExecuteRequest();
        request.setToolName("sales.getOrder");
        request.setRunId("run-tool-calling-2");
        request.setArguments(Map.of("orderNo", "SO-001"));

        ToolCallingExecuteResponse response = service.execute(request, context);

        assertTrue(response.isSuccess());
        assertEquals("sales.getOrder", response.getToolName());
        ArgumentCaptor<com.example.scm.aiagent.tool.dto.ToolInvokeRequest> captor =
                ArgumentCaptor.forClass(com.example.scm.aiagent.tool.dto.ToolInvokeRequest.class);
        verify(toolInvocationService).invoke(captor.capture(), eq(context));
        assertEquals("sales.getOrder", captor.getValue().getToolName());
        assertEquals("SO-001", captor.getValue().getParameters().get("orderNo"));
    }
}
