package com.example.scm.aiagent.multiagent;

import com.example.scm.aiagent.config.AiAgentSecurityConfig;
import com.example.scm.aiagent.multiagent.controller.MultiAgentController;
import com.example.scm.aiagent.multiagent.dto.MultiAgentAgentView;
import com.example.scm.aiagent.multiagent.dto.MultiAgentChatResponse;
import com.example.scm.aiagent.multiagent.dto.MultiAgentStepView;
import com.example.scm.aiagent.multiagent.model.MultiAgentActionType;
import com.example.scm.aiagent.multiagent.model.MultiAgentRole;
import com.example.scm.aiagent.multiagent.model.MultiAgentRunStatus;
import com.example.scm.aiagent.multiagent.model.MultiAgentStepStatus;
import com.example.scm.aiagent.multiagent.service.MultiAgentCoordinatorService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MultiAgentController.class)
@Import({GlobalExceptionHandler.class, TenantHeaderInterceptor.class, WebMvcConfiguration.class, AiAgentSecurityConfig.class})
class MultiAgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MultiAgentCoordinatorService coordinatorService;

    @Test
    void shouldRunMultiAgentChat() throws Exception {
        when(coordinatorService.chat(any(), any())).thenReturn(response("run-multi-agent-1"));

        mockMvc.perform(post("/api/v1/ai/multi-agent/chat")
                        .header(GatewayHeaders.TENANT_ID, "1")
                        .header(GatewayHeaders.USER_ID, "10001")
                        .header(GatewayHeaders.USERNAME, "admin")
                        .header(GatewayHeaders.USER_ROLES, "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "runId": "run-multi-agent-1",
                                  "message": "按库存可用数量口径解释，并查物料 MAT-001 的库存"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.runId").value("run-multi-agent-1"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.agents[0].agentName").value("CoordinatorAgent"))
                .andExpect(jsonPath("$.data.steps[1].actionType").value("PLAN"))
                .andExpect(jsonPath("$.data.rawData").doesNotExist())
                .andExpect(jsonPath("$.data.prompt").doesNotExist());
    }

    @Test
    void shouldQueryRunStatus() throws Exception {
        when(coordinatorService.getRun("run-multi-agent-1")).thenReturn(response("run-multi-agent-1"));

        mockMvc.perform(get("/api/v1/ai/multi-agent/runs/run-multi-agent-1")
                        .header(GatewayHeaders.TENANT_ID, "1")
                        .header(GatewayHeaders.USER_ID, "10001")
                        .header(GatewayHeaders.USERNAME, "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.runId").value("run-multi-agent-1"))
                .andExpect(jsonPath("$.data.steps[0].outputSummary").value("已接收用户任务"));
    }

    @Test
    void shouldRejectMissingUserContext() throws Exception {
        mockMvc.perform(post("/api/v1/ai/multi-agent/chat")
                        .header(GatewayHeaders.TENANT_ID, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"测试 Multi-Agent"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));
    }

    private MultiAgentChatResponse response(String runId) {
        return MultiAgentChatResponse.builder()
                .runId(runId)
                .status(MultiAgentRunStatus.SUCCESS)
                .answer("已创建 Multi-Agent 协作运行骨架")
                .agents(List.of(
                        MultiAgentAgentView.builder()
                                .agentName("CoordinatorAgent")
                                .role(MultiAgentRole.COORDINATOR)
                                .status(MultiAgentStepStatus.SUCCESS)
                                .build(),
                        MultiAgentAgentView.builder()
                                .agentName("PlannerAgent")
                                .role(MultiAgentRole.PLANNER)
                                .status(MultiAgentStepStatus.SUCCESS)
                                .build()))
                .steps(List.of(
                        MultiAgentStepView.builder()
                                .stepNo(1)
                                .agentName("CoordinatorAgent")
                                .actionType(MultiAgentActionType.NOOP)
                                .status(MultiAgentStepStatus.SUCCESS)
                                .outputSummary("已接收用户任务")
                                .build(),
                        MultiAgentStepView.builder()
                                .stepNo(2)
                                .agentName("PlannerAgent")
                                .actionType(MultiAgentActionType.PLAN)
                                .status(MultiAgentStepStatus.SUCCESS)
                                .outputSummary("识别为后续可扩展的多 Agent 协作任务")
                                .build()))
                .messages(List.of())
                .latencyMs(12)
                .build();
    }
}
