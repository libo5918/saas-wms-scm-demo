package com.example.scm.aiagent.service;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.dto.ChatRequest;
import com.example.scm.aiagent.dto.ChatResponse;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.model.ChatModelInvocation;
import com.example.scm.aiagent.model.ChatModelResult;
import com.example.scm.aiagent.model.ModelRoute;
import com.example.scm.aiagent.model.ModelRouteRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AgentChatService {

    private final AiAgentProperties properties;
    private final ModelRouter modelRouter;
    private final ChatModelClient chatModelClient;

    public AgentChatService(AiAgentProperties properties, ModelRouter modelRouter, ChatModelClient chatModelClient) {
        this.properties = properties;
        this.modelRouter = modelRouter;
        this.chatModelClient = chatModelClient;
    }

    /**
     * 处理一次 Chat 请求。
     *
     * <p>该方法负责生成 runId、整理租户用户上下文、调用 ModelRouter 选择模型，
     * 再把最终调用交给 ChatModelClient。后续审计、trace 和调用记录也会围绕 runId 串联。</p>
     */
    public ChatResponse chat(ChatRequest request, AgentRequestContext context) {
        long startedAt = System.nanoTime();
        String runId = UUID.randomUUID().toString();
        String taskType = StringUtils.hasText(request.getTaskType()) ? request.getTaskType() : "simple_chat";
        String providerMode = StringUtils.hasText(request.getProviderMode())
                ? request.getProviderMode()
                : properties.getProviderMode();
        List<String> requiredCapabilities = request.getRequiredCapabilities() == null
                || request.getRequiredCapabilities().isEmpty()
                ? List.of("CHAT")
                : request.getRequiredCapabilities();
        String conversationId = StringUtils.hasText(request.getConversationId())
                ? request.getConversationId()
                : UUID.randomUUID().toString();

        ModelRoute route = modelRouter.route(new ModelRouteRequest(
                context.tenantId(),
                context.userId(),
                taskType,
                request.getRequestedModel(),
                providerMode,
                requiredCapabilities,
                request.getCostLevel(),
                request.getMaxLatencyMs()
        ));
        ChatModelResult modelResult = chatModelClient.chat(new ChatModelInvocation(
                runId,
                request.getMessage(),
                taskType,
                context,
                route
        ));

        ChatResponse response = new ChatResponse();
        response.setRunId(runId);
        response.setConversationId(conversationId);
        response.setAnswer(modelResult.answer());
        response.setTenantId(context.tenantId());
        response.setUserId(context.userId());
        response.setModelName(route.modelName());
        response.setProviderModel(route.providerModel());
        response.setProvider(route.provider());
        response.setProviderType(route.providerType());
        response.setProviderMode(route.providerMode());
        response.setTaskType(taskType);
        response.setRouteReason(route.reason());
        response.setFallbackModels(route.fallbackModels());
        response.setLatencyMs((System.nanoTime() - startedAt) / 1_000_000);
        response.setCreatedAt(Instant.now());
        return response;
    }
}
