# AI Agent Codex 工作规则

## 1. 文档目的

本文档定义 Codex 后续如何继续开发 `saas-wms-scm` 的 AI Agent 能力。

目标是让每一次后续开发都能对齐：

- AI Agent 建设路线图
- 当前 Java 微服务架构
- Java AI Agent 岗位面试目标
- 当前项目已有业务、认证、网关、消息和可观测能力

开发 AI Agent 相关功能时，必须同时参考：

- `docs/architecture/ai-agent-roadmap.md`
- `docs` 下已有架构、业务、运维、数据库文档
- 本次任务涉及的模块源码

## 2. 核心原则

不要做空泛 AI Demo。

每个 AI 功能都必须至少连接下面一种真实上下文：

- SCM/WMS 真实业务能力
- 当前项目已有文档
- 当前项目已有运维流程
- 当前项目已有 gateway、auth、tenant、trace、messaging 基础设施

## 3. 标准工作顺序

每次开发 AI Agent 相关任务时，Codex 应按以下顺序执行：

1. 判断当前任务属于路线图中的哪个阶段
2. 阅读相关文档和代码
3. 明确本轮实现边界
4. 只实现当前任务范围内的改动
5. 增加聚焦的测试或明确验证方式
6. 如果影响架构、接口、工作流或运维步骤，同步更新文档
7. 最后说明改了什么、如何验证、下一步是什么

## 4. 模块边界

### 4.1 业务服务

以下模块拥有 SCM/WMS 业务逻辑：

- `scm-mdm`
- `scm-purchase`
- `scm-inventory`
- `scm-sales`

不要把这些模块的领域逻辑复制到 AI 模块。

AI 模块应该通过 service client、gateway route、tools 或 MCP adapter 调用这些能力。

### 4.2 认证与网关

以下模块拥有认证和请求入口能力：

- `scm-auth`
- `scm-gateway`
- `scm-common-security`
- `scm-common-web`

AI 模块必须遵守：

- JWT 认证
- gateway 统一入口
- `tenantId`
- `userId`
- roles
- 内部服务访问规则

### 4.3 AI 模块

规划中的 AI 模块职责如下：

- `scm-ai-agent`：Agent 编排、模型路由、Chat API、任务规划
- `scm-ai-rag`：文档导入、Embedding、检索、引用来源
- `scm-ai-mcp`：MCP tools 和外部 Agent 集成

只有当职责边界非常明确时，才新增其它 AI 模块。

## 5. 技术规则

### 5.1 Spring AI

使用 Spring AI 承载：

- `ChatClient`
- 模型抽象
- Advisors
- Tool Calling
- VectorStore
- Embedding
- MCP 基础集成
- 可观测钩子

### 5.2 Spring AI Alibaba

使用 Spring AI Alibaba 承载：

- DashScope / Qwen 集成
- Agent 增强能力
- Graph / Workflow 能力
- Nacos 相关 AI 集成
- 与本项目匹配的阿里生态能力

### 5.3 Spring Cloud

沿用现有 Spring Cloud 技术栈处理：

- 服务发现
- Nacos 配置
- gateway 路由
- 负载均衡
- 分布式服务集成

### 5.4 消息

使用 Kafka 处理：

- 长任务编排
- 异步 workflow step 事件
- 重试和 DLQ

需要优先复用 `scm-sales` 和 `scm-inventory` 里已有 Kafka / DLQ 设计风格。

### 5.5 向量数据库

RAG 相关开发默认使用 `Milvus Standalone` 作为主线向量数据库。

约束：

- 不要默认回退到 pgvector，除非任务明确要求做对照实现
- 不要把 Pinecone 作为项目主线实现
- RAG metadata 优先放 MySQL，向量检索放 Milvus
- 所有文档向量必须带 `tenantId`、来源文件、chunk 信息等 metadata

详细选型依据见：

```text
docs/architecture/ai-agent-technology-decisions.md
```

## 6. Model Router 规则

不要把整个 Agent 系统写死到某一个模型。

凡是涉及模型调用，都要新增或保留 `Model Router` 抽象。

路由能力应支持：

- 用户显式指定模型
- 任务类型
- 租户策略
- 模型能力要求
- 成本等级
- fallback 模型

第一批模型提供方：

- Qwen through DashScope
- ChatGPT through OpenAI or OpenAI-compatible endpoint

后续可扩展：

- DeepSeek
- Ollama
- 本地 OpenAI-compatible endpoint

## 7. RAG 规则

