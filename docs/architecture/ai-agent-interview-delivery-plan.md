# AI Agent 面试展示优先交付计划

## 1. 目标

从 Phase 4.15 开始，AI Agent 主线切换为“Java AI Agent 企业级面试展示优先”策略：

- 优先形成可运行、可讲解、可演示的企业级 Agent 闭环。
- 保留必要的工程治理能力，但不继续过度细化低收益内部实现。
- 重点覆盖 Java AI Agent 面试高频能力：RAG、Tools、Orchestrator、Workflow、MCP、权限审计、可观测性、配置隔离和稳定测试。

## 2. 当前完成度

截至 Phase 5.2：

- Tools 主线已收敛：真实模型规划、Tool 执行、模型总结 answer 已闭环。
- Tool 已具备 display schema、权限标签、路由标签、audit、runtime retry/circuit breaker。
- Orchestrator 已具备 run / plan / step / status，以及受控二步只读执行能力。
- RAG + Tool 组合问答已具备 `/api/v1/ai/agent/chat` 入口。
- Prompt Context / Advisor 风格上下文治理已落地，RAG、Tool、Orchestrator 可统一注入模型上下文。

## 3. 快速推进路线

### Phase 5.1：RAG + Tool 组合问答

目标：让用户一个问题同时用到项目知识库和业务 Tool。

当前实现重点：

- 新增 `/api/v1/ai/agent/chat` 组合入口。
- RAG 负责规则、口径、字段含义和流程背景。
- Tool 负责实时物料、仓库、库存、订单等业务数据。
- Orchestrator 提供 run / plan / step / status 的脱敏过程记录。
- “只查物料”不触发库存第二步；只有明确表达库存意图时才允许 `inventory.getBalance` follow-up。

### Phase 5.2：Prompt Context / Advisor 风格上下文工程化

目标：把 RAG context、Tool display schema、Orchestrator step summary 纳入统一上下文治理层。

当前实现重点：

- `AgentPromptContext` 表达一次模型回答所需的结构化上下文。
- `AgentPromptContextProvider` 作为 Advisor 风格扩展点，分别注入 RAG、Tool、Orchestrator、用户问题和安全约束。
- `AgentPromptContextAssembler` 统一做优先级排序、长度裁剪、敏感片段过滤。
- `AgentPromptContextRenderer` 统一渲染最终模型输入。
- 当前不强制绑定 Spring AI Advisor；后续可将 Provider 平滑包装为 Spring AI Advisor。

### Phase 6.1：Workflow 最小闭环

目标：实现一个可配置的企业流程编排示例。

当前实现重点：

- 新增 `/api/v1/ai/workflows`、`/api/v1/ai/workflows/{workflowCode}/run`、`/api/v1/ai/workflows/runs/{runId}`。
- 固定只读 Workflow 示例：查询物料 -> 查询库存 -> 生成补货建议草案。
- Workflow 复用 ToolInvocationService，保留权限、audit、runtime protection。
- Workflow status 只返回脱敏步骤概要，不返回完整 rawData、完整 prompt 或完整模型响应。
- 暂不实现通用工作流引擎、复杂异步长任务或写操作。

### Phase 6.2：Workflow + RAG 组合增强

目标：让固定只读 Workflow 的补货建议草案同时利用企业知识库规则和实时业务数据。

当前实现重点：

- Workflow run 请求支持可选 `knowledgeBaseId`、`topK`、`scoreThreshold`、`filters`。
- 不传 `knowledgeBaseId` 时保持 Phase 6.1 行为，不检索 RAG。
- 传入 `knowledgeBaseId` 时，`generate_advice` Summary 阶段先做 RAG retrieve，再结合物料 safeFields、库存 safeFields 生成建议。
- RAG 用于解释库存可用数量口径、锁定数量含义、物料状态含义、补货规则和人工确认边界。
- Tool 数据仍是实时事实来源，RAG 不覆盖 Tool 查询结果。
- RAG 概要放入 `generate_advice.safeFields.rag`，只返回脱敏 chunk 摘要，不返回完整文档原文、完整 prompt 或完整模型响应。
- 暂不实现通用 Workflow Engine；Phase 6.3 再抽象 StepExecutor、参数解析和条件步骤。

面试讲解话术：

“`/api/v1/ai/agent/chat` 更像自由问答 Agent，会根据问题动态组合 RAG 和 Tool；Workflow 则是明确业务步骤的流程编排，比如补货建议必须先查物料、再查库存、最后生成建议。Phase 6.2 把 RAG 放在 Summary 阶段，用知识库解释业务口径，用 Tool 保证实时数据准确，这就是企业 Agent 常见的‘流程 + 知识 + 实时数据’闭环。”

