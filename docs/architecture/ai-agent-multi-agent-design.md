# AI Agent Multi-Agent 设计

## 1. 定位

Phase 10.1 的 Multi-Agent 目标不是引入 AutoGen、CrewAI 或 LangGraph，也不是让多个 Agent 无约束互相聊天，而是在现有 Java 企业级 AI Agent 项目中建立一套可控的多角色协作骨架。

本项目已经具备 RAG、Tool Calling、Orchestrator、Workflow、Prompt Context、MCP Server transport。Multi-Agent 的价值是把这些能力按角色协作方式组织起来：

- CoordinatorAgent 负责任务调度、轮次控制、状态记录和最终汇总。
- PlannerAgent 负责识别任务需要 RAG、Tool、Workflow 还是 MCP。
- KnowledgeAgent 后续负责 RAG 检索和知识口径解释。
- ToolAgent 后续负责通过 ToolInvocationService 调用受治理 Tool。
- ReviewerAgent 后续负责检查答案是否安全、是否基于事实、是否遗漏失败原因。

## 2. 为什么不直接引入外部 Multi-Agent 框架

当前项目的首要目标是 Java AI Agent 企业级面试展示。直接引入外部 Multi-Agent 框架会带来几个问题：

- 学习和解释成本高，容易把重点从企业工程能力转移到框架 API。
- 外部框架通常偏 Python 或自治 Agent 范式，和当前 Spring Boot / Gateway / 租户上下文 / Tool 治理链路不完全贴合。
- 企业项目更关心权限、审计、状态记录、失败分支和终止条件，而不是多个 Agent 自由对话。

因此 Phase 10.1 先做项目内最小骨架：角色定义、Run/Step/Message 状态、Coordinator 服务和状态查询接口。后续如果需要接 LangGraph 或其他框架，可以把它们放在 Coordinator 内部作为执行策略，而不是替代现有治理链路。

## 3. Run / Step / Message 模型

`MultiAgentRun` 表达一次协作运行：

- runId
- tenantId / userId
- userMessage 安全摘要
- status
- agents
- steps
- messages
- finalAnswer
- latencyMs

`MultiAgentStep` 表达一个 Agent 的一次动作：

- stepNo
- agentName / agentRole
- actionType
- status
- inputSummary
- outputSummary
- errorCode / errorMessage
- latencyMs

`MultiAgentMessage` 表达 Agent 间安全摘要消息：

- fromAgent
- toAgent
- messageType
- contentSummary
- structuredData

Message 不保存完整 prompt、完整模型响应、完整 rawData、token、authorization、cookie 或敏感 header。

## 4. Phase 10.1 骨架行为

当前阶段 `POST /api/v1/ai/multi-agent/chat` 只做受控单轮骨架：

1. CoordinatorAgent 接收用户任务。
2. PlannerAgent 根据关键词生成计划摘要。
3. 记录 run / agents / steps / messages。
4. 返回一个可解释的最小 answer。

本阶段不执行真实 RAG、Tool、Workflow 或 MCP 调用。这样可以先把角色边界、状态模型、脱敏视图和接口跑通，降低后续接入复杂能力的风险。

## 5. 与 Orchestrator 的区别

Orchestrator 更偏 Tool 调用过程治理：

- run / plan / step
- controlled 二步只读 Tool 执行
- stepRef / safe summary
- runtime / audit / permission 贯通

Multi-Agent 更偏角色协作治理：

- Coordinator / Planner / Knowledge / Tool / Reviewer 角色边界
- Agent 间消息摘要
- 最大轮次、最大 Agent 数、最大 Tool 调用次数
- 后续可把 RAG、Tool、Workflow、MCP 都作为不同 Agent 的动作

一句话：Orchestrator 管“工具步骤怎么受控执行”，Multi-Agent 管“多个职责角色如何协作完成任务”。

## 6. 与 Workflow 的区别

Workflow 是确定性业务流程，例如补货建议：

- 固定步骤定义
- 固定执行顺序
- 明确业务边界
- 适合稳定、可审计的业务流程

Multi-Agent 是面向开放问题的协作框架：