RAG 必须基于当前项目资料，不做孤立知识库。

初始知识来源：

- `docs/architecture`
- `docs/business`
- `docs/operations`
- `docs/database`

RAG 输出应包含：

- 答案
- 引用文档
- 必要时返回检索分数或排序
- 租户级 metadata

当用户询问项目事实时，回答应尽量能追溯到检索内容。

## 8. Tool Calling 规则

Tools 必须代表真实项目能力。

第一批只读工具：

- 查询库存余额
- 查询库存流水
- 查询销售单
- 查询采购收货单
- 查询物料
- 查询运维 / DLQ 状态

写操作工具必须保护。

写操作规则：

- 必须显式确认
- 必须校验参数
- 必须校验租户
- 必须校验角色
- 必须记录工具调用日志
- 必要时必须保证幂等

## 9. Workflow 规则

Workflow 实现应支持：

- LLM 节点
- 工具节点
- 条件节点
- 人工确认节点
- 结束节点
- 持久化 run 状态
- 持久化 step 状态
- 重试
- 失败原因

第一批 workflow 场景：

- 库存异常诊断
- 销售发货失败分析
- 采购入库失败补偿
- 上线前巡检

不要一开始就做可视化设计器。先做后端 workflow 定义和执行记录。

## 10. Multi-Agent 规则

Multi-Agent 设计必须贴近业务，不做概念堆叠。

第一批 Agent：

- Inventory Diagnosis Agent
- Sales Fulfillment Agent
- Purchase Inbound Agent
- Operations Runbook Agent
- Knowledge QA Agent

每个 Agent 必须明确：

- 职责
- prompt
- 允许调用的 tools
- 允许访问的知识范围
- 输出格式

Orchestrator 应根据意图、租户策略和任务类型进行路由。

## 11. MCP 规则

MCP 应暴露有价值的 SCM/WMS 工具给外部 Agent 客户端。

MCP tools 必须包含：

- 清晰的名称
- 描述
- 输入 schema
- 输出 schema
- 租户校验
- 权限校验

MCP 不允许绕过 gateway、auth 或租户隔离。

## 12. 可观测规则

Agent 功能必须可追踪。

相关功能应尽量记录或暴露：

- `agent_run_id`
- `conversation_id`
- `workflow_run_id`
- `tool_call_id`
- `tenant_id`
- `user_id`
- `model_name`
- `provider`
- `task_type`
- `latency_ms`
- `prompt_tokens`
- `completion_tokens`
- `retrieval_doc_count`
- `fallback_count`
- `error_code`

优先复用项目已有日志和 SkyWalking 设计。

## 13. 测试规则

测试范围应与风险匹配。

最低期望：

- Model Router 选择逻辑测试
- Agent 编排服务测试
- 对外 API Controller 测试
- Tool 参数校验测试
- Workflow 状态流转测试
- RAG 导入与检索边界测试

涉及外部模型调用时，优先使用 mock 或 test adapter。

不要让测试依赖真实付费模型 API。

## 14. 文档规则

新增以下内容时必须更新文档：

- 新模块
- 新 API
- 新 workflow
- 新 Agent
- 新 tool
- 新 MCP 能力
- 新模型提供方
- 新运维流程

文档目录约定：

- 架构设计放 `docs/architecture`
- 业务行为放 `docs/business`
- 运维手册放 `docs/operations`
- 数据库设计放 `docs/database`

如果请求流、工作流或 Agent 协作关系适合画图，优先使用 Mermaid。

## 15. 安全规则

每次设计和实现都要考虑：

- 租户隔离
- 角色权限
- prompt injection
- tool misuse
- 高风险写操作
- secret 泄露
- 模型 provider API key
- 文档访问控制
- MCP 外部访问

敏感信息不能硬编码提交。

应使用配置占位符，并在文档中说明需要哪些环境变量。

## 16. 完成汇报规则

每次实现结束时，Codex 应说明：

- 改了什么
- 关键文件在哪里
- 如何验证
- 哪些内容是本轮刻意不做的
- 下一步最值得做什么

说明要简洁、具体，不要泛泛总结。

## 17. 后续任务标准提示词

后续复杂任务可以使用以下提示词：

```text
请严格按照 docs/architecture/ai-agent-roadmap.md 和 docs/architecture/ai-agent-codex-working-rules.md 推进当前任务。先确认当前处于哪个阶段，再实施，不要跳阶段，不要做空泛 Demo，完成后更新对应文档和测试。
```
