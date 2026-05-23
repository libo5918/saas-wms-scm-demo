package com.example.scm.aiagent.agent;

import com.example.scm.aiagent.agent.service.AgentIntentType;
import com.example.scm.aiagent.agent.service.RagToolIntentRouter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagToolIntentRouterTest {

    private final RagToolIntentRouter router = new RagToolIntentRouter();

    @Test
    void shouldRouteRagOnlyToolOnlyAndRagTool() {
        assertEquals(AgentIntentType.RAG_ONLY, router.route("解释库存可用数量口径", "kb-scm"));
        assertEquals(AgentIntentType.TOOL_ONLY, router.route("帮我查物料 MAT-001", null));
        assertEquals(AgentIntentType.RAG_TOOL, router.route("按库存口径解释并查物料 MAT-001 的库存", "kb-scm"));
    }

    @Test
    void shouldDetectInventoryFollowUpIntent() {
        assertFalse(router.hasInventoryFollowUpIntent("帮我查物料 MAT-001"));
        assertTrue(router.hasInventoryFollowUpIntent("帮我查物料 MAT-001 的库存余额"));
    }
}
