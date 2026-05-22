package com.example.scm.aiagent.tool;

import com.example.scm.aiagent.config.AiAgentSecurityConfig;
import com.example.scm.aiagent.tool.controller.AiToolController;
import com.example.scm.aiagent.tool.dto.ToolInvocationAuditListResponse;
import com.example.scm.aiagent.tool.dto.ToolListResponse;
import com.example.scm.aiagent.tool.dto.ToolResponse;
import com.example.scm.aiagent.tool.model.ToolDefinition;
import com.example.scm.aiagent.tool.model.ToolInvocationAuditRecord;
import com.example.scm.aiagent.tool.model.ToolRuntimeStatus;
import com.example.scm.aiagent.tool.service.ToolInvocationService;
import com.example.scm.aiagent.tool.service.ToolRuntimeProtectionService;
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
import java.time.Instant;

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

    @MockitoBean
    private ToolRuntimeProtectionService runtimeProtectionService;

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
    void shouldListToolInvocations() throws Exception {
        when(toolInvocationService.listInvocations(any(), any(), any(), any())).thenReturn(ToolInvocationAuditListResponse.builder()
                .tenantId(1L)
                .count(1)
                .records(List.of(ToolInvocationAuditRecord.builder()
                        .tenantId(1L)
                        .userId(10001L)
                        .runId("run-tools-1")
                        .toolName("inventory.getBalance")
                        .adapterMode("mock")
                        .success(true)
                        .latencyMs(5)
                        .createdAt(Instant.parse("2026-05-20T09:00:00Z"))
                        .build()))
                .build());

        mockMvc.perform(get("/api/v1/ai/tools/invocations")
                        .header(GatewayHeaders.TENANT_ID, "1")
                        .header(GatewayHeaders.USER_ID, "10001")
                        .header(GatewayHeaders.USERNAME, "admin")
                        .header(GatewayHeaders.USER_ROLES, "ROLE_ADMIN")
                        .param("toolName", "inventory.getBalance")
                        .param("runId", "run-tools-1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tenantId").value(1))
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.records[0].toolName").value("inventory.getBalance"));
    }

    @Test
    void shouldListRuntimeStatuses() throws Exception {
        when(runtimeProtectionService.listStatuses()).thenReturn(List.of(ToolRuntimeStatus.builder()
                .toolName("inventory.getBalance")
                .totalCalls(2)
                .successCount(1)
                .failureCount(1)
                .retryCount(1)
                .circuitState("CLOSED")
                .build()));

        mockMvc.perform(get("/api/v1/ai/tools/runtime/status")
                        .header(GatewayHeaders.TENANT_ID, "1")
                        .header(GatewayHeaders.USER_ID, "10001")
                        .header(GatewayHeaders.USERNAME, "admin")
                        .header(GatewayHeaders.USER_ROLES, "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].toolName").value("inventory.getBalance"))
                .andExpect(jsonPath("$.data[0].totalCalls").value(2))
                .andExpect(jsonPath("$.data[0].retryCount").value(1))
                .andExpect(jsonPath("$.data[0].circuitState").value("CLOSED"));
    }

    @Test
    void shouldGetRuntimeStatusByToolName() throws Exception {
        when(runtimeProtectionService.getStatus("inventory.getBalance")).thenReturn(ToolRuntimeStatus.builder()
                .toolName("inventory.getBalance")
                .totalCalls(3)
                .successCount(2)
                .failureCount(1)
                .lastErrorType("ToolClientException")
                .circuitState("OPEN")
                .build());

        mockMvc.perform(get("/api/v1/ai/tools/runtime/status/inventory.getBalance")
                        .header(GatewayHeaders.TENANT_ID, "1")
                        .header(GatewayHeaders.USER_ID, "10001")
                        .header(GatewayHeaders.USERNAME, "admin")
                        .header(GatewayHeaders.USER_ROLES, "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.toolName").value("inventory.getBalance"))
                .andExpect(jsonPath("$.data.totalCalls").value(3))
                .andExpect(jsonPath("$.data.lastErrorType").value("ToolClientException"))
                .andExpect(jsonPath("$.data.circuitState").value("OPEN"));
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
