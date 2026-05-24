package com.example.scm.aiagent.mcp;

import com.example.scm.aiagent.mcp.dto.McpToolInvokeRequest;
import com.example.scm.aiagent.mcp.service.McpToolExposureService;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.tool.dto.ToolResponse;
import com.example.scm.aiagent.tool.model.ToolDefinition;
import com.example.scm.aiagent.tool.service.ToolInvocationService;
import com.example.scm.aiagent.tool.service.ToolRegistry;
import com.example.scm.aiagent.toolcalling.display.ToolCallingDisplaySchemaBuilder;
import com.example.scm.aiagent.toolcalling.schema.ToolSchemaConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class McpToolExposureServiceTest {

    private ToolRegistry toolRegistry;
    private ToolInvocationService toolInvocationService;
    private McpToolExposureService service;
    private AgentRequestContext context;

    @BeforeEach
    void setUp() {
        toolRegistry = mock(ToolRegistry.class);
        toolInvocationService = mock(ToolInvocationService.class);
        service = new McpToolExposureService(
                toolRegistry,
                toolInvocationService,
                new ToolSchemaConverter(),
                new ToolCallingDisplaySchemaBuilder());
        context = new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));
    }

    @Test
    void shouldListOnlyMcpExposedReadOnlyTools() {
        when(toolRegistry.listDefinitions()).thenReturn(List.of(
                materialDefinition(),
                inventoryDefinition(),
                salesDefinition()));

        var response = service.listTools(context);

        assertEquals(2, response.getToolCount());
        assertTrue(response.getTools().stream().anyMatch(tool -> "mdm.getMaterial".equals(tool.getName())));
        assertTrue(response.getTools().stream().anyMatch(tool -> "inventory.getBalance".equals(tool.getName())));
        assertFalse(response.getTools().stream().anyMatch(tool -> "sales.getOrder".equals(tool.getName())));
        assertTrue(response.getTools().stream().allMatch(tool -> tool.getInputSchema() != null));
        assertTrue(response.getTools().stream().allMatch(tool -> tool.getDisplaySchema() != null));
    }

    @Test
    void shouldInvokeMcpExposedToolThroughToolInvocationService() {
        when(toolRegistry.findDefinition("mdm.getMaterial")).thenReturn(Optional.of(materialDefinition()));
        when(toolInvocationService.invoke(any(), any())).thenReturn(ToolResponse.builder()
                .success(true)
                .toolName("mdm.getMaterial")
                .runId("run-mcp-1")
                .data(Map.of("id", 1L, "materialCode", "MAT-001", "materialName", "螺丝", "unit", "PCS"))
                .latencyMs(12)
                .build());
        McpToolInvokeRequest request = new McpToolInvokeRequest();
        request.setRunId("run-mcp-1");
        request.setArguments(Map.of("materialCode", "MAT-001"));

        var response = service.invoke("mdm.getMaterial", request, context);

        assertTrue(response.isSuccess());
        assertEquals("mdm.getMaterial", response.getToolName());
        assertNotNull(response.getDisplay());
        assertEquals("物料信息", response.getDisplay().getDisplayTitle());
        assertFalse(response.getDisplay().getDisplayFields().isEmpty());
        ArgumentCaptor<com.example.scm.aiagent.tool.dto.ToolInvokeRequest> captor =
                ArgumentCaptor.forClass(com.example.scm.aiagent.tool.dto.ToolInvokeRequest.class);
        verify(toolInvocationService).invoke(captor.capture(), any());
        assertEquals("mdm.getMaterial", captor.getValue().getToolName());
        assertEquals("MAT-001", captor.getValue().getParameters().get("materialCode"));
    }

    @Test
    void shouldRejectNonExposedToolBeforeInvocation() {
        when(toolRegistry.findDefinition("sales.getOrder")).thenReturn(Optional.of(salesDefinition()));
        McpToolInvokeRequest request = new McpToolInvokeRequest();
        request.setRunId("run-mcp-denied");

        var response = service.invoke("sales.getOrder", request, context);

        assertFalse(response.isSuccess());
        assertEquals("403", response.getErrorCode());
        verify(toolInvocationService, never()).invoke(any(), any());
    }

    @Test
    void shouldKeepToolInvocationFailureSemantics() {
        when(toolRegistry.findDefinition("inventory.getBalance")).thenReturn(Optional.of(inventoryDefinition()));
        when(toolInvocationService.invoke(any(), any())).thenReturn(ToolResponse.builder()
                .success(false)
                .toolName("inventory.getBalance")
                .runId("run-mcp-failed")
                .errorCode("403")
                .errorMessage("Tool permission denied")
                .latencyMs(2)
                .build());
        McpToolInvokeRequest request = new McpToolInvokeRequest();
        request.setRunId("run-mcp-failed");

        var response = service.invoke("inventory.getBalance", request, context);

        assertFalse(response.isSuccess());
        assertEquals("403", response.getErrorCode());
        assertEquals("Tool permission denied", response.getErrorMessage());
        verify(toolInvocationService).invoke(any(), any());
    }

    private ToolDefinition materialDefinition() {
        return ToolDefinition.builder()
                .name("mdm.getMaterial")
                .domain("mdm")
                .category("query")
                .description("查询物料")
                .readOnly(true)
                .routeTags(List.of("mdm", "read", "query", "material"))
                .requiredPermissions(List.of("ai.tool.read", "ai.tool.mdm.read"))
                .parameters(Map.of("materialCode", "物料编码"))
                .requiredParameters(List.of("materialCode"))
                .build();
    }

    private ToolDefinition inventoryDefinition() {
        return ToolDefinition.builder()
                .name("inventory.getBalance")
                .domain("inventory")
                .category("query")
                .description("查询库存")
                .readOnly(true)
                .routeTags(List.of("inventory", "read", "query"))
                .requiredPermissions(List.of("ai.tool.read", "ai.tool.inventory.read"))
                .parameters(Map.of("materialId", "物料ID", "warehouseId", "仓库ID", "locationId", "库位ID"))
                .requiredParameters(List.of("materialId"))
                .build();
    }

    private ToolDefinition salesDefinition() {
        return ToolDefinition.builder()
                .name("sales.getOrder")
                .domain("sales")
                .category("query")
                .description("查询销售订单")
                .readOnly(true)
                .routeTags(List.of("sales", "read", "query"))
                .parameters(Map.of("orderNo", "订单号"))
                .build();
    }
}
