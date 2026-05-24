package com.example.scm.aiagent.workflow;

import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.workflow.dto.AgentWorkflowRunRequest;
import com.example.scm.aiagent.workflow.engine.AgentWorkflowExecutionContext;
import com.example.scm.aiagent.workflow.model.AgentWorkflowRun;
import com.example.scm.aiagent.workflow.model.AgentWorkflowStep;
import com.example.scm.aiagent.workflow.model.AgentWorkflowStepStatus;
import com.example.scm.aiagent.workflow.service.AgentWorkflowDefinitionRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentWorkflowExecutionContextTest {

    @Test
    void shouldStoreSafeStepOutputByStepCode() {
        AgentWorkflowRunRequest request = new AgentWorkflowRunRequest();
        AgentWorkflowRun run = AgentWorkflowRun.builder()
                .runId("run-context")
                .workflowCode("scm_stock_replenishment_advice")
                .workflowName("库存补货建议草案")
                .steps(new ArrayList<>())
                .build();
        AgentWorkflowExecutionContext context = new AgentWorkflowExecutionContext(
                new AgentWorkflowDefinitionRegistry().listDefinitions().get(0),
                request,
                new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN")),
                run);

        context.putStepOutput("query_material", Map.of("materialId", 1001L));
        context.completeStep(AgentWorkflowStep.builder()
                .stepCode("query_material")
                .status(AgentWorkflowStepStatus.SUCCESS)
                .build());

        assertEquals(1001L, context.getStepOutput("query_material").get("materialId"));
        assertTrue(context.isStepSuccess("query_material"));
    }
}
