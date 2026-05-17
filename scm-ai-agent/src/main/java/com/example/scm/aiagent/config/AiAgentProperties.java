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
     * RAG 相关配置。
     * 默认使用 mock embedding 和 in-memory vector store，保证本地启动和单元测试不依赖外部服务。
     */
    private RagProperties rag = new RagProperties();

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

    /**
     * RAG 配置聚合。
     */
    @Getter
    @Setter
    public static class RagProperties {

        /**
         * 文档切片配置。
         */
        private ChunkProperties chunk = new ChunkProperties();

        /**
         * Embedding 配置。
         */
        private EmbeddingProperties embedding = new EmbeddingProperties();

        /**
         * 向量存储配置。
         */
        private VectorStoreProperties vectorStore = new VectorStoreProperties();

        /**
         * 检索配置。
         */
        private RetrievalProperties retrieval = new RetrievalProperties();
    }

    /**
     * 文档切片配置。
     */
    @Getter
    @Setter
    public static class ChunkProperties {

        /**
         * 单个切片最大字符数。
         */
        private int size = 800;

        /**
         * 相邻切片重叠字符数，用于减少上下文断裂。
         */
        private int overlap = 100;
    }

    /**
     * Embedding 配置。
     */
    @Getter
    @Setter
    public static class EmbeddingProperties {

        /**
         * embedding 模式，mock 表示本地确定性向量，spring-ai 预留给真实 EmbeddingModel。
         */
        private String mode = "mock";

        /**
         * mock embedding 向量维度。
         */
        private int dimension = 64;

        /**
         * 真实 embedding 模型名称，后续接入 DashScope/OpenAI embedding 时使用。
         */
        private String model = "mock-embedding";
    }

    /**
     * 向量存储配置。
     */
    @Getter
    @Setter
    public static class VectorStoreProperties {

        /**
         * 向量存储模式，当前默认 in-memory，milvus 为后续真实接入预留。
         */
        private String mode = "in-memory";

        /**
         * Milvus 连接配置。
         */
        private MilvusProperties milvus = new MilvusProperties();
    }

    /**
     * Milvus 连接配置骨架。
     */
    @Getter
    @Setter
    public static class MilvusProperties {

        /**
         * Milvus 服务地址。
         */
        private String uri = "http://localhost:19530";

        /**
         * Milvus token，必须通过环境变量或本地配置注入，不能硬编码真实值。
         */
        private String token;

        /**
         * Milvus collection 名称。
         */
        private String collectionName = "scm_ai_rag_chunks";

        /**
         * 向量字段名称。
         */
        private String vectorField = "embedding";

        /**
         * 相似度度量方式，默认 COSINE。
         */
        private String metricType = "COSINE";
    }

    /**
     * 检索配置。
     */
    @Getter
    @Setter
    public static class RetrievalProperties {

        /**
         * 默认返回切片数量。
         */
        private int defaultTopK = 3;

        /**
         * 单次请求允许返回的最大切片数量。
         */
        private int maxTopK = 10;
    }
}