### Phase 6.3：Workflow Engine 最小抽象

目标：把固定 Workflow 中写死的步骤调用拆成 Engine / Executor / Context，形成可扩展步骤执行框架。

当前实现重点：

- `AgentWorkflowService` 变成门面服务，只负责 definitions、run、status。
- `AgentWorkflowEngine` 负责创建 run，并按 definition.steps 顺序调度。
- `AgentWorkflowStepExecutorRegistry` 负责根据 step definition 找 executor。
- `ToolWorkflowStepExecutor` 负责 Tool 步骤，复用 ToolInvocationService，保留权限、audit、runtime protection。
- `SummaryWorkflowStepExecutor` 负责 Summary 步骤，按需检索 RAG 并生成 finalAnswer。
- `AgentWorkflowExecutionContext` 负责跨步骤传递安全摘要，例如从 `query_material` 的 safeFields 中取 `materialId` 给库存步骤。
- 当前仍不实现完整通用工作流平台，不做 BPMN、并行、异步恢复、人工审批或写操作。

面试讲解话术：

“Phase 6.1/6.2 先把业务流程闭环跑通，Phase 6.3 再把硬编码三步拆成 Engine + Executor。这样新增流程时不是复制一个 WorkflowService2，而是新增 definition，复用已有 Tool/Summary executor；如果出现新步骤类型，再扩展一个 executor。这体现的是企业项目常见的渐进式抽象：先闭环，再抽象，不一开始就造一个大而全的工作流平台。”

### Phase 7.1：MCP-style Tool Adapter 最小演示

目标：面试中能说明项目可以把企业内部已治理 Tool 以 MCP 风格暴露给外部 Agent、IDE 或客户端。

当前实现重点：

- 新增 `GET /api/v1/ai/mcp/tools`，返回允许暴露的只读 Tool 列表。
- 新增 `POST /api/v1/ai/mcp/tools/{toolName}/invoke`，以 MCP 风格调用只读 Tool。
- 默认只暴露 `mdm.getMaterial`、`inventory.getBalance` 两个安全只读 Tool。
- MCP-style invoke 复用 `ToolInvocationService`，因此权限、audit、runtime timeout / retry / circuit breaker 都继续生效。
- 返回 display 安全视图，不返回完整 rawData、内部 URL、token、API Key、敏感 header、完整 prompt 或完整模型响应。

面试讲解话术：

“MCP 解决的是外部 Agent 如何发现和调用企业内部工具的问题。这个项目没有重新造一套 Tool 执行体系，而是在现有 ToolRegistry 和 ToolInvocationService 外面包了一层 MCP-style adapter。这样外部客户端看到的是标准化的 tool list 和 invoke，内部仍然复用权限、审计、熔断、display schema 等企业级治理能力。当前阶段是 HTTP MCP-style adapter，后续如果接标准 MCP Server，只需要替换协议 transport 层，核心 Tool 治理链路不用重写。”

### Phase 8.1：面试交付收敛

目标：把已有能力收敛成一套可运行、可讲解、可演示的 Java AI Agent 企业级项目交付物。

当前实现重点：

- 新增 `docs/architecture/ai-agent-interview-demo-guide.md`，集中整理演示顺序、启动配置、接口调用示例、能力矩阵和面试讲解稿。
- 演示顺序覆盖 Chat、RAG、Tool Calling、Agent Chat、runtime status、Orchestrator、Workflow 和 MCP-style Adapter。
- 不继续新增复杂业务功能，不改变已有 API 返回结构。
- 明确当前项目已具备面试展示所需的 RAG、Tool、Orchestrator、Workflow、MCP-style、权限审计、runtime protection、Prompt Context 和测试隔离能力。

面试讲解话术：

“Phase 8.1 做的是交付收敛，而不是继续堆功能。前面各阶段已经把 RAG、Tool Calling、Agent Chat、Orchestrator、Workflow 和 MCP-style adapter 都跑通了，这一阶段把它们整理成一条面试可演示路径：先导入知识库，再验证 RAG，再查实时 Tool，再组合 RAG + Tool，最后展示 Workflow 和 MCP-style 外部工具暴露。这样面试时我不仅能讲设计，也能按 gateway 18080 把完整链路跑出来。”

## 4. 面试讲解主线

推荐讲法：

