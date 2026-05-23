package com.example.scm.aiagent.config;

import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationPlanMode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * AI Agent 模块总配置。
 *
 * <p>集中描述模型路由、RAG、Tools、Tool Calling 等能力的静态配置结构。
 * API Key、数据库密码等敏感信息必须通过环境变量或本地配置注入，不能直接写入仓库。</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ai.agent")
public class AiAgentProperties {

    /** 模型调用模式，当前支持 mock 和 spring-ai。 */
    private String providerMode = "mock";

    /** 路由兜底时使用的默认逻辑模型名称。 */
    private String defaultModel = "qwen-plus";

    /** 模型提供方配置，例如 mock、dashscope、openai、deepseek。 */
    private List<ProviderProperties> providers = new ArrayList<>();

    /** 逻辑模型配置，声明模型能力、任务类型和 fallback 关系。 */
    private List<ModelProperties> models = new ArrayList<>();

    /** RAG 相关配置。 */
    private RagProperties rag = new RagProperties();

    /** Agent Tools 相关配置。 */
    private ToolsProperties tools = new ToolsProperties();

    /** Tool Calling Chat 相关配置。 */
    private ToolCallingProperties toolCalling = new ToolCallingProperties();

    /** 单个模型提供方配置。 */
    @Getter
    @Setter
    public static class ProviderProperties {

        /** 提供方名称，例如 mock、dashscope、openai。 */
        private String name;

        /** 提供方类型，例如 openai-compatible。 */
        private String type;

        /** 是否启用该提供方。 */
        private boolean enabled = true;

        /** 兼容 OpenAI 协议时使用的基础地址。 */
        private String baseUrl;

        /** 本地临时调试时可直接注入的 API Key。 */
        private String apiKey;

        /** API Key 对应的环境变量名。 */
        private String apiKeyEnv;

        /** 调用超时时间，单位毫秒。 */
        private long timeoutMs = 30000;
    }

    /** 逻辑模型配置。 */
    @Getter
    @Setter
    public static class ModelProperties {

        /** 项目内部使用的逻辑模型名称。 */
        private String name;

        /** 提供方侧真实模型名称。 */
        private String providerModel;

        /** 该模型所属提供方名称。 */
        private String provider;

        /** 是否启用该模型。 */
        private boolean enabled = true;

        /** 成本等级，用于后续路由权衡。 */
        private String costLevel = "medium";

        /** 该模型允许的 providerMode 列表。 */
        private List<String> providerModes = new ArrayList<>();

        /** 模型能力标签，例如 CHAT、RAG、TOOL_CALLING。 */
        private List<String> capabilities = new ArrayList<>();

        /** 模型优先承接的任务类型。 */
        private List<String> taskTypes = new ArrayList<>();

        /** 当前模型失败后的降级模型列表。 */
        private List<String> fallbackModels = new ArrayList<>();

        /** 路由优先级，数值越小优先级越高。 */
        private int priority = 100;

        /** 允许的最大延迟，用于后续路由约束。 */
        private Long maxLatencyMs;
    }

    /** RAG 聚合配置。 */
    @Getter
    @Setter
    public static class RagProperties {

        /** 文档切片配置。 */
        private ChunkProperties chunk = new ChunkProperties();

        /** Embedding 配置。 */
        private EmbeddingProperties embedding = new EmbeddingProperties();

        /** 向量存储配置。 */
        private VectorStoreProperties vectorStore = new VectorStoreProperties();

        /** 检索配置。 */
        private RetrievalProperties retrieval = new RetrievalProperties();

        /** docs 目录导入配置。 */
        private DocsImportProperties docsImport = new DocsImportProperties();

        /** RAG 文档治理元数据配置。 */
        private RegistryProperties registry = new RegistryProperties();
    }

    /** 文档切片参数。 */
    @Getter
    @Setter
    public static class ChunkProperties {

        /** 单个切片的最大字符数。 */
        private int size = 800;

        /** 相邻切片重叠字符数，用于减少上下文断裂。 */
        private int overlap = 100;
    }

    /** Embedding 配置。 */
    @Getter
    @Setter
    public static class EmbeddingProperties {

        /** embedding 模式，当前支持 mock、dashscope、openai-compatible。 */
        private String mode = "mock";

        /** 向量维度。 */
        private int dimension = 64;

        /** embedding 模型名称。 */
        private String model = "mock-embedding";

        /** embedding provider 名称。 */
        private String provider = "mock";

        /** OpenAI-compatible embedding 基础地址。 */
        private String baseUrl;

        /** embedding API Key 对应的环境变量名。 */
        private String apiKeyEnv;

        /** 是否预留 rerank 扩展点。 */
        private boolean rerankEnabled = false;
    }

