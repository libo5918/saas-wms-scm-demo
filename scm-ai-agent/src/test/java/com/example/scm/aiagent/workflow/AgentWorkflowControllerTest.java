package com.example.scm.aiagent.workflow;

import com.example.scm.aiagent.config.AiAgentSecurityConfig;
import com.example.scm.aiagent.workflow.controller.AgentWorkflowController;
import com.example.scm.aiagent.workflow.dto.AgentWorkflowDefinitionView;
import com.example.scm.aiagent.workflow.dto.AgentWorkflowRunResponse;
import com.example.scm.aiagent.workflow.dto.AgentWorkflowStepDefinitionView;
import com.example.scm.aiagent.workflow.dto.AgentWorkflowStepView;
import com.example.scm.aiagent.workflow.service.AgentWorkflowService;
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

@WebMvcTest(AgentWorkflowController.class)
@Import({GlobalExceptionHandler.class, TenantHeaderInterceptor.class, WebMvcConfiguration.class, AiAgentSecurityConfig.class})
class AgentWorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AgentWorkflowService workflowService;

    @Test
    void shouldListWorkflowDefinitions() throws Exception {
        when(workflowService.listDefinitions()).thenReturn(List.of(AgentWorkflowDefinitionView.builder()
                .workflowCode("scm_stock_replenishment_advice")
                .workflowName("库存补货建议草案")
                .enabled(true)
                .steps(List.of(AgentWorkflowStepDefinitionView.builder()
                        .stepCode("query_material")
                        .stepType("TOOL")
                        .toolName("mdm.getMaterial")
                        .build()))
                .build()));

        mockMvc.perform(get("/api/v1/ai/workflows")
                        .header(GatewayHeaders.TENANT_ID, "1")
                        .header(GatewayHeaders.USER_ID, "10001")
                        .header(GatewayHeaders.USERNAME, "admin")
                        .header(GatewayHeaders.USER_ROLES, "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].workflowCode").value("scm_stock_replenishment_advice"));
    }

    @Test
    void shouldRunWorkflowAndReturnSafeStatus() throws Exception {
        when(workflowService.run(eq("scm_stock_replenishment_advice"), any(), any())).thenReturn(runResponse());

        mockMvc.perform(post("/api/v1/ai/workflows/scm_stock_replenishment_advice/run")
                        .header(GatewayHeaders.TENANT_ID, "1")
                        .header(GatewayHeaders.USER_ID, "10001")
                        .header(GatewayHeaders.USERNAME, "admin")
                        .header(GatewayHeaders.USER_ROLES, "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "runId": "run-workflow-1",
                                  "message": "帮我生成物料 MAT-001 在仓库ID 1、库位ID 2 的补货建议草案"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.runId").value("run-workflow-1"))
                .andExpect(jsonPath("$.data.steps[0].safeFields.rawData").doesNotExist());
    }

    @Test
    void shouldGetWorkflowRunStatus() throws Exception {
        when(workflowService.getRun("run-workflow-1")).thenReturn(runResponse());

        mockMvc.perform(get("/api/v1/ai/workflows/runs/run-workflow-1")
                        .header(GatewayHeaders.TENANT_ID, "1")
                        .header(GatewayHeaders.USER_ID, "10001")
                        .header(GatewayHeaders.USERNAME, "admin")
                        .header(GatewayHeaders.USER_ROLES, "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.runId").value("run-workflow-1"))
                .andExpect(jsonPath("$.data.steps[0].safeFields.rawData").doesNotExist());
    }

    private AgentWorkflowRunResponse runResponse() {
        return AgentWorkflowRunResponse.builder()
                .runId("run-workflow-1")
                .workflowCode("scm_stock_replenishment_advice")
                .workflowName("库存补货建议草案")
                .status("SUCCESS")
                .finalAnswer("补货建议草案")
                .steps(List.of(AgentWorkflowStepView.builder()
                        .stepCode("query_material")
                        .stepName("查询物料")
                        .stepNo(1)
                        .stepType("TOOL")
                        .status("SUCCESS")
                        .toolName("mdm.getMaterial")
                        .safeFields(Map.of("materialId", 1001L))
                        .build()))
                .build();
    }
}
