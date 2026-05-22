package com.example.scm.aiagent.toolcalling;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.tool.model.ToolDefinition;
import com.example.scm.aiagent.tool.model.ToolRequest;
import com.example.scm.aiagent.tool.service.ToolRegistry;
import com.example.scm.aiagent.tool.spi.ToolExecutor;
import com.example.scm.aiagent.toolcalling.model.ToolCallingPlan;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationPlan;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationPlanMode;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationPlanValidationResult;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationPlanValidator;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationPlannerService;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationRun;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationStep;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationStepRefBuilder;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationStepStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolOrchestrationPlannerServiceTest {

    @Test
    void shouldBuildSingleStepPlanByDefault() {
        AiAgentProperties properties = new AiAgentProperties();
        ToolOrchestrationPlannerService planner = planner(properties, readOnlyRegistry());

        ToolOrchestrationPlan plan = planner.buildPlan(run(null), toolPlan("inventory.getBalance"));

        assertEquals(ToolOrchestrationPlanMode.SINGLE_STEP, plan.getMode());
        assertEquals(1, plan.getSteps().size());
        assertEquals("step-1", plan.getSteps().get(0).getStepRef());
        assertEquals("$.steps[0].outputSummary", plan.getSteps().get(0).getOutputRef());
    }

    @Test
    void shouldForceSingleStepWhenRequestedToolExists() {
        AiAgentProperties properties = dryRunProperties();
        ToolOrchestrationPlannerService planner = planner(properties, readOnlyRegistry());

        ToolOrchestrationPlan plan = planner.buildPlan(run("mdm.getMaterial"), toolPlan("mdm.getMaterial"));

        assertEquals(ToolOrchestrationPlanMode.SINGLE_STEP, plan.getMode());
        assertEquals(1, plan.getSteps().size());
    }

    @Test
    void shouldBuildDryRunMultiStepPlanWhenEnabled() {
        AiAgentProperties properties = dryRunProperties();
        ToolOrchestrationPlannerService planner = planner(properties, readOnlyRegistry());

        ToolOrchestrationPlan plan = planner.buildPlan(run(null), toolPlan("inventory.getBalance"));

        assertEquals(ToolOrchestrationPlanMode.MULTI_STEP_DRY_RUN, plan.getMode());
        assertEquals(2, plan.getSteps().size());
        assertEquals(ToolOrchestrationStepStatus.SKIPPED, plan.getSteps().get(1).getStatus());
        assertEquals(List.of("step-1.outputSummary"), plan.getSteps().get(1).getInputRefs());
    }

    @Test
    void shouldBuildControlledPlanButKeepFollowUpSkipped() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getToolCalling().getOrchestrator().setPlanMode(ToolOrchestrationPlanMode.MULTI_STEP_CONTROLLED);
        properties.getToolCalling().getOrchestrator().setMaxSteps(2);
        properties.getToolCalling().getOrchestrator().setMultiStepEnabled(true);
        ToolOrchestrationPlannerService planner = planner(properties, readOnlyRegistry());

        ToolOrchestrationPlan plan = planner.buildPlan(run(null), toolPlan("inventory.getBalance"));

        assertEquals(ToolOrchestrationPlanMode.MULTI_STEP_CONTROLLED, plan.getMode());
        assertEquals(ToolOrchestrationStepStatus.SKIPPED, plan.getSteps().get(1).getStatus());
    }

    @Test
    void shouldFallbackSingleStepWhenCandidatesAreInsufficient() {
        AiAgentProperties properties = dryRunProperties();
        ToolOrchestrationPlannerService planner = planner(properties, registry(executor("inventory.getBalance", true)));

        ToolOrchestrationPlan plan = planner.buildPlan(run(null), toolPlan("inventory.getBalance"));

        assertEquals(ToolOrchestrationPlanMode.SINGLE_STEP, plan.getMode());
        assertEquals(1, plan.getSteps().size());
    }

    @Test
    void validatorShouldRejectTooManyStepsAndSensitiveSummary() {
        ToolOrchestrationPlanValidator validator = new ToolOrchestrationPlanValidator(readOnlyRegistry());
        ToolOrchestrationPlan plan = ToolOrchestrationPlan.builder()
                .planId("plan-1")
                .runId("run-1")
                .mode(ToolOrchestrationPlanMode.MULTI_STEP_DRY_RUN)
                .maxSteps(1)
                .generatedBy("test")
                .createdAt(Instant.now())
                .steps(List.of(step(1, "inventory.getBalance"), step(2, "orchestrator.futureStep")))
                .build();

        ToolOrchestrationPlanValidationResult result = validator.validate(plan, 1);

        assertFalse(result.valid());
        assertEquals("steps exceed maxSteps", result.reason());

        ToolOrchestrationStep sensitive = step(1, "inventory.getBalance");
        sensitive.setOutputSummary("rawData token authorization");
        ToolOrchestrationPlan sensitivePlan = ToolOrchestrationPlan.builder()
                .planId("plan-2")
                .runId("run-2")
                .mode(ToolOrchestrationPlanMode.SINGLE_STEP)
                .maxSteps(1)
                .generatedBy("test")
                .createdAt(Instant.now())
                .steps(List.of(sensitive))
                .build();
        assertFalse(validator.validate(sensitivePlan, 1).valid());
    }

    @Test
    void validatorShouldRejectNonReadOnlyToolAndFutureStepRef() {
        ToolRegistry registry = registry(executor("inventory.adjust", false), executor("inventory.getBalance", true));
        ToolOrchestrationPlanValidator validator = new ToolOrchestrationPlanValidator(registry);
        ToolOrchestrationStep writeStep = step(1, "inventory.adjust");
        ToolOrchestrationPlan writePlan = ToolOrchestrationPlan.builder()
                .planId("plan-write")
                .runId("run-write")
                .mode(ToolOrchestrationPlanMode.SINGLE_STEP)
                .maxSteps(1)
                .steps(List.of(writeStep))
                .generatedBy("test")
                .createdAt(Instant.now())
                .build();

        assertFalse(validator.validate(writePlan, 1).valid());

        ToolOrchestrationStep futureRefStep = step(2, "orchestrator.futureStep");
        futureRefStep.setInputRefs(List.of("step-2.outputSummary"));
        ToolOrchestrationPlan futureRefPlan = ToolOrchestrationPlan.builder()
                .planId("plan-ref")
                .runId("run-ref")
                .mode(ToolOrchestrationPlanMode.MULTI_STEP_DRY_RUN)
                .maxSteps(2)
                .steps(List.of(step(1, "inventory.getBalance"), futureRefStep))
                .generatedBy("test")
                .createdAt(Instant.now())
                .build();

        assertFalse(validator.validate(futureRefPlan, 2).valid());
    }

    private ToolOrchestrationPlannerService planner(AiAgentProperties properties, ToolRegistry registry) {
        ToolOrchestrationStepRefBuilder refBuilder = new ToolOrchestrationStepRefBuilder();
        return new ToolOrchestrationPlannerService(properties, registry, refBuilder,
                new ToolOrchestrationPlanValidator(registry));
    }

    private ToolOrchestrationRun run(String requestedTool) {
        return ToolOrchestrationRun.builder()
                .runId("run-plan")
                .tenantId(1L)
                .userId(10001L)
                .userMessage("帮我查库存")
                .requestedTool(requestedTool)
                .build();
    }

    private ToolCallingPlan toolPlan(String toolName) {
        return ToolCallingPlan.builder()
                .selectedTool(toolName)
                .toolArguments(Map.of("materialCode", "MAT-001"))
                .reason("model_plan")
                .build();
    }

    private ToolOrchestrationStep step(int stepNo, String toolName) {
        return ToolOrchestrationStep.builder()
                .stepId("run-step-" + stepNo)
                .stepRef("step-" + stepNo)
                .stepNo(stepNo)
                .toolName(toolName)
                .status(stepNo == 1 ? ToolOrchestrationStepStatus.RUNNING : ToolOrchestrationStepStatus.SKIPPED)
                .inputRefs(stepNo == 1 ? List.of() : List.of("step-1.outputSummary"))
                .outputRef("$.steps[" + (stepNo - 1) + "].outputSummary")
                .build();
    }

    private AiAgentProperties dryRunProperties() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getToolCalling().getOrchestrator().setPlanMode(ToolOrchestrationPlanMode.MULTI_STEP_DRY_RUN);
        properties.getToolCalling().getOrchestrator().setMaxSteps(2);
        properties.getToolCalling().getOrchestrator().setMultiStepEnabled(true);
        properties.getToolCalling().getOrchestrator().setDryRunEnabled(true);
        return properties;
    }

    private ToolRegistry readOnlyRegistry() {
        return registry(executor("inventory.getBalance", true), executor("mdm.getMaterial", true));
    }

    private ToolRegistry registry(ToolExecutor... executors) {
        return new ToolRegistry(List.of(executors));
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
}
