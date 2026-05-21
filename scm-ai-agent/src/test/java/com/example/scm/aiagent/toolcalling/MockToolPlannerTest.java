package com.example.scm.aiagent.toolcalling;

import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatRequest;
import com.example.scm.aiagent.toolcalling.model.ToolCallingPlan;
import com.example.scm.aiagent.toolcalling.planning.MockToolPlanner;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MockToolPlannerTest {

    private final MockToolPlanner planner = new MockToolPlanner();

    @Test
    void shouldRouteInventoryQuestionToInventoryTool() {
        ToolCallingChatRequest request = new ToolCallingChatRequest();
        request.setMessage("帮我查一下库存余额");

        ToolCallingPlan plan = planner.plan(request);

        assertEquals("inventory.getBalance", plan.selectedTool());
        assertEquals(1001L, plan.toolArguments().get("materialId"));
        assertEquals(1L, plan.toolArguments().get("warehouseId"));
    }

    @Test
    void shouldPreferRequestedToolOverMessageRules() {
        ToolCallingChatRequest request = new ToolCallingChatRequest();
        request.setMessage("帮我查一下库存");
        request.setRequestedTool("mdm.getMaterial");
        request.setToolArguments(Map.of("materialCode", "MAT-001"));

        ToolCallingPlan plan = planner.plan(request);

        assertEquals("mdm.getMaterial", plan.selectedTool());
        assertEquals("MAT-001", plan.toolArguments().get("materialCode"));
    }
}
