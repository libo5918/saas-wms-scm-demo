package com.example.scm.aiagent.mcp;

import com.example.scm.aiagent.config.AiAgentSecurityConfig;
import com.example.scm.aiagent.mcp.controller.McpServerController;
import com.example.scm.aiagent.mcp.dto.McpJsonRpcResponse;
import com.example.scm.aiagent.mcp.dto.McpServerContent;
import com.example.scm.aiagent.mcp.dto.McpServerToolCallResult;
import com.example.scm.aiagent.mcp.service.McpServerTransportService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(McpServerController.class)
@Import({GlobalExceptionHandler.class, TenantHeaderInterceptor.class, WebMvcConfiguration.class, AiAgentSecurityConfig.class})
class McpServerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private McpServerTransportService mcpServerTransportService;

    @Test
    void shouldHandleJsonRpcToolCall() throws Exception {
        when(mcpServerTransportService.handle(any(), any())).thenReturn(McpJsonRpcResponse.success("1",
                McpServerToolCallResult.builder()
                        .content(List.of(McpServerContent.builder().text("已查询到物料 MAT-001").build()))
                        .structuredContent(Map.of(
                                "success", true,
                                "toolName", "mdm.getMaterial",
                                "displaySummary", "已查询到物料 MAT-001"))
                        .build()));

        mockMvc.perform(post("/api/v1/ai/mcp/server")
                        .header(GatewayHeaders.TENANT_ID, "1")
                        .header(GatewayHeaders.USER_ID, "10001")
                        .header(GatewayHeaders.USERNAME, "admin")
                        .header(GatewayHeaders.USER_ROLES, "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": "1",
                                  "method": "tools/call",
                                  "params": {
                                    "name": "mdm.getMaterial",
                                    "arguments": {
                                      "materialCode": "MAT-001"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.result.content[0].text").value("已查询到物料 MAT-001"))
                .andExpect(jsonPath("$.result.structuredContent.rawData").doesNotExist());
    }

    @Test
    void shouldRejectMissingUserContext() throws Exception {
        mockMvc.perform(post("/api/v1/ai/mcp/server")
                        .header(GatewayHeaders.TENANT_ID, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":"1","method":"tools/list","params":{}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));
    }

}
