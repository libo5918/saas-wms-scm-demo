# AI Agent 建设路线图

## 1. 目标

本文档定义 `saas-wms-scm` 如何从一个 Java SCM/WMS 微服务项目，逐步升级为一个适合 Java AI Agent 开发岗位面试展示的企业级 Agent 项目。

目标不是做一个通用聊天机器人，而是做一个能结合真实供应链业务能力的 Agent 平台。它需要覆盖 RAG、Tools、MCP、Model Router、Workflow、Orchestrator、多 Agent 协作、异步长任务和可观测能力。

最终希望面试时可以讲清楚：

- Java 后端如何接入大模型
- Agent 如何调用真实业务系统
- RAG 如何基于项目文档回答问题
- Workflow 如何编排复杂任务
- 多模型如何动态路由和降级
- MCP 如何把企业系统能力暴露给外部 Agent
- Trace / Metrics 如何定位 Agent 调用链路问题

## 2. 目标能力清单

项目需要逐步覆盖 Java AI Agent 岗位常见能力：

- Spring AI `ChatClient`
- 支持 ChatGPT、Qwen、DeepSeek、Ollama、OpenAI-compatible endpoint 的多模型路由
- 基于项目文档、业务文档、运维手册、接口文档、数据库文档的 RAG
- 基于真实 SCM/WMS 服务的 Tool Calling
- MCP Server，将 SCM 工具暴露给外部 Agent 客户端
- Orchestrator，负责任务规划、路由、执行和校验
- Workflow 工作流编排，支持业务诊断和运维流程
- Multi-Agent 多 Agent 协作
- 长任务和异步任务编排
- Agent 运行状态、步骤状态、工具调用日志和重试记录
- 基于 Spring Boot Actuator、Micrometer、SkyWalking 的 Trace / Metrics
- Dify 作为外部编排和演示入口

## 3. 模块规划

### 3.1 `scm-ai-agent`

Agent 主服务。

职责：

- Chat API
- Model Router
- Agent Orchestrator
- Planner / Router / Executor / Verifier
- Multi-Agent 协作
- Agent 运行记录
- Tool 调用记录
- 异步任务入口

### 3.2 `scm-ai-rag`

知识库服务。

职责：

- 文档导入
- 文档切分
- Embedding
- VectorStore 检索
- RAG Advisor
- 引用来源返回
- 租户级知识隔离

### 3.3 `scm-ai-mcp`

MCP Server 服务。

职责：

- 将 SCM/WMS 业务能力暴露为 MCP tools
- 支持 Dify、Cursor、Claude Code 等外部 MCP 客户端调用
- 管理 MCP tool schema、参数校验和权限校验

### 3.4 现有业务服务

现有服务继续作为真实业务能力提供方：

- `scm-mdm`
- `scm-purchase`
- `scm-inventory`
- `scm-sales`
- `scm-auth`
- `scm-gateway`

AI 模块应通过明确的 service client、gateway route、tool adapter 或 MCP adapter 调用现有业务能力，不要复制业务逻辑。

## 4. 技术选型

### 4.1 基础技术栈

- Java 21
- Spring Boot 3.5.x
- Spring Cloud 2025.x
- Spring Cloud Alibaba 2025.x
- Spring AI 1.1.x
- Spring AI Alibaba 1.1.x
- Milvus Standalone
- MySQL
- Redis
- Kafka
- Nacos
- SkyWalking

### 4.2 Spring AI 与 Spring AI Alibaba 的分工

两者都使用，但职责要清晰：

- Spring AI 作为标准 AI 抽象层，负责 `ChatClient`、Tool Calling、VectorStore、Embedding、MCP 基础能力和观测钩子
- Spring AI Alibaba 作为增强层，负责 DashScope/Qwen、Agent、Graph、Workflow、Nacos 以及阿里生态集成

### 4.3 模型提供方

项目不能写死单一模型。

第一批支持：

- Qwen，通过 DashScope 接入
- ChatGPT，通过 OpenAI 或 OpenAI-compatible endpoint 接入

后续支持：

- DeepSeek，通过 OpenAI-compatible endpoint 接入
- Ollama，用于本地模型演示

### 4.4 向量数据库

