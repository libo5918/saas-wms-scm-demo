package com.example.scm.aiagent.service;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.ChatModelInvocation;
import com.example.scm.aiagent.model.ChatModelResult;
import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Chat 模型调用客户端。
 *
 * <p>默认 mock 模式不访问外部模型，便于本地开发和测试稳定运行。
 * 当路由结果为 spring-ai 模式时，才通过 Spring AI 的 ChatClient 调用真实模型。</p>
 */
@Service
public class RoutingChatModelClient implements ChatModelClient {

    private final AiAgentProperties properties;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    public RoutingChatModelClient(AiAgentProperties properties,
                                  ObjectProvider<ChatClient.Builder> chatClientBuilderProvider) {
        this.properties = properties;
        this.chatClientBuilderProvider = chatClientBuilderProvider;
    }

    @Override
    public ChatModelResult chat(ChatModelInvocation invocation) {
        String providerMode = StringUtils.hasText(invocation.route().providerMode())
                ? invocation.route().providerMode()
                : properties.getProviderMode();
        if ("spring-ai".equalsIgnoreCase(providerMode)) {
            return callSpringAi(invocation);
        }
        if (!"mock".equalsIgnoreCase(providerMode)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(),
                    "Unsupported AI provider mode: " + providerMode);
        }
        return mockAnswer(invocation);
    }

    /**
     * 通过 Spring AI ChatClient 调用真实模型。
     *
     * <p>当前阶段先保留真实 Provider 调用骨架，模型和密钥由 Spring AI 配置负责注入。
     * 后续如果要做到每次请求动态切换底层模型，会继续在这里按 provider 构造专属 ChatModel。</p>
     */
    private ChatModelResult callSpringAi(ChatModelInvocation invocation) {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(),
                    "Spring AI ChatClient.Builder is not configured");
        }
        String answer = builder.build()
                .prompt()
                .system("You are an enterprise SCM/WMS AI agent. "
                        + "Answer with tenant-aware context. "
                        + "Selected logical model: " + invocation.route().modelName()
                        + ", provider model: " + invocation.route().providerModel()
                        + ", provider: " + invocation.route().provider() + ".")
                .user(invocation.message())
                .call()
                .content();
        return new ChatModelResult(answer);
    }

    /**
     * 本地 mock 响应。
     *
     * <p>用于保证单元测试和默认启动不依赖真实 API Key，同时能把路由结果展示出来。</p>
     */
    private ChatModelResult mockAnswer(ChatModelInvocation invocation) {
        String taskType = invocation.taskType() == null ? "simple_chat" : invocation.taskType().toLowerCase(Locale.ROOT);
        String answer = "AI Agent mock response"
                + " | taskType=" + taskType
                + " | tenantId=" + invocation.context().tenantId()
                + " | userId=" + invocation.context().userId()
                + " | model=" + invocation.route().modelName()
                + " | providerModel=" + invocation.route().providerModel()
                + " | provider=" + invocation.route().provider()
                + " | providerType=" + invocation.route().providerType()
                + " | providerMode=" + invocation.route().providerMode()
                + " | fallbackModels=" + invocation.route().fallbackModels()
                + " | message=" + invocation.message();
        return new ChatModelResult(answer);
    }
}
