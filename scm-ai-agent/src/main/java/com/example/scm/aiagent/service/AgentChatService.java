package com.example.scm.aiagent.service;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.dto.ChatRequest;
import com.example.scm.aiagent.dto.ChatResponse;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.model.ChatModelInvocation;
import com.example.scm.aiagent.model.ChatModelResult;
import com.example.scm.aiagent.model.ModelRoute;
import com.example.scm.aiagent.model.ModelRouteRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Agent Chat 应用服务。
 *
 * <p>负责整理请求上下文、调用模型路由器和模型客户端，是 Chat API 的核心业务入口。</p>
 */
@Slf4j
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

        log.info("AI chat request received, runId={}, tenantId={}, userId={}, taskType={}, providerMode={}, messageLength={}",
                runId, context.tenantId(), context.userId(), taskType, providerMode, safeLength(request.getMessage()));

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
        log.info("AI model routed, runId={}, tenantId={}, userId={}, modelName={}, providerModel={}, provider={}, providerType={}, providerMode={}, routeReason={}",
                runId, context.tenantId(), context.userId(), route.modelName(), route.providerModel(),
                route.provider(), route.providerType(), route.providerMode(), route.reason());

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
        log.info("AI chat finished, runId={}, tenantId={}, userId={}, modelName={}, provider={}, providerMode={}, latencyMs={}",
                runId, context.tenantId(), context.userId(), route.modelName(), route.provider(),
                route.providerMode(), response.getLatencyMs());
        return response;
    }

    private int safeLength(String value) {
        return value == null ? 0 : value.length();
    }
}
