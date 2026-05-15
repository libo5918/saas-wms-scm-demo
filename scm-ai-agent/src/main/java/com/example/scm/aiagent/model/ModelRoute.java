package com.example.scm.aiagent.model;

import java.util.List;

/**
 * 模型路由结果。
 *
 * <p>ModelRouter 根据请求上下文选定模型后，会把模型、provider、能力标签、
 * fallback 列表和路由原因封装到这里，供后续模型调用和审计记录使用。</p>
 */
public record ModelRoute(
        /**
         * 项目内使用的逻辑模型名称，例如 qwen-plus、chatgpt-main。
         */
        String modelName,

        /**
         * 提供方里的真实模型名称，例如 qwen-plus、deepseek-chat。
         */
        String providerModel,

        /**
         * 模型提供方名称，例如 dashscope、openai、deepseek。
         */
        String provider,

        /**
         * 模型提供方类型，例如 dashscope、openai-compatible、mock。
         */
        String providerType,

        /**
         * 本次调用模式，例如 mock 或 spring-ai。
         */
        String providerMode,

        /**
         * 路由命中的原因，例如 requested_model、task_type:simple_chat、capabilities。
         */
        String reason,

        /**
         * 当前模型支持的能力标签。
         */
        List<String> capabilities,

        /**
         * 当前模型失败后允许降级尝试的模型列表。
         */
        List<String> fallbackModels
) {
}