    /** 向量存储配置。 */
    @Getter
    @Setter
    public static class VectorStoreProperties {

        /** 向量存储模式，当前支持 in-memory 和 milvus。 */
        private String mode = "in-memory";

        /** Milvus 连接配置。 */
        private MilvusProperties milvus = new MilvusProperties();
    }

    /** Milvus 配置骨架。 */
    @Getter
    @Setter
    public static class MilvusProperties {

        /** Milvus 服务地址。 */
        private String uri = "http://localhost:19530";

        /** Milvus 访问令牌。 */
        private String token;

        /** Collection 名称。 */
        private String collectionName = "scm_ai_rag_chunks";

        /** 主键字段名。 */
        private String primaryField = "chunk_id";

        /** 向量字段名。 */
        private String vectorField = "embedding";

        /** 相似度计算方式。 */
        private String metricType = "COSINE";

        /** 索引类型。 */
        private String indexType = "AUTOINDEX";
    }

    /** 检索参数配置。 */
    @Getter
    @Setter
    public static class RetrievalProperties {

        /** 默认返回的 topK。 */
        private int defaultTopK = 3;

        /** 接口允许的最大 topK。 */
        private int maxTopK = 10;

        /** 最低分数阈值，0 表示默认不过滤。 */
        private double scoreThreshold = 0.0;

        /** RAG Chat 最多拼接的上下文 chunk 数量。 */
        private int maxContextChunks = 5;

        /** 单个上下文 chunk 允许的最大字符数。 */
        private int maxContextChunkLength = 1200;
    }

    /** docs 目录导入配置。 */
    @Getter
    @Setter
    public static class DocsImportProperties {

        /** 是否启用 docs 导入能力。 */
        private boolean enabled = true;

        /** docs 根目录。 */
        private String rootPath = "docs";

        /** 默认写入的知识库 ID。 */
        private String knowledgeBaseId = "kb-project-docs";

        /** 默认扫描的子目录。 */
        private List<String> includeDirectories = new ArrayList<>(
                List.of("architecture", "business", "operations", "database","examples","interview"));

        /** 支持导入的文件后缀。 */
        private List<String> supportedExtensions = new ArrayList<>(List.of(".md"));

        /** 单次最大导入文件数。 */
        private int maxFiles = 100;
    }

    /** RAG Document Registry 配置。 */
    @Getter
    @Setter
    public static class RegistryProperties {

        /** Registry 存储模式，当前支持 in-memory 和 mysql。 */
        private String mode = "mysql";

        /** MySQL Registry 连接配置。 */
        private MysqlRegistryProperties mysql = new MysqlRegistryProperties();
    }

    /** MySQL Registry 连接信息。 */
    @Getter
    @Setter
    public static class MysqlRegistryProperties {

        /** JDBC 地址。 */
        private String url;

        /** 数据库用户名。 */
        private String username;

        /** 数据库密码。 */
        private String password;
    }

    /** Agent Tools 配置。 */
    @Getter
    @Setter
    public static class ToolsProperties {

        /** Tool 适配模式，当前支持 mock 和 http。 */
        private String adapterMode = "mock";

        /** HTTP ToolClient 连接配置。 */
        private HttpToolClientProperties http = new HttpToolClientProperties();

        /** Tool 调用审计配置。 */
        private AuditProperties audit = new AuditProperties();

        /** Tool 权限治理配置。 */
        private AccessControlProperties accessControl = new AccessControlProperties();

        /** Tool 运行时保护配置。 */
        private RuntimeProperties runtime = new RuntimeProperties();
    }

    /** Tool HTTP 客户端配置。 */
    @Getter
    @Setter
    public static class HttpToolClientProperties {

        /** 库存服务基础地址。 */
        private String inventoryBaseUrl = "http://localhost:18084";

        /** 主数据服务基础地址。 */
        private String mdmBaseUrl = "http://localhost:18082";

        /** 销售服务基础地址。 */
        private String salesBaseUrl = "http://localhost:18085";

        /** 采购服务基础地址。 */
        private String purchaseBaseUrl = "http://localhost:18083";

        /** HTTP 连接超时，单位毫秒。 */
        private int connectTimeoutMs = 3000;

        /** HTTP 读取超时，单位毫秒。 */
        private int readTimeoutMs = 5000;
    }

    /** Tool 调用审计配置。 */
    @Getter
    @Setter
    public static class AuditProperties {

        /** 审计存储模式，当前默认 in-memory，后续可扩展 mysql。 */
        private String mode = "in-memory";

        /** 内存模式下最多保留的审计记录数。 */
        private int maxRecords = 500;
    }

