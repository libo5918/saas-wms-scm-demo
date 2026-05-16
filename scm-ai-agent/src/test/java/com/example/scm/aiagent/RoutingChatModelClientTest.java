package com.example.scm.aiagent;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.model.ChatModelInvocation;
import com.example.scm.aiagent.model.ChatModelResult;
import com.example.scm.aiagent.model.ModelRoute;
import com.example.scm.aiagent.service.RoutingChatModelClient;
import com.example.scm.common.core.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutingChatModelClientTest {

    @Test
    void shouldUseMockModeWithoutRealApiKey() {
        RoutingChatModelClient client = new RoutingChatModelClient(new AiAgentProperties(),
                emptyChatClientBuilder(), new MockEnvironment());

        ChatModelResult result = client.chat(new ChatModelInvocation(
                "run-1",
                "hello",
                "simple_chat",
                new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN")),
                route("mock")
        ));

        assertTrue(result.answer().contains("AI Agent mock response"));
        assertTrue(result.answer().contains("providerMode=mock"));
        assertTrue(result.answer().contains("providerModel=qwen-plus"));
    }

    @Test
    void shouldRequireChatClientBuilderWhenUsingSpringAiMode() {
        RoutingChatModelClient client = new RoutingChatModelClient(new AiAgentProperties(),
                emptyChatClientBuilder(), new MockEnvironment());

        assertThrows(BusinessException.class, () -> client.chat(new ChatModelInvocation(
                "run-1",
                "hello",
                "simple_chat",
                new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN")),
                route("spring-ai")
        )));
    }

    private ModelRoute route(String providerMode) {
        return new ModelRoute(
                "qwen-plus",
                "qwen-plus",
                "dashscope",
                "dashscope",
                providerMode,
                "default_model",
                List.of("CHAT"),
                List.of("qwen-turbo")
        );
    }

    private ObjectProvider<ChatClient.Builder> emptyChatClientBuilder() {
        return new ObjectProvider<>() {
            @Override
            public ChatClient.Builder getObject() {
                return null;
            }
        };
    }
}
