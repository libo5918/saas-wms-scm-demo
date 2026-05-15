package com.example.scm.aiagent;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.ModelRoute;
import com.example.scm.aiagent.model.ModelRouteRequest;
import com.example.scm.aiagent.service.DefaultModelRouter;
import com.example.scm.common.core.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultModelRouterTest {

    @Test
    void shouldRouteByRequestedModel() {
        DefaultModelRouter router = new DefaultModelRouter(new AiAgentProperties());

        ModelRoute route = router.route(new ModelRouteRequest(1L, 10001L,
                "simple_chat", "qwen-turbo", "mock", List.of("CHAT"), null, null));

        assertEquals("qwen-turbo", route.modelName());
        assertEquals("dashscope", route.provider());
        assertEquals("qwen-turbo", route.providerModel());
        assertEquals("dashscope", route.providerType());
        assertEquals("mock", route.providerMode());
        assertEquals("requested_model", route.reason());
    }

    @Test
    void shouldRouteByTaskType() {
        DefaultModelRouter router = new DefaultModelRouter(new AiAgentProperties());

        ModelRoute route = router.route(new ModelRouteRequest(1L, 10001L,
                "workflow_planning", null, "mock", List.of("PLANNING"), null, null));

        assertEquals("chatgpt-main", route.modelName());
        assertEquals("openai", route.provider());
        assertEquals("task_type:workflow_planning", route.reason());
    }

    @Test
    void shouldRouteByCapabilitiesWhenTaskTypeNotMatched() {
        DefaultModelRouter router = new DefaultModelRouter(new AiAgentProperties());

        ModelRoute route = router.route(new ModelRouteRequest(1L, 10001L,
                "unknown_task", null, "mock", List.of("LOW_COST"), null, null));

        assertEquals("qwen-turbo", route.modelName());
        assertEquals("capabilities", route.reason());
        assertEquals(List.of("chatgpt-main"), route.fallbackModels());
    }

    @Test
    void shouldRespectProviderModeFromRequest() {
        DefaultModelRouter router = new DefaultModelRouter(new AiAgentProperties());

        ModelRoute route = router.route(new ModelRouteRequest(1L, 10001L,
                "simple_chat", null, "spring-ai", List.of("CHAT"), null, null));

        assertEquals("qwen-plus", route.modelName());
        assertEquals("spring-ai", route.providerMode());
    }

    @Test
    void shouldFilterByCostLevel() {
        DefaultModelRouter router = new DefaultModelRouter(new AiAgentProperties());

        ModelRoute route = router.route(new ModelRouteRequest(1L, 10001L,
                "simple_chat", null, "mock", List.of("CHAT"), "low", null));

        assertEquals("qwen-turbo", route.modelName());
        assertEquals("low", route.capabilities().contains("LOW_COST") ? "low" : "unknown");
    }

    @Test
    void shouldRejectUnknownRequestedModel() {
        DefaultModelRouter router = new DefaultModelRouter(new AiAgentProperties());

        assertThrows(BusinessException.class, () -> router.route(new ModelRouteRequest(1L, 10001L,
                "simple_chat", "unknown-model", "mock", List.of("CHAT"), null, null)));
    }
}
