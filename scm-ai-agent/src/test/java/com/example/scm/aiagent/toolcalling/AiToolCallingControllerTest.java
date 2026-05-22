package com.example.scm.aiagent.toolcalling;

import com.example.scm.aiagent.config.AiAgentSecurityConfig;
import com.example.scm.aiagent.tool.dto.ToolResponse;
import com.example.scm.aiagent.toolcalling.application.SpringAiToolCallingService;
import com.example.scm.aiagent.toolcalling.application.ToolCallingChatService;
import com.example.scm.aiagent.toolcalling.controller.AiToolCallingController;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatResponse;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecuteResponse;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecutionView;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingSchemaListResponse;
import com.example.scm.aiagent.toolcalling.model.SpringAiToolDescriptor;
import com.example.scm.aiagent.toolcalling.model.SpringAiToolInputSchema;
import com.example.scm.aiagent.toolcalling.model.SpringAiToolParameterSchema;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolCallingOrchestratorService;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationExecutionSummary;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationPlan;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationPlanMode;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationRun;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationStep;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationStepStatus;
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

    @MockitoBean
    private ToolCallingOrchestratorService orchestratorService;

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
                .toolResponse(ToolResponse.builder()
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
                .planningReason("model_plan")
                .execution(ToolCallingExecutionView.builder()
                        .success(true)
                        .toolName("sales.getOrder")
                        .data(Map.of("orderNo", "SO-001", "status", "ALLOCATED"))
                        .latencyMs(8)
                        .build())
                .answer("已查询到销售订单 SO-001，状态 ALLOCATED。")
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
                .andExpect(jsonPath("$.data.selectedTool").value("sales.getOrder"))
                .andExpect(jsonPath("$.data.execution.success").value(true))
                .andExpect(jsonPath("$.data.execution.toolName").value("sales.getOrder"))
                .andExpect(jsonPath("$.data.answer").value("已查询到销售订单 SO-001，状态 ALLOCATED。"));
    }

    @Test
    void shouldListOrchestrationRuns() throws Exception {
        when(orchestratorService.listRuns(20)).thenReturn(List.of(orchestrationRun()));

        mockMvc.perform(get("/api/v1/ai/tool-calling/orchestrations")
                        .header(GatewayHeaders.TENANT_ID, "1")
                        .header(GatewayHeaders.USER_ID, "10001")
                        .header(GatewayHeaders.USERNAME, "admin")
                        .header(GatewayHeaders.USER_ROLES, "ROLE_ADMIN")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].runId").value("run-orch-1"))
                .andExpect(jsonPath("$.data[0].plan.mode").value("SINGLE_STEP"))
                .andExpect(jsonPath("$.data[0].steps[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].steps[0].execution.displayTitle").value("库存余额"))
                .andExpect(jsonPath("$.data[0].steps[0].execution.rawData").doesNotExist());
    }

    @Test
    void shouldGetOrchestrationRunByRunId() throws Exception {
        when(orchestratorService.getRun("run-orch-1")).thenReturn(orchestrationRun());

        mockMvc.perform(get("/api/v1/ai/tool-calling/orchestrations/run-orch-1")
                        .header(GatewayHeaders.TENANT_ID, "1")
                        .header(GatewayHeaders.USER_ID, "10001")
                        .header(GatewayHeaders.USERNAME, "admin")
                        .header(GatewayHeaders.USER_ROLES, "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.runId").value("run-orch-1"))
                .andExpect(jsonPath("$.data.plan.generatedBy").value("orchestration-planner-single-step"))
                .andExpect(jsonPath("$.data.steps[0].stepRef").value("step-1"))
                .andExpect(jsonPath("$.data.steps[0].outputRef").value("$.steps[0].outputSummary"))
                .andExpect(jsonPath("$.data.steps[0].execution.toolName").value("inventory.getBalance"));
    }

    private ToolOrchestrationRun orchestrationRun() {
        return ToolOrchestrationRun.builder()
                .runId("run-orch-1")
                .tenantId(1L)
                .userId(10001L)
                .plannerMode("spring-ai")
                .answerMode("spring-ai")
                .routeTags(List.of("inventory"))
                .plan(ToolOrchestrationPlan.builder()
                        .planId("run-orch-1-plan-1")
                        .runId("run-orch-1")
                        .mode(ToolOrchestrationPlanMode.SINGLE_STEP)
                        .objective("查询库存余额")
                        .steps(List.of())
                        .maxSteps(1)
                        .generatedBy("orchestration-planner-single-step")
                        .createdAt(Instant.parse("2026-05-23T01:00:00Z"))
                        .build())
                .steps(List.of(ToolOrchestrationStep.builder()
                        .stepId("run-orch-1-step-1")
                        .stepRef("step-1")
                        .stepNo(1)
                        .toolName("inventory.getBalance")
                        .inputRefs(List.of())
                        .outputRef("$.steps[0].outputSummary")
                        .status(ToolOrchestrationStepStatus.SUCCESS)
                        .startedAt(Instant.parse("2026-05-23T01:00:00Z"))
                        .finishedAt(Instant.parse("2026-05-23T01:00:01Z"))
                        .latencyMs(1000)
                        .execution(ToolOrchestrationExecutionSummary.builder()
                                .success(true)
                                .toolName("inventory.getBalance")
                                .latencyMs(8)
                                .displayTitle("库存余额")
                                .displaySummary("已查询到库存余额")
                                .build())
                        .build()))
                .success(true)
                .finalAnswer("库存可用数量为 128")
                .createdAt(Instant.parse("2026-05-23T01:00:00Z"))
                .finishedAt(Instant.parse("2026-05-23T01:00:01Z"))
                .latencyMs(1000)
                .build();
    }
}
