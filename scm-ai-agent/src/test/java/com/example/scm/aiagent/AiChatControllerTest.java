package com.example.scm.aiagent;

import com.example.scm.aiagent.controller.AiChatController;
import com.example.scm.aiagent.config.AiAgentSecurityConfig;
import com.example.scm.aiagent.dto.ChatResponse;
import com.example.scm.aiagent.service.AgentChatService;
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

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiChatController.class)
@Import({GlobalExceptionHandler.class, TenantHeaderInterceptor.class, WebMvcConfiguration.class, AiAgentSecurityConfig.class})
class AiChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AgentChatService agentChatService;

    @Test
    void shouldChatWithTenantAndUserContext() throws Exception {
        ChatResponse response = new ChatResponse();
        response.setRunId("run-1");
        response.setConversationId("conv-1");
        response.setAnswer("agent answer");
        response.setTenantId(1L);
        response.setUserId(10001L);
        response.setModelName("qwen-plus");
        response.setProviderModel("qwen-plus");
        response.setProvider("dashscope");
        response.setProviderType("dashscope");
        response.setProviderMode("mock");
        response.setTaskType("simple_chat");
        response.setRouteReason("default_model");
        response.setLatencyMs(12);
        response.setCreatedAt(Instant.now());
        when(agentChatService.chat(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/ai/chat")
                        .header(GatewayHeaders.TENANT_ID, "1")
                        .header(GatewayHeaders.USER_ID, "10001")
                        .header(GatewayHeaders.USERNAME, "admin")
                        .header(GatewayHeaders.USER_ROLES, "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "hello agent"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tenantId").value(1))
                .andExpect(jsonPath("$.data.userId").value(10001))
                .andExpect(jsonPath("$.data.modelName").value("qwen-plus"))
                .andExpect(jsonPath("$.data.providerMode").value("mock"));
    }

    @Test
    void shouldRejectMissingUserContext() throws Exception {
        mockMvc.perform(post("/api/v1/ai/chat")
                        .header(GatewayHeaders.TENANT_ID, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "hello agent"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));
    }
}