    /** Tool 权限治理配置。 */
    @Getter
    @Setter
    public static class AccessControlProperties {

        /** 是否开启严格权限校验；默认关闭，避免影响本地只读联调。 */
        private boolean strictEnabled = false;

        /** 严格模式关闭时，是否默认放行只读 Tool。 */
        private boolean defaultAllowReadOnly = true;

        /** 可绕过权限标签校验的管理员角色。 */
        private List<String> adminRoles = new ArrayList<>(List.of("ROLE_ADMIN"));
    }

    /** Tool 运行时保护配置。 */
    @Getter
    @Setter
    public static class RuntimeProperties {

        /** 单次 Tool 执行超时阈值，当前用于日志和超时语义保护。 */
        private long timeoutMs = 5000;

        /** 是否对可重试异常执行重试。 */
        private boolean retryEnabled = true;

        /** 可重试异常的最大重试次数，不包含首次调用。 */
        private int maxRetries = 1;

        /** 是否开启轻量熔断；Phase 4.10 仅完成配置预留。 */
        private boolean circuitBreakerEnabled = false;

        /** 轻量熔断失败阈值；Phase 4.10 仅完成配置预留。 */
        private int failureThreshold = 5;

        /** 熔断打开时长；Phase 4.10 仅完成配置预留。 */
        private long openDurationMs = 30000;
    }

    /** Tool Calling Chat 配置。 */
    @Getter
    @Setter
    public static class ToolCallingProperties {

        /** planner 模式，当前支持 mock 和 spring-ai。 */
        private String plannerMode = "mock";

        /** answer 模式，当前支持 template 和 spring-ai。 */
        private String answerMode = "template";

        /** Spring AI Planner 细分配置。 */
        private SpringAiPlannerProperties springAiPlanner = new SpringAiPlannerProperties();

        /** Spring AI Answer 细分配置。 */
        private SpringAiAnswerProperties springAiAnswer = new SpringAiAnswerProperties();

        /** Orchestrator 单步运行记录配置。 */
        private OrchestratorProperties orchestrator = new OrchestratorProperties();
    }

    /** Spring AI Planner 配置。 */
    @Getter
    @Setter
    public static class SpringAiPlannerProperties {

        /** 是否启用真实 Spring AI Planner。 */
        private boolean enabled = false;

        /** 真实模型规划失败时是否回退到 mock planner。 */
        private boolean fallbackToMock = true;

        /** 规划阶段最大重试次数。 */
        private int maxRetries = 1;

        /** 用于模型路由的任务类型。 */
        private String taskType = "tool_calling";
    }

    /** Spring AI Answer 配置。 */
    @Getter
    @Setter
    public static class SpringAiAnswerProperties {

        /** 是否启用真实 Spring AI 总结答案。 */
        private boolean enabled = false;

        /** 总结答案失败时是否回退到模板答案。 */
        private boolean fallbackToTemplate = true;

        /** 总结答案阶段最大重试次数。 */
        private int maxRetries = 1;

        /** 用于答案总结模型路由的任务类型。 */
        private String taskType = "tool_calling_answer";
    }

    /** Tool Calling Orchestrator 配置。 */
    @Getter
    @Setter
    public static class OrchestratorProperties {

        /** 是否启用 Orchestrator 单步记录；默认关闭，保持 Phase 4.11 行为。 */
        private boolean enabled = false;

        /** 是否记录 run/step 状态；关闭后 Orchestrator 不保留调试状态。 */
        private boolean recordRuns = true;

        /** in-memory run store 最多保留的记录数。 */
        private int maxRecords = 100;

        /** Orchestration plan 模式，默认只构造单步计划。 */
        private ToolOrchestrationPlanMode planMode = ToolOrchestrationPlanMode.SINGLE_STEP;

        /** 单次 run 允许表达的最大步骤数，默认 1。 */
        private int maxSteps = 1;

        /** 是否允许受控多步骤计划；默认关闭，不执行多 Tool 编排。 */
        private boolean multiStepEnabled = false;

        /** 是否允许 dry-run 多步骤计划；开启后后续步骤只记录为 SKIPPED。 */
        private boolean dryRunEnabled = false;

        /** 是否允许 controlled 模式真实执行受控后续步骤；默认关闭，保持单步主路径。 */
        private boolean controlledExecutionEnabled = false;

        /** 单次 run 最多允许真实执行的步骤数；默认 1，Phase 4.15 显式配置下最大建议 2。 */
        private int maxExecutableSteps = 1;

        /** 是否允许第二步执行只读 Tool；默认允许，但仍受 controlledExecutionEnabled 和 maxExecutableSteps 约束。 */
        private boolean allowSecondStepReadOnly = true;
    }
}
