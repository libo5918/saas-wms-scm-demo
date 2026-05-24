package com.example.scm.aiagent.mcp;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.mcp.dto.McpJsonRpcRequest;
import com.example.scm.aiagent.mcp.dto.McpServerToolCallResult;
import com.example.scm.aiagent.mcp.dto.McpServerToolsListResult;
import com.example.scm.aiagent.mcp.dto.McpToolDisplayView;
import com.example.scm.aiagent.mcp.dto.McpToolInvokeResponse;
import com.example.scm.aiagent.mcp.dto.McpToolListResponse;
import com.example.scm.aiagent.mcp.dto.McpToolView;
import com.example.scm.aiagent.mcp.service.McpServerTransportService;
import com.example.scm.aiagent.mcp.service.McpToolExposureService;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.toolcalling.model.SpringAiToolInputSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class McpServerTransportServiceTest {

    private AiAgentProperties properties;
    private McpToolExposureService exposureService;
    private McpServerTransportService service;
    private AgentRequestContext context;

    @BeforeEach
    void setUp() {
        properties = new AiAgentProperties();
        exposureService = mock(McpToolExposureService.class);
        service = new McpServerTransportService(properties, exposureService);
        context = new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));
    }

    @Test
    void shouldReturnDisabledErrorWhenServerDisabled() {
        McpJsonRpcRequest request = request("1", "tools/list", Map.of());

        var response = service.handle(request, context);

        assertNotNull(response.getError());
        assertEquals(-32000, response.getError().getCode());
    }

    @Test
    void shouldMapToolsListWhenEnabled() {
        properties.getMcp().getServer().setEnabled(true);
        when(exposureService.listTools(any())).thenReturn(McpToolListResponse.builder()
                .tenantId(1L)
                .toolCount(2)
                .tools(List.of(tool("mdm.getMaterial"), tool("inventory.getBalance")))
                .build());

        var response = service.handle(request("2", "tools/list", Map.of()), context);

        assertNull(response.getError());
        McpServerToolsListResult result = (McpServerToolsListResult) response.getResult();
        assertEquals(2, result.getTools().size());
        assertTrue(result.getTools().stream().anyMatch(tool -> "mdm.getMaterial".equals(tool.getName())));
        assertTrue(result.getTools().stream().allMatch(tool -> tool.getAnnotations() != null));
        assertFalse(result.getTools().toString().toLowerCase().contains("apikey"));
        assertFalse(result.getTools().toString().toLowerCase().contains("authorization"));
    }

    @Test
    void shouldMapToolsCallToExposureService() {
        properties.getMcp().getServer().setEnabled(true);
        when(exposureService.invoke(eq("mdm.getMaterial"), any(), any())).thenReturn(McpToolInvokeResponse.builder()
                .runId("run-mcp-server-1")
                .toolName("mdm.getMaterial")
                .success(true)
                .display(McpToolDisplayView.builder()
                        .displayTitle("物料信息")
                        .displaySummary("已查询到物料 MAT-001")
                        .build())
                .latencyMs(8)
                .build());

        var response = service.handle(request("3", "tools/call", Map.of(
                "name", "mdm.getMaterial",
                "runId", "run-mcp-server-1",
                "arguments", Map.of("materialCode", "MAT-001"))), context);

        assertNull(response.getError());
        McpServerToolCallResult result = (McpServerToolCallResult) response.getResult();
        assertFalse(result.isError());
        assertEquals("已查询到物料 MAT-001", result.getContent().get(0).getText());
        assertEquals("物料信息", result.getStructuredContent().get("displayTitle"));
        assertFalse(result.getStructuredContent().containsKey("rawData"));
        verify(exposureService).invoke(eq("mdm.getMaterial"), any(), any());
    }

    @Test
    void shouldMapToolFailureToJsonRpcError() {
        properties.getMcp().getServer().setEnabled(true);
        when(exposureService.invoke(eq("unknown.tool"), any(), any())).thenReturn(McpToolInvokeResponse.builder()
                .runId("run-failed")
                .toolName("unknown.tool")
                .success(false)
                .errorCode("404")
                .errorMessage("Tool not found: unknown.tool")
                .latencyMs(1)
                .build());

        var response = service.handle(request("4", "tools/call", Map.of(
                "name", "unknown.tool",
                "runId", "run-failed")), context);

        assertNotNull(response.getError());
        assertEquals(-32010, response.getError().getCode());
        assertEquals("Tool not found: unknown.tool", response.getError().getMessage());
        assertFalse(response.getError().getData().toString().contains("rawData"));
    }

    private McpJsonRpcRequest request(String id, String method, Map<String, Object> params) {
        McpJsonRpcRequest request = new McpJsonRpcRequest();
        request.setId(id);
        request.setMethod(method);
        request.setParams(params);
        return request;
    }

    private McpToolView tool(String name) {
        return McpToolView.builder()
                .name(name)
                .description("查询工具")
                .inputSchema(SpringAiToolInputSchema.builder().type("object").build())
                .domain(name.startsWith("mdm") ? "mdm" : "inventory")
                .category("query")
                .routeTags(List.of("read", "query"))
                .readOnly(true)
                .requiredPermissions(List.of("ai.tool.read"))
                .build();
    }
}
