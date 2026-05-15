package com.example.scm.aiagent.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * AI Agent 模块配置。
 *
 * <p>该配置只描述模型提供方、模型能力和路由策略，不保存真实密钥。
 * 真实 API Key 应通过环境变量或配置中心注入，避免提交到代码仓库。</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ai.agent")
public class AiAgentProperties {

    /**
     * 当前模型调用模式。
     * mock 表示只走本地模拟响应；spring-ai 表示通过 Spring AI ChatClient 调用真实模型。
     */
    private String providerMode = "mock";

    /**
     * 默认逻辑模型名称。
     * 当 requestedModel、taskType、capabilities 都无法命中时，路由器优先回退到该模型。
     */
    private String defaultModel = "qwen-plus";

    /**
     * 模型提供方配置，例如 mock、DashScope、OpenAI-compatible、DeepSeek-compatible。
     */
    private List<ProviderProperties> providers = new ArrayList<>();

    /**
     * 逻辑模型配置。
     * 一个逻辑模型会绑定到某个 provider，并声明能力标签、任务类型和 fallback 列表。
     */
    private List<ModelProperties> models = new ArrayList<>();

    /**
     * 模型提供方配置。
     */
    @Getter
    @Setter
    public static class ProviderProperties {

        /**
         * 提供方唯一名称，例如 mock、dashscope、openai、deepseek。
         */
        private String name;

        /**
         * 提供方类型，例如 mock、dashscope、openai-compatible。
         */
        private String type;

        /**
         * 是否启用该提供方。
         */
        private boolean enabled = true;

        /**
         * OpenAI-compatible 兼容接口地址，DashScope 兼容模式和 DeepSeek 都可通过该字段描述。
         */
        private String baseUrl;

        /**
         * API Key 直接配置值。
         * 仅用于本地临时验证，不建议提交真实值。
         */
        private String apiKey;

        /**
         * API Key 对应的环境变量名称，例如 DASHSCOPE_API_KEY、OPENAI_API_KEY。
         */
        private String apiKeyEnv;

        /**
         * 调用超时时间，单位毫秒。
         */
        private long timeoutMs = 30000;
    }

    /**
     * 逻辑模型配置。
     */
    @Getter
    @Setter
    public static class ModelProperties {

        /**
         * 项目内使用的逻辑模型名称，例如 qwen-plus、chatgpt-main、deepseek-chat。
         */
        private String name;

        /**
         * 提供方里的真实模型名称。
         * 逻辑名称可以稳定服务项目，真实模型名称可通过配置替换。
         */
        private String providerModel;

        /**
         * 该模型绑定的 provider 名称。
         */
        private String provider;

        /**
         * 是否启用该模型。
         */
        private boolean enabled = true;

        /**
         * 成本等级，例如 low、medium、high。
         */
        private String costLevel = "medium";

        /**
         * 当前模型支持的调用模式，例如 mock、spring-ai。
         */
        private List<String> providerModes = new ArrayList<>();

        /**
         * 当前模型具备的能力标签，例如 CHAT、RAG、TOOL_CALLING、PLANNING。
         */
        private List<String> capabilities = new ArrayList<>();

        /**
         * 当前模型优先承接的任务类型，例如 simple_chat、summary、workflow_planning。
         */
        private List<String> taskTypes = new ArrayList<>();

        /**
         * 当前模型调用失败时可以降级尝试的逻辑模型名称列表。
         */
        private List<String> fallbackModels = new ArrayList<>();

        /**
         * 路由优先级，数值越小优先级越高。
         */
        private int priority = 100;

        /**
         * 当前模型期望最大延迟，单位毫秒。
         * 后续可用于实时任务过滤高延迟模型。
         */
        private Long maxLatencyMs;
    }
}
