# AI Agent 技术选型决策

## 1. 文档目的

本文档记录 `saas-wms-scm` AI Agent 方向的关键技术选型，避免后续开发过程中在模型、向量数据库、工作流、异步任务和可观测方案上反复摇摆。

本文档是 `docs/architecture/ai-agent-roadmap.md` 的补充文档。后续开发 AI Agent 相关能力时，应优先遵循本文档中的选型结论。

## 2. 总体选型结论

当前项目第一版 AI Agent 主线选型如下：

```text
LLM Framework: Spring AI + Spring AI Alibaba
模型接入: Qwen DashScope + OpenAI-compatible
模型路由: 自研 Model Router
向量数据库: Milvus Standalone
RAG metadata: MySQL
工作流: 自研轻量 Workflow 状态机
长任务: Kafka + MySQL 状态表
Agent 状态: MySQL
短期缓存: Redis 可选
MCP: Spring AI MCP Server
外部编排: Dify 作为演示集成
观测: SkyWalking + Micrometer + Actuator
入口: scm-gateway
认证: 复用 JWT + tenantId/userId/roles
```

## 3. 向量数据库选型

### 3.1 最终选择

第一版主线选择：

```text
Milvus Standalone
```

保留对照方案：

```text
PostgreSQL + pgvector
```

了解但不作为主线：

```text
Pinecone
```

### 3.2 为什么选择 Milvus

Milvus 更适合当前项目目标：

- 开源，可本地部署，适合写入项目和 GitHub
- 支持从 Standalone 到 Distributed 的生产演进路径
- 更适合企业私有化、内网部署和微服务集成场景
- 能体现 Java 后端工程能力，而不仅是调用托管云服务
- 适合讲清楚 collection、schema、index、topK、metadata filter、召回性能等 AI 工程细节
- 比 Pinecone 更适合国内开发环境和面试演示环境

### 3.3 为什么不把 Pinecone 作为主线

Pinecone 是优秀的托管向量数据库，但当前项目不把它作为主线，原因是：

- 它更偏海外云托管服务
- 国内网络、账号、付费和稳定性不一定适合个人长期演示
- 不利于展示私有化部署、索引治理、资源运维等后端工程能力
- 面试中容易变成“买云服务”，而不是展示自己对向量数据库工程化的理解

Pinecone 可以作为对比方案了解，用于说明 managed vector database 和 serverless vector database 的产品形态。

### 3.4 pgvector 在项目中的定位

你已经使用过 PostgreSQL + pgvector，因此它适合作为对照方案。

面试时可以这样表达：

```text
我先用 pgvector 理解了向量检索和 RAG 的基础链路；在这个项目中进一步引入 Milvus，是为了展示专用向量数据库在大规模文档、metadata filter、索引策略、水平扩展和 AI Agent 工程化中的价值。
```

### 3.5 Milvus 第一版使用范围

第一版只使用 Milvus Standalone，不直接上 Distributed。

第一版需要掌握并落地：

- collection
- schema
- vector field
- scalar metadata
- index
- similarity metric
- topK
- metadata filter

后续作为生产扩展讲解：

- Milvus Distributed
- etcd
- MinIO / object storage
- Proxy
- QueryNode
- DataNode
- IndexNode

## 4. 模型接入选型

### 4.1 最终选择

第一版支持：

- Qwen through DashScope
- ChatGPT / DeepSeek through OpenAI-compatible endpoint

### 4.2 设计原则

不要把 Agent 系统绑定到单一模型。

所有模型调用都必须经过 `Model Router`，由它根据任务类型、租户策略、模型能力、成本和 fallback 规则选择模型。

第一版至少支持：

- 显式指定模型
- 默认模型
- 按任务类型路由
- 按模型能力标签路由
- 按 providerMode 区分 mock 和真实调用骨架
- fallback 降级

当前模型配置命名约定：

- `qwen-plus`：默认模型，Provider 为 DashScope
- `qwen-turbo`：低成本模型，Provider 为 DashScope
- `chatgpt-main`：高质量规划模型，Provider 为 OpenAI-compatible
- `deepseek-chat`：复杂推理和代码推理预留模型，Provider 为 OpenAI-compatible

默认运行模式为 `mock`，保证本地启动、单元测试和 CI 不依赖真实 API Key。
真实模型调用切换到 `spring-ai` 模式后，应通过环境变量或配置中心注入 Provider API Key。

### 4.3 真实 Provider Smoke Test 选型

真实模型 smoke test 使用以下依赖：

- `org.springframework.ai:spring-ai-starter-model-openai`
- `com.alibaba.cloud.ai:spring-ai-alibaba-starter-dashscope`

默认配置必须关闭真实模型：

```text
spring.ai.model.chat=none
ai.agent.provider-mode=mock
```

DashScope smoke test 启用方式：

```text
AI_AGENT_SPRING_AI_MODEL_CHAT=dashscope
AI_AGENT_DASHSCOPE_ENABLED=true
AI_AGENT_DASHSCOPE_CHAT_ENABLED=true
DASHSCOPE_API_KEY=本地环境变量
```

OpenAI-compatible smoke test 启用方式：

```text
AI_AGENT_SPRING_AI_MODEL_CHAT=openai
OPENAI_API_KEY=本地环境变量
AI_AGENT_OPENAI_BASE_URL=兼容接口地址
```

当前阶段一个进程只建议启用一个真实 ChatModel。后续如果要做到同一进程内按请求动态切换多个 Provider 的 baseUrl，需要新增 Provider Client Registry 或 Provider 专属 `ChatModel` 工厂。

