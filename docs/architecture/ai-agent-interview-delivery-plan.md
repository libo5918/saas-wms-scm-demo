# AI Agent 面试展示优先交付计划

## 1. 目标

从 Phase 4.15 开始，AI Agent 主线切换为“Java AI Agent 企业级面试展示优先”策略：

- 优先形成可运行、可讲解、可演示的企业级 Agent 闭环。
- 保留必要的工程治理能力，但不继续过度细化低收益内部实现。
- 重点覆盖 Java AI Agent 面试中高频能力：RAG、Tools、Orchestrator、Workflow、MCP、权限审计、可观测性、配置隔离和稳定测试。

## 2. 当前完成度

截至 Phase 4.15，Tools 主线可以收敛：

- Tool Calling Chat 已完成真实模型规划、Tool 执行和模型总结 answer。
- Tool 执行已接入权限标签、路由标签、runtime retry/circuit breaker 和 audit。
- Tool 结果已统一 display schema，保留 rawData 追溯。
- Orchestrator 已具备 run / plan / step / status，以及受控二步只读执行能力。
- 默认仍保持单步主路径，受控二步执行必须显式开启。

## 3. 后续快速推进路线

### Phase 5.1：RAG + Tool 组合问答

状态：已进入实现阶段。

目标：让用户一个问题同时用到项目知识库和业务 Tool。

建议能力：

- 新增组合入口，例如 `/api/v1/ai/agent/chat` 或在既有 chat 中增加模式。
- 先检索 RAG 上下文，再规划 Tool，再由模型基于 RAG context + Tool execution 生成答案。
- 保持外部网络、真实模型、业务服务在单测中全部 mock/stub。
- 给出可演示问题：先解释库存查询规则，再查询指定物料库存。

当前实现重点：

- 新增 `/api/v1/ai/agent/chat` 组合入口。
- RAG 负责规则、口径、字段含义和流程背景。
- Tool 负责实时物料、仓库、库存、订单等业务数据。
- Orchestrator 提供 run / plan / step / status 的脱敏过程记录。
- “只查物料” 不触发库存第二步；只有明确表达库存意图时才允许 `inventory.getBalance` follow-up。

### Phase 5.2：RAG Advisor / Prompt Context 工程化

目标：对齐 Spring AI 常见用法，能讲清楚 Advisor 与自研检索编排的取舍。

建议能力：

- 在现有 `RoutingChatModelClient` 基础上抽象上下文增强层。
- 支持 RAG context、Tool display schema、Orchestrator step summary 分区注入。
- 文档说明为什么当前项目先自研组合编排，后续可平滑接入 Spring AI Advisor。

### Phase 6.1：Workflow 最小闭环

目标：实现一个可配置的企业流程编排示例。

建议能力：

- 固定只读 Workflow 示例：查询物料 -> 查询库存 -> 生成补货建议草案。
- Workflow step 与 Orchestrator step 区分：Workflow 更偏业务流程，Orchestrator 更偏 Agent 工具执行。
- 暂不实现复杂异步长任务。

### Phase 7.1：MCP Server 最小演示

目标：面试中能说明项目可以把内部 Tool 暴露给标准 MCP 客户端。

建议能力：

- 暴露只读 Tool list 和 invoke 的 MCP 最小接口。
- 只接入 `mdm.getMaterial`、`inventory.getBalance` 等安全只读 Tool。
- 保留权限、审计、runtime 保护。

## 4. 面试讲解主线

推荐讲法：

1. 先讲基础后端：多模块 Spring Boot、Gateway、统一认证上下文、租户上下文。
2. 再讲 RAG：文档导入、切片、Embedding、Milvus / in-memory 双模式、检索与回答。
3. 再讲 Tool Calling：Tool schema、模型规划、权限、审计、runtime 保护、结果 display schema。
4. 再讲 Orchestrator：run / plan / step、stepRef、安全摘要、受控二步只读执行。
5. 最后讲扩展：Workflow、MCP、Multi-Agent 是后续扩展方向，项目已预留治理边界。

## 5. 推进原则

- 每个阶段必须可运行、可测试、可通过 gateway 18080 演示。
- 单测不依赖真实模型、真实业务服务、MySQL、Milvus、Embedding API 或外部网络。
- 新增能力优先选择最小闭环，能讲清楚企业级设计即可。
- 不为面试展示牺牲安全边界：不泄露 API Key、token、敏感 header、完整 prompt、完整模型响应。