1. 先讲基础后端：多模块 Spring Boot、Gateway、统一认证上下文、租户上下文。
2. 再讲 RAG：文档导入、切片、Embedding、Milvus / in-memory 双模式、检索与回答。
3. 再讲 Tool Calling：Tool schema、模型规划、权限、审计、runtime 保护、结果 display schema。
4. 再讲 Orchestrator：run / plan / step、stepRef、安全摘要、受控二步只读执行。
5. 再讲 Prompt Context：RAG、Tool、Orchestrator 通过 Advisor 风格 Provider 统一注入模型上下文。
6. 再讲 Workflow：固定业务流程如何复用 Tool 权限、审计、runtime 保护，并在 Summary 阶段接入 RAG。
7. 再讲 MCP：企业内部 Tool 如何以标准化接口暴露给外部 Agent，同时保留权限、审计和 runtime 保护。
8. 最后按 `docs/architecture/ai-agent-interview-demo-guide.md` 展示完整 gateway 18080 调用链路。
9. 再讲扩展：标准 MCP Server、Multi-Agent、复杂长任务编排是后续方向，项目已预留治理边界。

## 5. 推进原则

- 每个阶段必须可运行、可测试、可通过 gateway 18080 演示。
- 单测不依赖真实模型、真实业务服务、MySQL、Milvus、Embedding API 或外部网络。
- 新增能力优先选择最小闭环，能讲清楚企业级设计即可。
- 不为面试展示牺牲安全边界：不泄露 API Key、用户凭证、敏感 header、完整模型输入、完整模型响应。
### Phase 9.1：标准 MCP Server transport 最小演示

目标：在 Phase 7.1 HTTP MCP-style Adapter 基础上，增加 JSON-RPC 风格的标准 MCP Server transport，支持 `tools/list` 和 `tools/call`，让项目在面试中可以讲清楚“内部 Java Tool 如何通过 MCP 协议暴露给外部 Agent / IDE / MCP Client”。

当前实现重点：

- 新增 `POST /api/v1/ai/mcp/server`。
- 支持 `tools/list`，返回可暴露的只读 Tool 定义。
- 支持 `tools/call`，调用 `mdm.getMaterial`、`inventory.getBalance` 等安全只读 Tool。
- 复用 `McpToolExposureService` 和 `ToolInvocationService`。
- 权限、audit、runtime timeout / retry / circuit breaker、display schema 不重复实现。
- 不返回完整 rawData、API Key、token、敏感 header、完整 prompt 或完整模型响应。
- 不引入复杂外部 MCP Client / IDE 集成，不实现 Multi-Agent。

面试讲解话术：

“MCP 的价值是让外部 Agent 或 IDE 用标准协议发现和调用工具。这个项目先在 Phase 7.1 做 HTTP MCP-style Adapter，便于通过 gateway 演示；Phase 9.1 再补一个 JSON-RPC 风格的 MCP Server transport，支持 MCP 里的 `tools/list` 和 `tools/call` 核心语义。重点不是重新写一套 Tool 系统，而是复用项目已有的 `ToolRegistry`、`McpToolExposureService` 和 `ToolInvocationService`。所以外部看起来是 MCP 协议，内部仍然走租户上下文、权限校验、审计、runtime 保护和 display schema。后续如果换成 Spring AI MCP Server Starter 或接真实 IDE，只需要替换 transport 和会话层，企业治理链路不用重写。”

### Phase 10.1：Multi-Agent 基础模型与 Coordinator 骨架

目标：进入 Multi-Agent，但先做角色边界、状态模型和 Coordinator 最小骨架，不引入外部 Multi-Agent 框架，不做无约束多轮自治。

当前实现重点：

- 新增 `POST /api/v1/ai/multi-agent/chat`。
- 新增 `GET /api/v1/ai/multi-agent/runs/{runId}`。
- 新增 `MultiAgentRun`、`MultiAgentStep`、`MultiAgentMessage`。
- 定义 CoordinatorAgent、PlannerAgent、KnowledgeAgent、ToolAgent、ReviewerAgent。
- Phase 10.1 只真实执行 Coordinator 和 Planner 的安全骨架步骤。
- 状态接口只返回脱敏摘要，不返回完整 prompt、模型响应、rawData 或敏感 header。

面试讲解话术：

“我没有一上来引入 AutoGen 或 CrewAI，因为企业级 Multi-Agent 的第一步不是让多个 Agent 自由聊天，而是先把角色边界、运行状态、终止约束和安全摘要设计清楚。Phase 10.1 里 CoordinatorAgent 负责调度和状态记录，PlannerAgent 负责生成计划摘要，KnowledgeAgent、ToolAgent、ReviewerAgent 先作为角色定义保留扩展点。后续 Phase 10.2 再逐步把 RAG、ToolInvocationService 和 Reviewer 校验接进来。这样做的好处是，多 Agent 协作不会绕过现有权限、审计、runtime protection 和 Prompt Context 治理。”