## 5. Spring AI 与 Spring AI Alibaba 选型

### 5.1 最终选择

两者同时使用：

- Spring AI 作为标准 AI 抽象层
- Spring AI Alibaba 作为 Agent、DashScope、Graph、Workflow 和 Nacos 生态增强层

### 5.2 使用边界

Spring AI 负责：

- `ChatClient`
- Tool Calling
- VectorStore
- Embedding
- Advisors
- MCP 基础能力
- Observability hooks

Spring AI Alibaba 负责：

- DashScope / Qwen
- Agent 增强
- Graph / Workflow
- Nacos 相关集成
- 阿里生态能力

## 6. RAG 存储选型

### 6.1 最终选择

RAG 主存储拆成两部分：

- Milvus：存储向量和可检索字段
- MySQL：存储文档、分片、导入任务、租户、来源文件、引用信息等 metadata

### 6.2 第一版数据范围

第一版导入以下目录：

- `docs/architecture`
- `docs/business`
- `docs/operations`
- `docs/database`

第一版必须支持：

- 文档分片
- Embedding
- 向量检索
- 引用来源
- tenant metadata
- 文档重新导入

## 7. Workflow 选型

### 7.1 最终选择

第一版使用自研轻量 Workflow 状态机。

不优先引入：

- Flowable
- Temporal
- Dify Workflow 作为核心实现
- 可视化流程设计器

### 7.2 原因

自研轻量 Workflow 更适合当前项目：

- 学习成本可控
- 面试时能讲清楚底层状态流转
- 更容易结合 Agent step、tool call、human confirmation
- 不会过早被外部工作流引擎复杂度拖住

后续可以把 Dify 或 Temporal 作为对比方案。

## 8. 长任务与异步编排选型

### 8.1 最终选择

```text
Kafka + MySQL 状态表
```

Kafka 负责事件驱动：

- 任务提交
- 步骤完成
- 任务失败
- 任务完成
- DLQ

MySQL 负责状态持久化：

- Agent run
- Agent step
- Workflow run
- Workflow step
- Tool call
- Retry record

### 8.2 原因

当前项目已经有 Kafka、Outbox、DLQ、补偿相关设计，AI Agent 长任务应复用这套经验，而不是另起一套完全不同的异步体系。

## 9. MCP 选型

### 9.1 最终选择

新增独立模块：

```text
scm-ai-mcp
```

使用 Spring AI MCP Server 暴露工具能力。

### 9.2 第一批 MCP tools

第一版只开放只读工具：

- `inventory.getBalance`
- `inventory.listTxnRecords`
- `sales.getOrder`
- `purchase.getReceipt`
- `mdm.getMaterial`
- `ops.getDlqSummary`

写操作工具必须等权限、确认、幂等和审计机制明确后再开放。

## 10. Dify 选型

### 10.1 最终选择

Dify 作为外部演示和对接平台，不作为 Java Agent 核心实现。

### 10.2 原因

当前目标是 Java AI Agent 岗位，因此核心能力必须在 Java 项目中实现。

Dify 的定位：

- 演示外部平台如何调用本项目 MCP tools
- 演示外部 workflow 如何编排 SCM 工具
- 作为面试中“我了解主流 Agent 平台，也能和 Java 后端结合”的补充

## 11. 可观测选型

### 11.1 最终选择

沿用当前项目方向：

- Spring Boot Actuator
- Micrometer
- SkyWalking

### 11.2 Agent 需要补充的观测字段

- `agent_run_id`
- `conversation_id`
- `workflow_run_id`
- `tool_call_id`
- `model_name`
- `provider`
- `task_type`
- `latency_ms`
- `prompt_tokens`
- `completion_tokens`
- `retrieval_doc_count`
- `fallback_count`
- `error_code`

## 12. 安全与租户选型

AI Agent 模块必须复用当前认证链路：

- gateway 统一入口
- JWT
- `tenantId`
- `userId`
- roles

所有 RAG 文档、tool 调用、MCP tools、workflow run 都必须带租户上下文。

写操作必须具备：

- 权限校验
- 显式确认
- 幂等控制
- 审计记录

## 13. 当前决策摘要

当前最重要的决策如下：

1. 向量数据库主线选 Milvus Standalone
2. pgvector 作为对照方案保留，不作为主线
3. Pinecone 只做了解和面试对比，不接入项目主线
4. 模型必须通过 Model Router 动态选择
5. 第一版模型支持 Qwen 和 OpenAI-compatible
6. Workflow 第一版自研轻量状态机
7. 长任务使用 Kafka + MySQL 状态表
8. MCP 独立成 `scm-ai-mcp`
9. Dify 只做外部演示，不替代 Java Agent 实现
10. 所有 AI 能力必须继承当前 gateway / auth / tenant 安全边界

## 14. Phase 3 RAG 基础能力落地决策

当前 RAG 第一阶段采用以下落地策略：

```text
Embedding: mock deterministic embedding
VectorStore: in-memory
Future VectorStore: Milvus Standalone
Metadata: 当前内存承载，后续进入 MySQL
```

这样设计的原因：

- 保证默认启动和单元测试不依赖真实 Milvus。
- 保证 CI 不依赖真实 Embedding API 或外部网络。
- 先验证文档切片、租户隔离、检索和 RAG Chat 编排链路。
- 保留 Milvus 主线方向，后续通过 `MilvusRagVectorStore` 替换当前 in-memory 实现。

Milvus 相关配置只允许通过环境变量或本地配置注入，不允许硬编码真实 token。

详细说明见：

```text
docs/architecture/ai-agent-phase3-rag-basic.md
```