RAG 主线选择 `Milvus Standalone`。

选型边界：

- `Milvus Standalone`：当前项目主线，用于项目文档 RAG 和后续业务知识库
- `PostgreSQL + pgvector`：作为对照方案保留，用于说明中小规模场景的低复杂度选择
- `Pinecone`：只作为托管向量数据库对比方案了解，不作为当前项目主线

详细决策见：

```text
docs/architecture/ai-agent-technology-decisions.md
```

## 5. Model Router 规划

在 `scm-ai-agent` 中增加 `ModelRouter` 抽象。

路由输入：

- `tenantId`
- `userId`
- `taskType`
- `requestedModel`
- `providerMode`
- `requiredCapabilities`
- `costLevel`
- `maxLatencyMs`

路由策略：

- 用户显式指定模型
- 按任务类型路由
- 按租户模型策略路由
- 按模型能力标签路由
- 按成本等级路由
- 按延迟要求路由
- 按 fallback 列表降级

示例：

```text
simple_chat -> qwen-plus
rag_qa -> qwen-plus
tool_calling -> qwen-plus
workflow_planning -> chatgpt-main
complex_reasoning -> chatgpt-main / deepseek-chat
summary -> qwen-turbo
```

模型能力标签：

- `CHAT`
- `RAG`
- `TOOL_CALLING`
- `STRUCTURED_OUTPUT`
- `PLANNING`
- `LONG_CONTEXT`
- `VISION`
- `LOW_COST`
- `HIGH_QUALITY`
- `LOCAL`

## 6. 实施阶段

### Phase 1：AI Agent 基座

目标：

- 新增 `scm-ai-agent`
- 新增基础 Chat API
- 接入 Spring AI `ChatClient`
- 接入 gateway 透传的租户和用户上下文
- 增加第一版模型配置

验收标准：

- `POST /api/v1/ai/chat` 可以通过 gateway 调用
- 请求能携带 `tenantId` 和 `userId`
- 响应能记录本次选择的模型和耗时
- 有基础测试覆盖

### Phase 2：动态 Model Router

目标：

- 增加模型提供方抽象
- 支持 Qwen 和 OpenAI-compatible 模型
- 支持显式指定模型
- 支持按 `taskType` 自动路由
- 支持 fallback 模型列表

验收标准：

- 调用方可以指定模型
- 系统可以根据任务类型选择模型
- 系统可以根据模型能力标签选择模型
- 系统可以在 mock 和 spring-ai providerMode 之间切换
- 默认 mock 模式不依赖真实 API Key
- 模型调用失败时可以降级到其它模型
- 路由决策可记录、可追踪

当前 Phase 2 落地说明见：

```text
docs/architecture/ai-agent-phase2-model-routing.md
```

### Phase 3：RAG

目标：

- 新增 `scm-ai-rag`
- 导入项目 `docs`
- 将文档向量写入 VectorStore
- 查询时返回答案和引用来源

知识范围：

- `docs/architecture`
- `docs/business`
- `docs/operations`
- `docs/database`

验收标准：

- 用户可以询问项目架构和业务流程
- 回答中包含引用文档
- 检索支持租户级 metadata

### Phase 4：Tools

目标：

- 将真实 SCM/WMS 业务接口包装成 Agent tools

第一批工具：

- `inventory.getBalance`
- `inventory.listTxnRecords`
- `sales.getOrder`
- `purchase.getReceipt`
- `mdm.getMaterial`
- `ops.getDlqSummary`

验收标准：

- Agent 可以根据用户意图调用工具
- 工具参数有校验
- 工具调用记录包含 `runId` 和 `tenantId`
- 写操作工具执行前必须有确认机制

### Phase 5：Orchestrator 与 Multi-Agent

目标：

- 增加任务规划、路由、执行和校验能力
- 增加面向领域的 Agent

第一批 Agent：

- Inventory Diagnosis Agent
- Sales Fulfillment Agent
- Purchase Inbound Agent
- Operations Runbook Agent
- Knowledge QA Agent

验收标准：

- Orchestrator 可以把请求路由到正确 Agent
- 每个 Agent 有自己的 prompt、tools、RAG 范围和输出格式
- 多步骤诊断能输出结构化结论

