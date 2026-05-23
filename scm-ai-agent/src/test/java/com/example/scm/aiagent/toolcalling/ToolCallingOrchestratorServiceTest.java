package com.example.scm.aiagent.toolcalling;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.agent.service.RagToolIntentRouter;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.tool.dto.ToolInvokeRequest;
import com.example.scm.aiagent.tool.dto.ToolResponse;
import com.example.scm.aiagent.tool.model.ToolDefinition;
import com.example.scm.aiagent.tool.model.ToolRequest;
import com.example.scm.aiagent.tool.service.ToolInvocationService;
import com.example.scm.aiagent.tool.service.ToolRegistry;
import com.example.scm.aiagent.tool.spi.ToolExecutor;
import com.example.scm.aiagent.toolcalling.display.ToolCallingDisplaySchemaBuilder;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatRequest;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecutionView;
import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayData;
import com.example.scm.aiagent.toolcalling.model.ToolCallingPlan;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolCallingOrchestratorService;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationParameterResolver;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationPlanMode;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationPlanValidator;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationPlannerService;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationRun;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationRunStore;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationStepRefBuilder;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationStepStatus;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationStepSummaryBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        assertEquals(ToolOrchestrationPlanMode.SINGLE_STEP, stored.getPlan().getMode());
        assertEquals(1, stored.getPlan().getMaxSteps());
        assertEquals("step-1", stored.getSteps().get(0).getStepRef());
        assertEquals("$.steps[0].outputSummary", stored.getSteps().get(0).getOutputRef());
        assertEquals(ToolOrchestrationStepStatus.SUCCESS, stored.getSteps().get(0).getStatus());
        assertTrue(stored.getSteps().get(0).getExecutable());
        assertTrue(stored.getSteps().get(0).getExecuted());
        assertTrue(stored.getSteps().get(0).getInputResolved());
        assertEquals("库存余额", stored.getSteps().get(0).getExecution().getDisplayTitle());
        assertTrue(stored.getSteps().get(0).getOutputSummary().contains("displayTitle=库存余额"));
    }

    @Test
    void shouldKeepRequestedToolAsSingleStepPlan() {
        AiAgentProperties properties = dryRunProperties();
        ToolCallingOrchestratorService service = newService(properties);
        ToolCallingChatRequest request = request();
        request.setRequestedTool("mdm.getMaterial");

        ToolOrchestrationRun run = service.startRun(request, context, "run-requested", "spring-ai", "spring-ai");
        service.startStep(run, plan("mdm.getMaterial"));

        assertEquals(ToolOrchestrationPlanMode.SINGLE_STEP, run.getPlan().getMode());
        assertEquals(1, run.getSteps().size());
    }

    @Test
    void shouldCreateDryRunSkippedStepWithoutExecutingRealTool() {
        AiAgentProperties properties = dryRunProperties();
        ToolCallingOrchestratorService service = newService(properties);

        ToolOrchestrationRun run = service.startRun(request(), context, "run-dry-run", "spring-ai", "spring-ai");
        service.startStep(run, plan("inventory.getBalance"));
        service.finishStep(run, successExecution());

        assertEquals(ToolOrchestrationPlanMode.MULTI_STEP_DRY_RUN, run.getPlan().getMode());
        assertEquals(2, run.getSteps().size());
        assertEquals(ToolOrchestrationStepStatus.SUCCESS, run.getSteps().get(0).getStatus());
        assertEquals(ToolOrchestrationStepStatus.SKIPPED, run.getSteps().get(1).getStatus());
        assertFalse(run.getSteps().get(1).getExecutable());
        assertFalse(run.getSteps().get(1).getExecuted());
        assertFalse(run.getSteps().get(1).getInputResolved());
        assertEquals("step-2", run.getSteps().get(1).getStepRef());
        assertEquals(List.of("step-1.outputSummary"), run.getSteps().get(1).getInputRefs());
        assertEquals("multi-step dry-run only; real Tool is not executed", run.getSteps().get(1).getSkipReason());
        assertTrue(run.getSteps().get(1).getInputSummary().contains("inputRefs=[step-1.outputSummary]"));
    }

    @Test
    void shouldCreateControlledPlanButSkipFollowUpExecutionByDefault() {
        AiAgentProperties properties = enabledProperties();
        properties.getToolCalling().getOrchestrator().setPlanMode(ToolOrchestrationPlanMode.MULTI_STEP_CONTROLLED);
        properties.getToolCalling().getOrchestrator().setMaxSteps(2);
        properties.getToolCalling().getOrchestrator().setMultiStepEnabled(true);
        ToolCallingOrchestratorService service = newService(properties);

        ToolOrchestrationRun run = service.startRun(materialRequest(), context, "run-controlled", "spring-ai", "spring-ai");
        service.startStep(run, plan("mdm.getMaterial"));

        assertEquals(ToolOrchestrationPlanMode.MULTI_STEP_CONTROLLED, run.getPlan().getMode());
        assertEquals("inventory.getBalance", run.getSteps().get(1).getToolName());
        assertEquals(ToolOrchestrationStepStatus.SKIPPED, run.getSteps().get(1).getStatus());
        assertEquals("multi-step controlled plan created; follow-up Tool waits for explicit controlled execution",
                run.getSteps().get(1).getSkipReason());
    }

    @Test
    void shouldExecuteSecondReadOnlyToolWhenControlledExecutionEnabled() {
        AiAgentProperties properties = controlledExecutionProperties();
        ToolInvocationService invocationService = mock(ToolInvocationService.class);
        when(invocationService.invoke(any(ToolInvokeRequest.class), any())).thenReturn(ToolResponse.builder()
                .success(true)
                .toolName("inventory.getBalance")
                .runId("run-controlled-exec")
                .data(Map.of("materialId", 1001L, "warehouseId", 1L, "locationId", 2L, "availableQty", 128))
                .latencyMs(6)
                .build());
        ToolCallingOrchestratorService service = newService(properties, invocationService, true);

        ToolOrchestrationRun run = service.startRun(materialRequest(), context, "run-controlled-exec", "spring-ai", "spring-ai");
        service.startStep(run, plan("mdm.getMaterial"));
        service.finishStep(run, materialExecution());
        service.executeControlledFollowUp(run, context);

        assertEquals(2, run.getSteps().size());
        assertEquals("inventory.getBalance", run.getSteps().get(1).getToolName());
        assertEquals(ToolOrchestrationStepStatus.SUCCESS, run.getSteps().get(1).getStatus());
        assertTrue(run.getSteps().get(1).getExecutable());
        assertTrue(run.getSteps().get(1).getExecuted());
        assertTrue(run.getSteps().get(1).getInputResolved());
        assertEquals(1001L, run.getSteps().get(1).getArguments().get("materialId"));
        assertEquals(1L, run.getSteps().get(1).getArguments().get("warehouseId"));
        assertEquals(2L, run.getSteps().get(1).getArguments().get("locationId"));
        verify(invocationService).invoke(any(ToolInvokeRequest.class), any());
    }

    @Test
    void shouldSkipInventoryFollowUpWhenUserOnlyAsksMaterial() {
        AiAgentProperties properties = controlledExecutionProperties();
        ToolInvocationService invocationService = mock(ToolInvocationService.class);
        ToolCallingOrchestratorService service = newService(properties, invocationService, true);

        ToolOrchestrationRun run = service.startRun(materialOnlyRequest(), context, "run-material-only", "spring-ai", "spring-ai");
        service.startStep(run, plan("mdm.getMaterial"));
        service.finishStep(run, materialExecution());
        service.executeControlledFollowUp(run, context);

        assertEquals(ToolOrchestrationStepStatus.SKIPPED, run.getSteps().get(1).getStatus());
        assertFalse(run.getSteps().get(1).getExecuted());
        assertEquals("inventory follow-up intent is missing", run.getSteps().get(1).getInputResolveError());
        verify(invocationService, never()).invoke(any(ToolInvokeRequest.class), any());
    }

    @Test
    void shouldMapMdmReturnedIdToInventoryMaterialId() {
        AiAgentProperties properties = controlledExecutionProperties();
        ToolInvocationService invocationService = mock(ToolInvocationService.class);
        when(invocationService.invoke(any(ToolInvokeRequest.class), any())).thenReturn(ToolResponse.builder()
                .success(true)
                .toolName("inventory.getBalance")
                .runId("run-mdm-id")
                .data(Map.of("materialId", 1008L, "warehouseId", 1L, "availableQty", 88))
                .latencyMs(6)
                .build());
        ToolCallingOrchestratorService service = newService(properties, invocationService, true);

        ToolOrchestrationRun run = service.startRun(materialRequest(), context, "run-mdm-id", "spring-ai", "spring-ai");
        service.startStep(run, plan("mdm.getMaterial"));
        service.finishStep(run, materialExecutionWithRawId());
        service.executeControlledFollowUp(run, context);

        assertEquals(ToolOrchestrationStepStatus.SUCCESS, run.getSteps().get(1).getStatus());
        assertEquals(1008L, run.getSteps().get(1).getArguments().get("materialId"));
        assertEquals(1L, run.getSteps().get(1).getArguments().get("warehouseId"));
        verify(invocationService).invoke(any(ToolInvokeRequest.class), any());
    }


    @Test
    void shouldKeepSecondStepSkippedWhenControlledExecutionDisabled() {
        AiAgentProperties properties = enabledProperties();
        properties.getToolCalling().getOrchestrator().setPlanMode(ToolOrchestrationPlanMode.MULTI_STEP_CONTROLLED);
        properties.getToolCalling().getOrchestrator().setMaxSteps(2);
        properties.getToolCalling().getOrchestrator().setMultiStepEnabled(true);
        ToolInvocationService invocationService = mock(ToolInvocationService.class);
        ToolCallingOrchestratorService service = newService(properties, invocationService, true);

        ToolOrchestrationRun run = service.startRun(materialRequest(), context, "run-controlled-disabled", "spring-ai", "spring-ai");
        service.startStep(run, plan("mdm.getMaterial"));
        service.finishStep(run, materialExecution());
        service.executeControlledFollowUp(run, context);

        assertEquals(ToolOrchestrationStepStatus.SKIPPED, run.getSteps().get(1).getStatus());
        assertFalse(run.getSteps().get(1).getExecuted());
        verify(invocationService, never()).invoke(any(ToolInvokeRequest.class), any());
    }

    @Test
    void shouldSkipSecondStepWhenInputResolveFailedWithoutFakeAudit() {
        AiAgentProperties properties = controlledExecutionProperties();
        ToolInvocationService invocationService = mock(ToolInvocationService.class);
        ToolCallingOrchestratorService service = newService(properties, invocationService, true);

        ToolOrchestrationRun run = service.startRun(noCodeMaterialRequest(), context, "run-resolve-failed", "spring-ai", "spring-ai");
        service.startStep(run, planWithoutMaterialCode("mdm.getMaterial"));
        service.finishStep(run, materialExecutionWithoutCode());
        service.executeControlledFollowUp(run, context);

        assertEquals(ToolOrchestrationStepStatus.SKIPPED, run.getSteps().get(1).getStatus());
        assertFalse(run.getSteps().get(1).getInputResolved());
        assertTrue(run.getSteps().get(1).getInputResolveError().contains("materialId"));
        verify(invocationService, never()).invoke(any(ToolInvokeRequest.class), any());
    }

    @Test
    void shouldMarkSecondStepFailedWhenInvocationReturnsPermissionFailure() {
        AiAgentProperties properties = controlledExecutionProperties();
        ToolInvocationService invocationService = mock(ToolInvocationService.class);
        when(invocationService.invoke(any(ToolInvokeRequest.class), any())).thenReturn(ToolResponse.builder()
                .success(false)
                .toolName("inventory.getBalance")
                .runId("run-second-denied")
                .errorCode("403")
                .errorMessage("Tool permission denied")
                .latencyMs(3)
                .build());
        ToolCallingOrchestratorService service = newService(properties, invocationService, true);

        ToolOrchestrationRun run = service.startRun(materialRequest(), context, "run-second-denied", "spring-ai", "spring-ai");
        service.startStep(run, plan("mdm.getMaterial"));
        service.finishStep(run, materialExecution());
        service.executeControlledFollowUp(run, context);

        assertEquals(ToolOrchestrationStepStatus.FAILED, run.getSteps().get(1).getStatus());
        assertTrue(run.getSteps().get(1).getExecuted());
        assertEquals("403", run.getSteps().get(1).getExecution().getErrorCode());
        verify(invocationService).invoke(any(ToolInvokeRequest.class), any());
    }

    @Test
    void shouldNotExecuteSecondStepWhenFollowUpToolIsNotReadOnly() {
        AiAgentProperties properties = controlledExecutionProperties();
        ToolInvocationService invocationService = mock(ToolInvocationService.class);
        ToolCallingOrchestratorService service = newService(properties, invocationService, false);

        ToolOrchestrationRun run = service.startRun(materialRequest(), context, "run-not-readonly", "spring-ai", "spring-ai");
        service.startStep(run, plan("mdm.getMaterial"));
        service.finishStep(run, materialExecution());
        service.executeControlledFollowUp(run, context);

        assertEquals(1, run.getSteps().size());
        verify(invocationService, never()).invoke(any(ToolInvokeRequest.class), any());
    }

    @Test
    void shouldSkipDryRunNextStepWhenCurrentStepFails() {
        AiAgentProperties properties = dryRunProperties();
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
        assertEquals(ToolOrchestrationStepStatus.SKIPPED, stored.getSteps().get(1).getStatus());
        assertEquals("previous step failed; real Tool is not executed", stored.getSteps().get(1).getSkipReason());
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
        return newService(properties, mock(ToolInvocationService.class), true);
    }

    private ToolCallingOrchestratorService newService(AiAgentProperties properties,
                                                     ToolInvocationService invocationService,
                                                     boolean inventoryReadOnly) {
        ToolRegistry registry = new ToolRegistry(List.of(
                executor("inventory.getBalance", inventoryReadOnly),
                executor("mdm.getMaterial", true)
        ));
        ToolOrchestrationStepRefBuilder refBuilder = new ToolOrchestrationStepRefBuilder();
        ToolOrchestrationPlanValidator validator = new ToolOrchestrationPlanValidator(registry);
        ToolOrchestrationPlannerService planner = new ToolOrchestrationPlannerService(properties, registry, refBuilder, validator);
        return new ToolCallingOrchestratorService(properties, new ToolOrchestrationRunStore(properties),
                new ToolOrchestrationStepSummaryBuilder(), planner, new ToolOrchestrationParameterResolver(),
                invocationService, new ToolCallingDisplaySchemaBuilder(), registry, new RagToolIntentRouter());
    }

    private ToolExecutor executor(String toolName, boolean readOnly) {
        return new ToolExecutor() {
            @Override
            public ToolDefinition definition() {
                return ToolDefinition.builder()
                        .name(toolName)
                        .domain(toolName.substring(0, toolName.indexOf('.')))
                        .category("query")
                        .description(toolName)
                        .readOnly(readOnly)
                        .build();
            }

            @Override
            public Object execute(ToolRequest request) {
                return Map.of();
            }
        };
    }

    private AiAgentProperties enabledProperties() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getToolCalling().getOrchestrator().setEnabled(true);
        properties.getToolCalling().getOrchestrator().setRecordRuns(true);
        return properties;
    }

    private AiAgentProperties dryRunProperties() {
        AiAgentProperties properties = enabledProperties();
        properties.getToolCalling().getOrchestrator().setPlanMode(ToolOrchestrationPlanMode.MULTI_STEP_DRY_RUN);
        properties.getToolCalling().getOrchestrator().setMaxSteps(2);
        properties.getToolCalling().getOrchestrator().setMultiStepEnabled(true);
        properties.getToolCalling().getOrchestrator().setDryRunEnabled(true);
        return properties;
    }

    private AiAgentProperties controlledExecutionProperties() {
        AiAgentProperties properties = enabledProperties();
        properties.getToolCalling().getOrchestrator().setPlanMode(ToolOrchestrationPlanMode.MULTI_STEP_CONTROLLED);
        properties.getToolCalling().getOrchestrator().setMaxSteps(2);
        properties.getToolCalling().getOrchestrator().setMultiStepEnabled(true);
        properties.getToolCalling().getOrchestrator().setControlledExecutionEnabled(true);
        properties.getToolCalling().getOrchestrator().setMaxExecutableSteps(2);
        properties.getToolCalling().getOrchestrator().setAllowSecondStepReadOnly(true);
        return properties;
    }

    private ToolCallingChatRequest request() {
        ToolCallingChatRequest request = new ToolCallingChatRequest();
        request.setMessage("帮我查库存余额");
        request.setRequestedDomain("inventory");
        request.setRouteTags(List.of("inventory", "balance"));
        return request;
    }

    private ToolCallingChatRequest materialRequest() {
        ToolCallingChatRequest request = new ToolCallingChatRequest();
        request.setMessage("帮我查物料 MAT-001，并看看仓库ID 1、库位ID 2 的库存");
        request.setRequestedDomain("mdm");
        request.setRouteTags(List.of("mdm", "material"));
        return request;
    }

    private ToolCallingChatRequest noCodeMaterialRequest() {
        ToolCallingChatRequest request = new ToolCallingChatRequest();
        request.setMessage("帮我查这个物料，并看看库存");
        request.setRequestedDomain("mdm");
        request.setRouteTags(List.of("mdm", "material"));
        return request;
    }

    private ToolCallingChatRequest materialOnlyRequest() {
        ToolCallingChatRequest request = new ToolCallingChatRequest();
        request.setMessage("帮我查物料 MAT-001");
        request.setRequestedDomain("mdm");
        request.setRouteTags(List.of("mdm", "material"));
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

    private ToolCallingPlan planWithoutMaterialCode(String toolName) {
        return ToolCallingPlan.builder()
                .plannerMode("spring-ai")
                .planningSource("spring-ai")
                .selectedTool(toolName)
                .toolArguments(Map.of())
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

    private ToolCallingExecutionView materialExecution() {
        return ToolCallingExecutionView.builder()
                .success(true)
                .toolName("mdm.getMaterial")
                .data(new ToolCallingDisplayData("物料信息", "已查询到物料 MAT-001", List.of(), List.of(),
                        Map.of("materialId", 1001L, "materialCode", "MAT-001")))
                .latencyMs(8)
                .build();
    }

    private ToolCallingExecutionView materialExecutionWithoutCode() {
        return ToolCallingExecutionView.builder()
                .success(true)
                .toolName("mdm.getMaterial")
                .data(new ToolCallingDisplayData("物料信息", "已查询到物料", List.of(), List.of(),
                        Map.of("materialName", "测试物料")))
                .latencyMs(8)
                .build();
    }

    private ToolCallingExecutionView materialExecutionWithRawId() {
        return ToolCallingExecutionView.builder()
                .success(true)
                .toolName("mdm.getMaterial")
                .data(new ToolCallingDisplayData("物料信息", "已查询到物料 MAT-001", List.of(), List.of(),
                        Map.of("id", 1008L, "materialCode", "MAT-001")))
                .latencyMs(8)
                .build();
    }
}
