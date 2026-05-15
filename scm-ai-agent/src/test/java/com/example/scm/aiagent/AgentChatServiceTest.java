package com.example.scm.aiagent;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.dto.ChatRequest;
import com.example.scm.aiagent.dto.ChatResponse;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.model.ChatModelResult;
import com.example.scm.aiagent.model.ModelRoute;
import com.example.scm.aiagent.service.AgentChatService;
import com.example.scm.aiagent.service.ChatModelClient;
import com.example.scm.aiagent.service.ModelRouter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AgentChatServiceTest {

    @Test
    void shouldReturnChatResponseWithRouteAndTenantContext() {
        ModelRouter router = request -> new ModelRoute("qwen-plus", "qwen-plus", "dashscope",
                "dashscope", "mock", "default_model", List.of("CHAT"), List.of("qwen-turbo"));
        ChatModelClient client = invocation -> new ChatModelResult("hello from agent");
        AgentChatService service = new AgentChatService(new AiAgentProperties(), router, client);
        ChatRequest request = new ChatRequest();
        request.setMessage("hello");

        ChatResponse response = service.chat(request,
                new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN")));

        assertNotNull(response.getRunId());
        assertNotNull(response.getConversationId());
        assertEquals("hello from agent", response.getAnswer());
        assertEquals(1L, response.getTenantId());
        assertEquals(10001L, response.getUserId());
        assertEquals("qwen-plus", response.getModelName());
        assertEquals("qwen-plus", response.getProviderModel());
        assertEquals("dashscope", response.getProvider());
        assertEquals("dashscope", response.getProviderType());
        assertEquals("mock", response.getProviderMode());
        assertEquals("simple_chat", response.getTaskType());
    }
}