### Phase 6：Workflow

目标：

- 增加工作流定义和执行能力
- 支持 LLM 节点、工具节点、条件节点、人工确认节点和结束节点

第一批工作流：

- 库存异常诊断
- 销售发货失败分析
- 采购入库失败补偿
- 上线前巡检

验收标准：

- Workflow run 状态可以持久化
- Workflow node 状态可以持久化
- 失败节点支持重试
- 人工确认节点可以暂停和恢复流程

### Phase 7：异步长任务

目标：

- 通过 Kafka 和持久化状态支持长时间运行的 Agent 任务

建议 topic：

```text
agent.task.requested
agent.task.step.completed
agent.task.failed
agent.task.completed
```

验收标准：

- 提交接口返回 `taskId`
- 查询接口返回任务状态和步骤进度
- 失败任务可以重试
- DLQ 处理方式有文档说明

### Phase 8：MCP

目标：

- 新增 `scm-ai-mcp`
- 通过 MCP 暴露 SCM/WMS 工具

验收标准：

- MCP client 可以发现工具
- MCP client 可以调用只读 SCM tools
- 工具权限和租户校验生效
- Dify 或其它外部 Agent 客户端可以调用暴露的工具

### Phase 9：Trace And Metrics

目标：

- 增加 Agent 可观测能力

核心字段：

- `agent_run_id`
- `conversation_id`
- `tool_call_id`
- `model_name`
- `provider`
- `prompt_tokens`
- `completion_tokens`
- `latency_ms`
- `retrieval_doc_count`
- `tool_success_rate`
- `fallback_count`
- `workflow_step_count`

验收标准：

- Agent 请求可以串联 gateway、Agent 服务、tools 和业务服务
- 模型调用和工具调用有耗时指标
- 失败的 Agent run 可以通过日志和 trace 诊断

### Phase 10：Dify 集成

目标：

- 将 Dify 作为外部工作流和演示平台，而不是作为项目核心实现

集成方式：

- Dify 调用 `scm-ai-mcp`
- Dify 调用 gateway OpenAPI tools
- Dify 演示外部编排如何调用当前项目工具

验收标准：

- Dify workflow 至少能调用一个 SCM tool
- Dify 调用不绕过 gateway 鉴权和租户隔离

## 7. 面试演示场景

### 7.1 供应链知识问答 Agent

问题示例：

```text
这个项目的销售发货链路是怎么设计的？
```

体现能力：

- RAG
- 引用来源
- 租户级知识隔离

### 7.2 库存异常诊断 Agent

问题示例：

```text
为什么销售单 SO-1001 无法发货？
```

体现能力：

- Tool Calling
- 多步骤推理
- 库存余额查询
- 库存流水查询
- 失败原因总结

### 7.3 采购入库补偿 Agent

问题示例：

```text
采购收货单 RCV-1001 入库失败，应该如何处理？
```

体现能力：

- Tool Calling
- Workflow
- 人工确认
- 重试建议

### 7.4 上线巡检 Agent

检查内容：

- Nacos 服务注册
- Kafka topic
- 服务健康状态
- DLQ 堆积
- 最近 trace

体现能力：

- Workflow
- 异步任务
- 运维工具
- Metrics / Trace

### 7.5 MCP 工具平台

场景：

```text
让外部 Agent 客户端通过 MCP 调用 SCM/WMS 工具。
```

体现能力：

- MCP Server
- Tool schema
- 权限校验
- 外部 Agent 集成

## 8. 非目标

项目应避免以下方向：

- 做一个和 SCM/WMS 业务无关的通用聊天机器人
- 用 AI 代码替代现有业务服务
- 把所有 AI 逻辑堆在一个 Controller
- 绕过 gateway 鉴权
- 跳过租户隔离
- 只接 Dify，但 Java AI Agent 逻辑为空
- 写死单一模型提供方

## 9. 完成标准

每个阶段只有满足以下条件才算完成：

- 代码可以编译
- 核心行为有测试或明确验证脚本
- 已考虑 gateway 和租户上下文
- 对应文档已更新到 `docs`
- 风险和下一步已记录
- 该阶段能力可以用于 Java AI Agent 岗位面试讲解
