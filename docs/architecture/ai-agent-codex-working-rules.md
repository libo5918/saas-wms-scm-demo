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
8. 每个阶段的代码完成后，需要同时提供一份可直接用于 Git 提交的中文 commit 文案

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

### 5.6 Lombok 使用规则

后续新增 Java 数据承载类时，优先使用 Lombok，避免重复编写 getter、setter、构造方法和日志字段。

默认适用范围：

- entity
- DTO
- VO
- Command
- Query
- PO
- 配置属性类
- 简单事件对象

推荐规则：

- 普通可变数据对象优先使用 `@Getter` 和 `@Setter`
- 需要日志的 service、controller、job、consumer 优先使用 `@Slf4j`
- 值对象可按需要使用 `@Getter`、`@AllArgsConstructor`、`@EqualsAndHashCode`
- 需要链式创建或测试构造便利时，可使用 `@Builder`
- 不要为了省事在所有类上无脑使用 `@Data`
- 涉及 JPA/MyBatis/JSON 反序列化时，按框架需要补充无参构造
- 如果当前模块还没有引入 Lombok，新增 Lombok 注解前必须先补齐该模块依赖并确保 Maven 测试通过

例外情况：

- record 更适合表达不可变简单返回结构时，可以继续使用 record
- 领域对象中存在明确业务方法和不变量保护时，不要为了 Lombok 破坏封装
- 安全敏感字段不要因为 `@ToString` 或 `@Data` 泄露到日志

### 5.7 中文注释规则

后续新增或重构 Java 代码时，需要给类、关键方法、关键属性补充中文注释，帮助后续阅读和面试讲解。

默认要求：

- 新增类需要说明该类在当前模块中的职责
- record、DTO、VO、Command、Query、PO、配置属性类中的关键属性需要说明业务含义
- service、router、orchestrator、workflow、tool、MCP 等核心方法需要说明输入、处理意图和输出
- 涉及租户、用户、权限、模型路由、RAG、工具调用、工作流状态、异步任务、重试、补偿、审计等关键字段必须注释
- 对外接口 request / response 对象优先补充字段注释

注释风格：

- 优先使用简洁中文 Javadoc
- 注释解释业务意图，不重复描述语法
- 不要给每一行普通赋值、getter、setter 写无意义注释
- 不要用注释掩盖命名不清晰的问题，能通过命名表达的优先改好命名
- 修改已有代码时，如果顺手触碰到关键类或关键方法，应补齐缺失注释

### 5.8 日志规则

后续新增或重构核心 Java 代码时，优先使用 Lombok `@Slf4j` 添加日志能力，并在关键流程打印必要日志。

默认适用范围：

- service
- controller
- router
- orchestrator
- workflow executor
- tool adapter
- MCP adapter
- job
- consumer
- provider client

建议打印的关键信息：

- 请求入口和核心业务阶段
- 模型路由结果
- provider 调用模式
- fallback 触发情况
- workflow run / step 状态变化
- tool 调用开始、结束和失败原因
- RAG 检索文档数量和耗时
- 异步任务投递、重试和 DLQ 情况
- 异常码、异常类型和可定位的业务 ID

日志字段建议：

- `tenantId`
- `userId`
- `runId`
- `conversationId`
- `workflowRunId`
- `toolCallId`
- `modelName`
- `provider`
- `taskType`
- `latencyMs`

日志安全要求：

- 不打印真实 API Key、账号密码、手机号、身份证号等敏感信息
- 不完整打印用户 prompt 和模型响应，必要时只打印长度、摘要或脱敏内容
- 不把 Authorization、Cookie、内部密钥写入日志
- 异常日志要能定位问题，但不能泄露租户数据
- 高频循环或批量处理不要打印过量 info 日志，避免影响性能和可读性

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

每个阶段的代码完成后，完成汇报必须额外提供中文 Git commit 文案。

commit 文案要求：

- 使用中文
- 能概括业务目标和技术改动
- 优先使用一行标题加可选正文
- 标题建议使用类似 `feat(ai-agent): ...`、`fix(gateway): ...`、`docs(ai-agent): ...` 的格式
- 正文可以列出核心改动、验证方式和明确未包含的范围
- 不要把真实 API Key、账号、密码、手机号等敏感信息写入 commit 文案

示例：

```text
feat(ai-agent): 增强模型 Provider 配置与动态路由能力

- 新增 Qwen、OpenAI-compatible、DeepSeek Provider 配置骨架
- 增强 ModelRouter，支持 requestedModel、taskType、能力标签和 providerMode
- 保持默认 mock 模式，避免测试依赖真实 API Key
- 补充模型路由单元测试和 Phase 2 架构文档

验证：
- mvn -pl scm-gateway,scm-ai-agent -am test
```

## 17. 后续任务标准提示词

后续复杂任务可以使用以下提示词：

```text
请严格按照 docs/architecture/ai-agent-roadmap.md 和 docs/architecture/ai-agent-codex-working-rules.md 推进当前任务。先确认当前处于哪个阶段，再实施，不要跳阶段，不要做空泛 Demo，完成后更新对应文档和测试。
```

## 18. Phase 接口验证示例规则

后续每完成一个阶段性的 Phase 或子阶段，例如 Phase 3、Phase 3.1、Phase 4 等，完成汇报中必须提供本阶段对应的接口验证示例，方便本地直接验证实现是否正确。

接口示例默认要求：

- 默认通过 `scm-gateway` 调用，不直接绕过网关调用下游服务。
- 默认端口使用 `18080`。
- 示例 URL 使用 `http://localhost:18080` 开头。
- 如果接口需要鉴权，必须说明需要先登录并携带 `Authorization: Bearer <token>`。
- 如果接口依赖租户和用户上下文，优先说明这些信息由 gateway 从 token 解析并透传；只有直连服务调试时才手动传 `X-Tenant-Id`、`X-User-Id` 等内部请求头。
- 每个接口示例需要包含 method、URL、headers、body 和关键预期返回字段。
- 如果当前阶段包含多步验证，例如写入、检索、执行、查询状态，必须按调用顺序给出完整链路。
- 如果某能力默认是 mock / in-memory 模式，示例中要明确说明默认验证不依赖真实外部服务。
- 如果某能力需要本地中间件，例如 Milvus、Redis、Kafka，必须额外说明启用条件和环境变量，但不能把真实密钥写入示例。

示例格式建议：

```text
POST http://localhost:18080/api/v1/xxx
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "example": "value"
}
```

预期重点检查：

```text
success = true
code = 200
关键业务字段符合预期
```