package com.example.scm.aiagent.multiagent;

import com.example.scm.aiagent.multiagent.dto.MultiAgentChatRequest;
import com.example.scm.aiagent.multiagent.model.MultiAgentIntentType;
import com.example.scm.aiagent.multiagent.model.MultiAgentPlan;
import com.example.scm.aiagent.multiagent.service.MultiAgentPlannerService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiAgentPlannerServiceTest {

    private final MultiAgentPlannerService plannerService = new MultiAgentPlannerService();

    @Test
    void shouldDetectRagOnlyIntent() {
        MultiAgentChatRequest request = request("解释库存可用数量口径");
        request.setKnowledgeBaseId("kb-scm-demo");

        MultiAgentPlan plan = plannerService.plan(request);

        assertEquals(MultiAgentIntentType.RAG_ONLY, plan.getIntentType());
        assertTrue(plan.isNeedRag());
        assertFalse(plan.isNeedTool());
    }

    @Test
    void shouldDetectToolOnlyIntent() {
        MultiAgentPlan plan = plannerService.plan(request("帮我查物料 MAT-001"));

        assertEquals(MultiAgentIntentType.TOOL_ONLY, plan.getIntentType());
        assertFalse(plan.isNeedRag());
        assertTrue(plan.isNeedTool());
    }

    @Test
    void shouldDetectRagToolIntent() {
        MultiAgentPlan plan = plannerService.plan(request("按库存可用数量口径解释，并查物料 MAT-001 的库存"));

        assertEquals(MultiAgentIntentType.RAG_TOOL, plan.getIntentType());
        assertTrue(plan.isNeedRag());
        assertTrue(plan.isNeedTool());
    }

    private MultiAgentChatRequest request(String message) {
        MultiAgentChatRequest request = new MultiAgentChatRequest();
        request.setMessage(message);
        return request;
    }
}
