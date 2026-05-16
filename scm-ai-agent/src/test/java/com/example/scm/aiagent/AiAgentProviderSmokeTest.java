package com.example.scm.aiagent;

import com.example.scm.aiagent.config.AiAgentProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI Provider 配置 smoke test。
 *
 * <p>该测试只验证默认 mock 模式和真实 Provider 配置骨架，不访问外部模型，
 * 避免 CI 或本地无 API Key 时失败。</p>
 */
@SpringBootTest(properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.ai.model.chat=none",
        "spring.ai.model.embedding=none",
        "spring.ai.model.image=none",
        "spring.ai.model.audio.speech=none",
        "spring.ai.model.audio.transcription=none",
        "spring.ai.model.moderation=none",
        "spring.ai.model.rerank=none",
        "spring.ai.model.video=none",
        "spring.ai.dashscope.enabled=false",
        "spring.ai.dashscope.chat.enabled=false",
        "ai.agent.provider-mode=mock"
})
class AiAgentProviderSmokeTest {

    @Autowired
    private AiAgentProperties properties;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldStartInMockModeWithoutRealProviderKey() {
        assertEquals("mock", properties.getProviderMode());
        assertEquals(0, applicationContext.getBeanNamesForType(ChatModel.class, false, false).length);
        assertTrue(properties.getProviders().stream()
                .anyMatch(provider -> "dashscope".equals(provider.getName())
                        && "DASHSCOPE_API_KEY".equals(provider.getApiKeyEnv())));
        assertTrue(properties.getProviders().stream()
                .anyMatch(provider -> "openai".equals(provider.getName())
                        && "OPENAI_API_KEY".equals(provider.getApiKeyEnv())));
    }
}
