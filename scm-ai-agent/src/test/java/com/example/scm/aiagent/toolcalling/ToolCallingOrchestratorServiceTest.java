package com.example.scm.aiagent.toolcalling;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatRequest;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecutionView;
import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayData;
import com.example.scm.aiagent.toolcalling.model.ToolCallingPlan;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolCallingOrchestratorService;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationRun;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationRunStore;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationStepStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolCallingOrchestratorServiceTest {

    private final AgentRequestContext context = new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));

    @Test
    void shouldNotRecordWhenOrchestratorDisabled() {
        AiAgentProperties properties = new AiAgentProperties();
        ToolCallingOrchestratorService service = newService(properties);

        ToolOrchestrationRun run = service.startRun(request(), context, "run-disabled", "spring-ai", "spring-ai");

        assertNull(run);
        assertTrue(service.listRuns(10).isEmpty());
    }

    @Test
    void shouldRecordSuccessfulSingleStepRun() {
        AiAgentProperties properties = enabledProperties();
        ToolCallingOrchestratorService service = newService(properties);

        ToolOrchestrationRun run = service.startRun(request(), context, "run-success", "spring-ai", "spring-ai");
        service.startStep(run, plan("inventory.getBalance"));
        service.finishStep(run, successExecution());
        service.finishRun(run, true, "库存余额为 128", 20);

        ToolOrchestrationRun stored = service.getRun("run-success");
        assertNotNull(stored);
        assertTrue(stored.isSuccess());
        assertEquals("库存余额为 128", stored.getFinalAnswer());
        assertEquals(1, stored.getSteps().size());
        assertEquals(ToolOrchestrationStepStatus.SUCCESS, stored.getSteps().get(0).getStatus());
        assertEquals("库存余额", stored.getSteps().get(0).getExecution().getDisplayTitle());
    }

    @Test
    void shouldMarkStepFailedWhenExecutionFails() {
        AiAgentProperties properties = enabledProperties();
        ToolCallingOrchestratorService service = newService(properties);

        ToolOrchestrationRun run = service.startRun(request(), context, "run-failed", "spring-ai", "spring-ai");
        service.startStep(run, plan("mdm.getMaterial"));
        service.finishStep(run, ToolCallingExecutionView.builder()
                .success(false)
                .toolName("mdm.getMaterial")
                .errorCode("403")
                .errorMessage("Tool permission denied")
                .latencyMs(2)
                .build());
        service.finishRun(run, false, "权限不足", 5);

        ToolOrchestrationRun stored = service.getRun("run-failed");
        assertFalse(stored.isSuccess());
        assertEquals(ToolOrchestrationStepStatus.FAILED, stored.getSteps().get(0).getStatus());
        assertEquals("403", stored.getSteps().get(0).getExecution().getErrorCode());
    }

    @Test
    void shouldTrimOldRunsByMaxRecords() {
        AiAgentProperties properties = enabledProperties();
        properties.getToolCalling().getOrchestrator().setMaxRecords(2);
        ToolCallingOrchestratorService service = newService(properties);

        for (int i = 1; i <= 3; i++) {
            ToolOrchestrationRun run = service.startRun(request(), context, "run-" + i, "spring-ai", "spring-ai");
            service.finishRun(run, true, "ok", i);
        }

        assertNull(service.getRun("run-1"));
        assertEquals(2, service.listRuns(10).size());
        assertEquals("run-3", service.listRuns(10).get(0).getRunId());
    }

    private ToolCallingOrchestratorService newService(AiAgentProperties properties) {
        return new ToolCallingOrchestratorService(properties, new ToolOrchestrationRunStore(properties));
    }

    private AiAgentProperties enabledProperties() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getToolCalling().getOrchestrator().setEnabled(true);
        properties.getToolCalling().getOrchestrator().setRecordRuns(true);
        return properties;
    }

    private ToolCallingChatRequest request() {
        ToolCallingChatRequest request = new ToolCallingChatRequest();
        request.setMessage("帮我查库存余额");
        request.setRequestedDomain("inventory");
        request.setRouteTags(List.of("inventory", "balance"));
        return request;
    }

    private ToolCallingPlan plan(String toolName) {
        return ToolCallingPlan.builder()
                .plannerMode("spring-ai")
                .planningSource("spring-ai")
                .selectedTool(toolName)
                .toolArguments(Map.of("materialCode", "MAT-001"))
                .reason("model_plan")
                .build();
    }

    private ToolCallingExecutionView successExecution() {
        return ToolCallingExecutionView.builder()
                .success(true)
                .toolName("inventory.getBalance")
                .data(new ToolCallingDisplayData("库存余额", "已查询到库存余额", List.of(), List.of(),
                        Map.of("availableQty", 128)))
                .latencyMs(8)
                .build();
    }
}
