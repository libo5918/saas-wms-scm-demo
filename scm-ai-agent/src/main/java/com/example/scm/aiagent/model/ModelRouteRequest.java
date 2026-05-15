package com.example.scm.aiagent.model;

import java.util.List;

/**
 * 模型路由请求上下文。
 *
 * <p>Agent 在真正调用大模型之前，会先把本次请求的租户、用户、任务类型、
 * 显式指定模型、能力要求、成本要求和延迟要求封装到该对象中，再交给
 * {@code ModelRouter} 决定最终使用哪个模型。</p>
 */
public record ModelRouteRequest(
        /**
         * 当前请求所属租户 ID。
         * 后续用于租户级模型策略、成本控制、调用审计和数据隔离。
         */
        Long tenantId,

        /**
         * 当前请求用户 ID。
         * 后续用于用户级限额、行为审计、权限判断和个性化模型策略。
         */
        Long userId,

        /**
         * 当前任务类型。
         * 例如 simple_chat、rag_qa、tool_calling、workflow_planning。
         */
        String taskType,

        /**
         * 调用方显式指定的模型名称。
         * 如果该字段有值，ModelRouter 会优先尝试路由到该模型。
         */
        String requestedModel,

        /**
         * 本次请求期望使用的模型调用模式。
         * mock 表示不访问外部模型，spring-ai 表示通过 Spring AI 调用真实 provider。
         */
        String providerMode,

        /**
         * 本次任务要求模型具备的能力标签。
         * 例如 CHAT、RAG、TOOL_CALLING、STRUCTURED_OUTPUT、PLANNING。
         */
        List<String> requiredCapabilities,

        /**
         * 成本等级偏好。
         * 例如 low、medium、high，后续可用于把普通任务优先路由到低成本模型。
         */
        String costLevel,

        /**
         * 最大可接受延迟，单位毫秒。
         * 后续可用于实时性要求较高的任务路由。
         */
        Long maxLatencyMs
) {
}
