package com.example.scm.aiagent.mcp;

import com.example.scm.aiagent.config.AiAgentSecurityConfig;
import com.example.scm.aiagent.mcp.controller.McpToolController;
import com.example.scm.aiagent.mcp.dto.McpToolDisplayView;
import com.example.scm.aiagent.mcp.dto.McpToolInvokeResponse;
import com.example.scm.aiagent.mcp.dto.McpToolListResponse;
import com.example.scm.aiagent.mcp.dto.McpToolView;
import com.example.scm.aiagent.mcp.service.McpToolExposureService;
import com.example.scm.aiagent.toolcalling.model.SpringAiToolInputSchema;
import com.example.scm.common.security.GatewayHeaders;
import com.example.scm.common.web.GlobalExceptionHandler;
import com.example.scm.common.web.TenantHeaderInterceptor;
import com.example.scm.common.web.WebMvcConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(McpToolController.class)
@Import({GlobalExceptionHandler.class, TenantHeaderInterceptor.class, WebMvcConfiguration.class, AiAgentSecurityConfig.class})
class McpToolControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private McpToolExposureService mcpToolExposureService;

    @Test
    void shouldListMcpTools() throws Exception {
        when(mcpToolExposureService.listTools(any())).thenReturn(McpToolListResponse.builder()
                .tenantId(1L)
                .toolCount(2)
                .tools(List.of(McpToolView.builder()
                        .name("mdm.getMaterial")
                        .description("查询物料")
                        .domain("mdm")
                        .category("query")
                        .routeTags(List.of("mdm", "read", "query"))
                        .readOnly(true)
                        .requiredPermissions(List.of("ai.tool.read"))
                        .inputSchema(SpringAiToolInputSchema.builder().type("object").build())
                        .displaySchema(Map.of("type", "display"))
                        .build()))
                .build());

        mockMvc.perform(get("/api/v1/ai/mcp/tools")
                        .header(GatewayHeaders.TENANT_ID, "1")
                        .header(GatewayHeaders.USER_ID, "10001")
                        .header(GatewayHeaders.USERNAME, "admin")
                        .header(GatewayHeaders.USER_ROLES, "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.toolCount").value(2))
                .andExpect(jsonPath("$.data.tools[0].name").value("mdm.getMaterial"))
                .andExpect(jsonPath("$.data.tools[0].readOnly").value(true))
                .andExpect(jsonPath("$.data.tools[0].inputSchema.type").value("object"));
    }

    @Test
    void shouldInvokeMcpTool() throws Exception {
        when(mcpToolExposureService.invoke(eq("mdm.getMaterial"), any(), any())).thenReturn(McpToolInvokeResponse.builder()
                .runId("run-mcp-1")
                .toolName("mdm.getMaterial")
                .success(true)
                .display(McpToolDisplayView.builder()
                        .displayTitle("物料信息")
                        .displaySummary("已查询到物料 MAT-001")
                        .build())
                .latencyMs(5)
                .build());

        mockMvc.perform(post("/api/v1/ai/mcp/tools/mdm.getMaterial/invoke")
                        .header(GatewayHeaders.TENANT_ID, "1")
                        .header(GatewayHeaders.USER_ID, "10001")
                        .header(GatewayHeaders.USERNAME, "admin")
                        .header(GatewayHeaders.USER_ROLES, "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "runId": "run-mcp-1",
                                  "arguments": {
                                    "materialCode": "MAT-001"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.toolName").value("mdm.getMaterial"))
                .andExpect(jsonPath("$.data.display.displayTitle").value("物料信息"))
                .andExpect(jsonPath("$.data.rawData").doesNotExist());
    }

    @Test
    void shouldRejectMissingUserContext() throws Exception {
        mockMvc.perform(get("/api/v1/ai/mcp/tools")
                        .header(GatewayHeaders.TENANT_ID, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));
    }
}
