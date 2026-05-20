package com.example.scm.aiagent.toolcalling;

import com.example.scm.aiagent.config.AiAgentSecurityConfig;
import com.example.scm.aiagent.toolcalling.controller.AiToolCallingController;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatResponse;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecuteResponse;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingSchemaListResponse;
import com.example.scm.aiagent.toolcalling.model.SpringAiToolDescriptor;
import com.example.scm.aiagent.toolcalling.model.SpringAiToolInputSchema;
import com.example.scm.aiagent.toolcalling.model.SpringAiToolParameterSchema;
import com.example.scm.aiagent.toolcalling.service.SpringAiToolCallingService;
import com.example.scm.aiagent.toolcalling.service.ToolCallingChatService;
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

@WebMvcTest(AiToolCallingController.class)
@Import({GlobalExceptionHandler.class, TenantHeaderInterceptor.class, WebMvcConfiguration.class, AiAgentSecurityConfig.class})
class AiToolCallingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpringAiToolCallingService springAiToolCallingService;

    @MockitoBean
    private ToolCallingChatService toolCallingChatService;

    @Test
    void shouldListToolCallingSchemas() throws Exception {
        when(springAiToolCallingService.listSchemas(any())).thenReturn(ToolCallingSchemaListResponse.builder()
                .tenantId(1L)
                .toolCount(1)
                .tools(List.of(SpringAiToolDescriptor.builder()
                        .toolName("inventory.getBalance")
                        .description("查询库存")
                        .readOnly(true)
                        .inputSchema(SpringAiToolInputSchema.builder()
                                .type("object")
                                .properties(Map.of("materialId", SpringAiToolParameterSchema.builder()
                                        .type("integer")
                                        .description("物料 ID")
                                        .required(true)
                                        .build()))
                                .required(List.of("materialId"))
                                .oneOfRequiredGroups(List.of())
                                .build())
                        .build()))
                .build());

        mockMvc.perform(get("/api/v1/ai/tool-calling/schema")
                        .header(GatewayHeaders.TENANT_ID, "1")
                        .header(GatewayHeaders.USER_ID, "10001")
                        .header(GatewayHeaders.USERNAME, "admin")
                        .header(GatewayHeaders.USER_ROLES, "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.toolCount").value(1))
                .andExpect(jsonPath("$.data.tools[0].toolName").value("inventory.getBalance"));
    }

    @Test
    void shouldExecuteToolCalling() throws Exception {
        when(springAiToolCallingService.execute(any(), any())).thenReturn(ToolCallingExecuteResponse.builder()
                .success(true)
                .toolName("sales.getOrder")
                .arguments(Map.of("orderNo", "SO-001"))
                .toolResponse(com.example.scm.aiagent.tool.dto.ToolResponse.builder()
                        .success(true)
                        .toolName("sales.getOrder")
                        .runId("run-tool-calling-1")
                        .data(Map.of("adapterMode", "mock"))
                        .latencyMs(10)
                        .build())
                .latencyMs(12)
                .build());

        mockMvc.perform(post("/api/v1/ai/tool-calling/execute")
                        .header(GatewayHeaders.TENANT_ID, "1")
                        .header(GatewayHeaders.USER_ID, "10001")
                        .header(GatewayHeaders.USERNAME, "admin")
                        .header(GatewayHeaders.USER_ROLES, "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toolName": "sales.getOrder",
                                  "runId": "run-tool-calling-1",
                                  "arguments": {
                                    "orderNo": "SO-001"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.toolName").value("sales.getOrder"))
                .andExpect(jsonPath("$.data.toolResponse.toolName").value("sales.getOrder"));
    }

    @Test
    void shouldRejectMissingUserContext() throws Exception {
        mockMvc.perform(post("/api/v1/ai/tool-calling/execute")
                        .header(GatewayHeaders.TENANT_ID, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toolName": "sales.getOrder"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    void shouldExecuteToolCallingChat() throws Exception {
        when(toolCallingChatService.chat(any(), any())).thenReturn(ToolCallingChatResponse.builder()
                .runId("run-tool-chat-1")
                .plannerMode("spring-ai")
                .planningSource("spring-ai")
                .fallbackUsed(false)
                .selectedTool("sales.getOrder")
                .toolArguments(Map.of("orderNo", "SO-001"))
                .toolResponse(ToolCallingExecuteResponse.builder()
                        .success(true)
                        .toolName("sales.getOrder")
                        .arguments(Map.of("orderNo", "SO-001"))
                        .latencyMs(8)
                        .build())
                .answer("已根据你的问题调用工具 `sales.getOrder` 完成查询。")
                .latencyMs(12)
                .build());

        mockMvc.perform(post("/api/v1/ai/tool-calling/chat")
                        .header(GatewayHeaders.TENANT_ID, "1")
                        .header(GatewayHeaders.USER_ID, "10001")
                        .header(GatewayHeaders.USERNAME, "admin")
                        .header(GatewayHeaders.USER_ROLES, "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "帮我查一下销售订单",
                                  "runId": "run-tool-chat-1",
                                  "plannerMode": "spring-ai",
                                  "toolArguments": {
                                    "orderNo": "SO-001"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.runId").value("run-tool-chat-1"))
                .andExpect(jsonPath("$.data.plannerMode").value("spring-ai"))
                .andExpect(jsonPath("$.data.planningSource").value("spring-ai"))
                .andExpect(jsonPath("$.data.selectedTool").value("sales.getOrder"));
    }
}