- 先由 PlannerAgent 判断任务类型
- 再由不同 Agent 承担知识检索、工具执行、结果审查
- 适合问题形态不完全固定的 Agent 场景

一句话：Workflow 管“确定流程”，Multi-Agent 管“角色协作”。

## 7. 后续 Phase 10.2

Phase 10.2 建议接入最小真实协作：

- PlannerAgent 继续使用轻量规则识别 RAG / Tool / RAG_TOOL。
- KnowledgeAgent 复用 RagService 做 retrieve。
- ToolAgent 复用 ToolInvocationService 或现有 Agent Chat / Orchestrator，只允许只读 Tool。
- ReviewerAgent 基于安全摘要做规则校验，先不强制真实模型审查。
- Coordinator 控制最大轮次、最大 Tool 调用次数和终止条件。

这样可以形成可演示的“多 Agent 协作回答库存问题”闭环，同时保持企业级可控边界。

## 8. Phase 10.2：最小真实协作落地

Phase 10.2 已把上面的建议落成单轮受控协作：

1. `PlannerAgent` 规则化识别任务类型：`RAG_ONLY`、`TOOL_ONLY`、`RAG_TOOL`、`WORKFLOW`、`MCP_TOOL`、`GENERAL`。
2. `KnowledgeAgent` 在 `needRag=true` 且传入 `knowledgeBaseId` 时复用 `RagService.retrieve`，只保存知识片段摘要。
3. `ToolAgent` 在 `needTool=true` 时复用 `ToolCallingChatService`，继续走 Tool 权限、audit、runtime protection、display schema 和 Orchestrator。
4. `ReviewerAgent` 做规则化审查：不允许 RAG 未召回时声称“根据知识库”，不允许泄露 token、authorization、cookie、API Key、rawData、prompt 等敏感信息。
5. `CoordinatorAgent` 汇总各 Agent 的安全摘要，使用服务端模板生成 `finalAnswer`。

本阶段仍不做复杂多轮自治，也不让 Agent 自由互相聊天。核心目标是让面试中能讲清楚企业级 Multi-Agent 的角色边界、状态记录和治理链路。

## 9. Phase 10.2 与已有模块的复用关系

- RAG：复用 `RagService.retrieve`。
- Tool：复用 `ToolCallingChatService`，间接复用 `ToolInvocationService`、Tool 权限、Tool audit、runtime protection、Orchestrator。
- Review：当前为规则化审查，后续可升级模型审查。

面试表达：

> Multi-Agent 不直接绕过业务系统。ToolAgent 必须复用已经治理过的 Tool Calling 主链路，这样租户、权限、审计、熔断、display schema 都保持一致。

## 10. 后续 Phase 10.3

- 增加 `maxRounds`、`maxToolCalls` 的强约束。
- ReviewerAgent 可升级为模型审查，但保留规则兜底。
- Coordinator 可切换到模型总结，但仍使用 Prompt Context 治理输入。
- 增强失败分支和状态接口展示。

## 11. Phase 10.3：可控约束与总结治理

Phase 10.3 已补充 Multi-Agent 的企业级控制能力：

1. `maxRounds` 从配置绑定升级为硬约束。当前仍是单轮协作，如果配置小于 1，Coordinator 会直接受控终止，不进入 Tool 执行。
2. `maxToolCalls` 从配置绑定升级为硬约束。如果 Planner 判断需要 Tool，但 `maxToolCalls=0`，ToolAgent 会被跳过，不调用真实 Tool。
3. Run / Status 返回 `constraints`、`roundCount`、`toolCallCount`、`terminatedReason`、`summaryMode`、`fallbackUsed`。
4. ReviewerAgent 增强审查规则：
   - Tool 成功但最终答案缺少关键 `displaySummary` 时给出 issue；
   - RAG 有召回但回答未体现规则/口径时给出 suggestion；
   - Tool 失败但回答没有保留 errorMessage 时给出 issue；
   - 拦截 authorization、cookie、token、api key、rawData、prompt、model response 等敏感关键词。
5. Coordinator 支持 `model-summary-enabled` 开关。默认关闭，走模板总结；开启后复用已有 `AgentChatService` 做模型总结，失败自动回退模板。

