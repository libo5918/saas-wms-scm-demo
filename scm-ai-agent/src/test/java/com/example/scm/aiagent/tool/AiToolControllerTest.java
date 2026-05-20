package com.example.scm.aiagent.tool;

import com.example.scm.aiagent.config.AiAgentSecurityConfig;
import com.example.scm.aiagent.tool.controller.AiToolController;
import com.example.scm.aiagent.tool.dto.ToolListResponse;
import com.example.scm.aiagent.tool.dto.ToolResponse;
import com.example.scm.aiagent.tool.model.ToolDefinition;
import com.example.scm.aiagent.tool.service.ToolInvocationService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiToolController.class)
@Import({GlobalExceptionHandler.class, TenantHeaderInterceptor.class, WebMvcConfiguration.class, AiAgentSecurityConfig.class})
class AiToolControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ToolInvocationService toolInvocationService;

    @Test
    void shouldListToolsWithTenantAndUserContext() throws Exception {
        when(toolInvocationService.listTools(any())).thenReturn(ToolListResponse.builder()
                .tenantId(1L)
                .toolCount(1)
                .tools(List.of(ToolDefinition.builder()
                        .name("inventory.getBalance")
                        .domain("inventory")
                        .description("查询库存余额")
                        .readOnly(true)
                        .parameters(Map.of("materialId", "物料 ID"))
                        .build()))
                .build());

        mockMvc.perform(get("/api/v1/ai/tools")
                        .header(GatewayHeaders.TENANT_ID, "1")
                        .header(GatewayHeaders.USER_ID, "10001")
                        .header(GatewayHeaders.USERNAME, "admin")
                        .header(GatewayHeaders.USER_ROLES, "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tenantId").value(1))
                .andExpect(jsonPath("$.data.toolCount").value(1))
                .andExpect(jsonPath("$.data.tools[0].name").value("inventory.getBalance"));
    }

    @Test
    void shouldInvokeToolWithTenantAndUserContext() throws Exception {
        when(toolInvocationService.invoke(any(), any())).thenReturn(ToolResponse.builder()
                .success(true)
                .toolName("inventory.getBalance")
                .runId("run-tools-1")
                .data(Map.of("tenantId", 1L, "availableQty", 128))
                .latencyMs(3)
                .build());

        mockMvc.perform(post("/api/v1/ai/tools/invoke")
                        .header(GatewayHeaders.TENANT_ID, "1")
                        .header(GatewayHeaders.USER_ID, "10001")
                        .header(GatewayHeaders.USERNAME, "admin")
                        .header(GatewayHeaders.USER_ROLES, "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toolName": "inventory.getBalance",
                                  "runId": "run-tools-1",
                                  "parameters": {
                                    "materialId": 1001,
                                    "warehouseId": 1
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.toolName").value("inventory.getBalance"))
                .andExpect(jsonPath("$.data.data.tenantId").value(1));
    }

    @Test
    void shouldRejectMissingUserContext() throws Exception {
        mockMvc.perform(post("/api/v1/ai/tools/invoke")
                        .header(GatewayHeaders.TENANT_ID, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toolName": "inventory.getBalance"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));
    }
}
