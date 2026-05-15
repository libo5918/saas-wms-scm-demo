package com.example.scm.aiagent.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * AI Chat 响应结果。
 */
@Getter
@Setter
public class ChatResponse {

    /**
     * 本次 Agent 调用唯一 ID，用于日志、审计和后续 trace 串联。
     */
    private String runId;

    /**
     * 会话 ID，用于后续多轮对话。
     */
    private String conversationId;

    /**
     * 模型或 mock provider 返回的答案。
     */
    private String answer;

    /**
     * 当前租户 ID。
     */
    private Long tenantId;

    /**
     * 当前用户 ID。
     */
    private Long userId;

    /**
     * 本次路由选中的逻辑模型名称。
     */
    private String modelName;

    /**
     * 本次路由选中的 provider 真实模型名称。
     */
    private String providerModel;

    /**
     * 本次路由选中的模型提供方名称。
     */
    private String provider;

    /**
     * 本次路由选中的模型提供方类型。
     */
    private String providerType;

    /**
     * 本次调用模式，例如 mock 或 spring-ai。
     */
    private String providerMode;

    /**
     * 当前任务类型。
     */
    private String taskType;

    /**
     * 模型路由命中原因。
     */
    private String routeReason;

    /**
     * 当前模型失败时允许降级的模型列表。
     */
    private List<String> fallbackModels;

    /**
     * 本次请求耗时，单位毫秒。
     */
    private long latencyMs;

    /**
     * 响应创建时间。
     */
    private Instant createdAt;
}