Phase 10.3 仍然不做复杂多轮自治，不引入外部 Multi-Agent 框架，不新增写操作 Tool。

## 12. Phase 10.4：Reviewer 驱动的一次受控修正

Phase 10.4 在 Phase 10.3 的约束基础上增加“审查失败后最多一次修正”：

1. `review-repair-enabled=false` 时保持 Phase 10.3 行为，只审查不修正。
2. `review-repair-enabled=true` 且 ReviewerAgent 审查不通过时，CoordinatorAgent 可以基于 Planner / RAG / Tool / Review 的安全摘要生成修正回答。
3. 修正计入 `roundCount`，并受 `maxRounds` 限制；如果轮次不足，不执行修正，返回受控终止原因。
4. `max-repair-attempts` 默认 1，本阶段不允许无限循环，也不允许多个 Agent 自由互相聊天。
5. `repair-mode=template` 使用服务端模板修正，稳定、可测试；`repair-mode=model` 使用模型修正，失败后自动回退模板修正。
6. Run / Status 新增脱敏字段：`repairEnabled`、`repairAttempted`、`repairCount`、`repairMode`、`repairFallbackUsed`、`reviewAfterRepair`。

面试表达：

> 企业级 Multi-Agent 不能只让 Agent 产出答案，还要有审查、修正和终止约束。Phase 10.4 里 ReviewerAgent 如果发现答案没有引用 Tool 事实、遗漏错误原因或存在敏感信息，CoordinatorAgent 最多只做一次受控修正，然后再次审查。这样既体现了 Multi-Agent 协作闭环，也避免了无限自我反思和不可控成本。

## 13. Phase 10.5：会话级 Memory 最小闭环

Phase 10.5 增加的是 Multi-Agent 的 conversation 级安全摘要记忆，不是长期记忆系统，也不是向量 Memory。

核心设计：

- `MultiAgentMemoryEntry` 按 `conversationId + tenantId + userId` 隔离。
- 只保存摘要类型：`USER_MESSAGE_SUMMARY`、`PLAN_SUMMARY`、`RAG_SUMMARY`、`TOOL_SUMMARY`、`REVIEW_SUMMARY`、`FINAL_ANSWER_SUMMARY`。
- `InMemoryMultiAgentMemoryStore` 支持 append、按 conversationId 查询、清理、按 `memory-max-records` 裁剪。
- `MultiAgentCoordinatorService` 在 Planner 前读取最近记忆摘要，在 run 结束后写入本次安全摘要。
- `ReviewerAgent` 与 Memory service 都会过滤 `authorization`、`cookie`、`token`、`api key`、`rawData`、`prompt`、`model response` 等敏感字段。

Memory 与其他记录的区别：

- Run / Step：记录一次协作运行过程，用于状态查询和调试。
- Audit：记录 Tool 调用审计，用于权限、调用结果、耗时追踪。
- RAG：外部知识库检索，不等于用户会话记忆。
- Memory：只保存同一 conversation 下可复用的安全摘要，用于下一次 Multi-Agent 协作的上下文衔接。

配置示例：

```yaml
ai:
  agent:
    multi-agent:
      memory-enabled: true
      memory-max-records: 100
      memory-read-limit: 5
```

边界：

- 默认关闭，不影响 Phase 10.4 行为。
- 不保存完整 prompt、完整模型响应、完整 rawData、用户 token、Authorization、Cookie、API Key。
- 不做 MySQL/Redis 持久化，不做向量 Memory，不做跨用户共享。

面试表达：

> 企业级 Agent 的 Memory 不能简单地把聊天记录全量塞回上下文。这个项目里我先做会话级安全摘要记忆：按租户、用户、conversationId 隔离，只保存 Planner、RAG、Tool、Review 和最终回答的短摘要，并且可查询、可清理、可裁剪。这样既能让同一会话有上下文连续性，又避免泄露 prompt、rawData、token 或完整模型响应。后续如果要升级长期记忆，可以把 Store 换成 MySQL/Redis，或者把摘要再进入向量库，但治理边界不变。