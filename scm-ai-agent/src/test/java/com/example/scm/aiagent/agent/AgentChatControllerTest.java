package com.example.scm.aiagent.agent;

import com.example.scm.aiagent.agent.controller.AgentChatController;
import com.example.scm.aiagent.agent.dto.AgentChatResponse;
import com.example.scm.aiagent.agent.dto.AgentOrchestrationView;
import com.example.scm.aiagent.agent.dto.AgentRagView;
import com.example.scm.aiagent.agent.service.RagToolAgentChatService;
import com.example.scm.aiagent.config.AiAgentSecurityConfig;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgentChatController.class)
@Import({GlobalExceptionHandler.class, TenantHeaderInterceptor.class, WebMvcConfiguration.class, AiAgentSecurityConfig.class})
class AgentChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RagToolAgentChatService agentChatService;

    @Test
    void shouldReturnRagToolAgentChatResponse() throws Exception {
        when(agentChatService.chat(any(), any())).thenReturn(AgentChatResponse.builder()
                .runId("run-agent-1")
                .intentType("RAG_TOOL")
                .answer("物料和库存组合回答")
                .rag(AgentRagView.builder()
                        .knowledgeBaseId("kb-scm")
                        .retrievedCount(1)
                        .chunks(List.of())
                        .build())
                .orchestration(AgentOrchestrationView.builder()
                        .enabled(true)
                        .runId("run-agent-1")
                        .planMode("MULTI_STEP_CONTROLLED")
                        .stepCount(2)
                        .steps(List.of())
                        .build())
                .latencyMs(20)
                .build());

        mockMvc.perform(post("/api/v1/ai/agent/chat")
                        .header(GatewayHeaders.TENANT_ID, "1")
                        .header(GatewayHeaders.USER_ID, "10001")
                        .header(GatewayHeaders.USERNAME, "admin")
                        .header(GatewayHeaders.USER_ROLES, "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "runId": "run-agent-1",
                                  "knowledgeBaseId": "kb-scm",
                                  "message": "按库存口径解释，并查物料 MAT-001 在仓库ID 1 的库存"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.runId").value("run-agent-1"))
                .andExpect(jsonPath("$.data.intentType").value("RAG_TOOL"))
                .andExpect(jsonPath("$.data.rag.retrievedCount").value(1))
                .andExpect(jsonPath("$.data.orchestration.stepCount").value(2))
                .andExpect(jsonPath("$.data.tool.execution.rawData").doesNotExist());
    }
}
