# AI Agent 面试高频题答案

本文根据面试题截图整理，答案优先结合当前 `saas-wms-scm` 项目中的 AI Agent 模块回答。当前项目主线包括：Spring AI Chat、RAG、Tool Calling、RAG + Tool、Prompt Context / Advisor 风格上下文治理、Orchestrator、Workflow、MCP-style Adapter、标准 MCP Server transport、Multi-Agent、Memory、权限审计、runtime protection 和测试隔离。

## 1. 介绍一下你做过的 Agent 项目整体流程是什么？

我做的是一个面向 SCM/WMS 场景的企业级 Java AI Agent 项目，不是简单封装一个大模型接口，而是把大模型能力和企业业务系统结合起来。

整体流程可以概括为：

1. 用户通过 `/api/v1/ai/agent/chat` 或 Tool Calling Chat 入口提问。
2. 系统先做意图识别，判断问题是普通 Chat、RAG 知识问答、Tool 实时查询，还是 RAG + Tool 组合任务。
3. 如果需要知识解释，就走 RAG retrieve，检索库存口径、物料状态、补货规则等知识库片段。
4. 如果需要实时业务数据，就通过 Spring AI Planner 或受控 Orchestrator 规划 Tool，例如 `mdm.getMaterial`、`inventory.getBalance`。
5. 服务端执行 Tool，执行前经过权限、只读校验、参数校验、runtime retry / circuit breaker、audit 等治理。
6. Tool 执行结果会转成统一 display schema，例如 `displayTitle`、`displaySummary`、`displayFields`、`displayItems`、`rawData`。
7. 最后通过 Prompt Context / Advisor 风格上下文层，把用户问题、RAG 片段、Tool 结果、Orchestrator step 摘要、安全约束统一组装后交给模型总结。

我这样拆流程的原因是：企业 Agent 里规则解释和实时事实不是一类东西。规则解释适合 RAG，实时数据必须查业务系统，确定性步骤应该由服务端控制，模型主要负责理解、规划和自然语言总结。

## 2. 哪些节点是确定性 Workflow，哪些节点交给 LLM 决策？

我会把风险高、确定性强、需要审计的节点放在服务端 Workflow 或 Orchestrator 里，把语言理解、意图判断、总结表达交给 LLM。

确定性节点包括：

- 用户上下文、租户上下文解析。
- Tool 是否存在、是否只读、是否允许 MCP 暴露。
- 权限校验、参数校验、敏感字段过滤。
- Tool 执行、审计、超时、重试、熔断。
- Workflow 固定流程，例如补货建议中的 `query_material -> query_inventory_balance -> generate_advice`。
- Multi-Agent 的 maxRounds、maxToolCalls、Reviewer 审查和终止条件。

交给 LLM 的节点包括：

- 根据用户问题和 Tool schema 选择合适 Tool。
- Tool 执行成功后生成自然语言回答。
- RAG + Tool 场景下综合知识库规则和实时业务数据。
- Workflow Summary 阶段生成补货建议草案。
- Multi-Agent 中可选的 Coordinator 模型总结。

核心原则是：模型可以做“判断和表达”，但真实执行必须经过服务端治理链路。

## 3. 有没有失败分支和重试机制？

有。当前项目里 Tool 调用链路有三层兜底。

第一层是参数和权限失败。如果 Tool 不存在、参数不足、权限不足，服务端不会执行真实 Tool，会返回稳定的 `errorCode` 和 `errorMessage`，并写入 audit。

第二层是 runtime protection。ToolRuntimeProtectionService 支持 timeout、retry 和轻量 circuit breaker。可重试异常会在 `max-retries` 范围内重试；非可重试异常不会重复执行；连续失败达到阈值后会进入 OPEN 状态，短时间内拒绝继续调用，保护下游系统。

第三层是回答阶段 fallback。模型总结失败时可以回退到服务端模板回答；Tool 失败时，最终回答必须保留真实失败原因语义，不能把失败说成成功。

## 4. 状态是怎么保存和恢复的？

项目里按用途分了几类状态。

- Tool audit：支持 in-memory / MySQL，记录 tenantId、userId、runId、toolName、adapterMode、success、errorCode、latencyMs 等，不保存 API Key、token、完整 prompt、完整模型响应。
- Orchestrator run：记录 Tool Calling 的 run、plan、step、stepRef、inputSummary、outputSummary，用于调试和状态查询。
- Workflow run：记录 workflowCode、steps、safeFields、finalAnswer。
- Multi-Agent run：记录 Coordinator、Planner、Knowledge、Tool、Reviewer 的 step、message、metrics、traceSummary。
- Multi-Agent Memory：按 conversationId 保存会话级安全摘要记忆，不保存完整 prompt、rawData 或敏感信息。

当前项目为了面试展示，很多 run store 是 in-memory。生产级可以扩展到 MySQL / Redis，并增加幂等键、恢复策略、状态补偿和定时清理。

## 5. 多 Agent 如何分工、通信和终止？

当前项目的 Multi-Agent 是受控协作，不是多个 Agent 无约束互相聊天。

角色分工：

- CoordinatorAgent：统一调度，创建 run，控制轮次，汇总最终回答。
- PlannerAgent：判断任务类型，例如 RAG_ONLY、TOOL_ONLY、RAG_TOOL、WORKFLOW。
- KnowledgeAgent：复用 RAG retrieve，负责知识片段检索。
- ToolAgent：复用 Tool / Orchestrator，只调用受控只读 Tool。
- ReviewerAgent：检查最终回答是否安全、是否遗漏 Tool 事实、是否保留失败原因。

通信方式不是自由聊天，而是通过结构化 run / step / message 传递安全摘要。比如 ToolAgent 输出 `displaySummary` 和 `safeFields`，ReviewerAgent 只检查这些安全摘要。

终止条件包括：任务完成、Reviewer 通过、达到 maxRounds、达到 maxToolCalls、Tool 失败不可恢复、触发安全限制等。

## 6. 如何避免多个 Agent 相互扯皮或死循环？

企业级 Multi-Agent 不能让 Agent 随便互相对话，必须受控。

我的设计思路是：

- 所有 Agent 都由 Coordinator 统一调度。
- 每个 Agent 有明确职责和输出 schema。
- 通过 maxRounds、maxToolCalls、maxRepairAttempts 做硬约束。
- Reviewer 失败后最多允许一次受控修正，不允许无限自我反思。
- 每个 step 都记录状态，后续 step 只能基于结构化安全摘要继续。
- 出现重复失败、权限失败、runtime circuit open 时，流程要终止或降级。

这也是我没有直接做完全自治 Agent 的原因。企业项目里可控性比“看起来智能”更重要。

## 7. 你怎么设计 Tool Calling？

当前项目的 Tool Calling 是三段式闭环：

1. 模型基于 Tool schema 和用户问题规划 Tool。
2. 服务端解析 toolName、arguments、reason，并通过 ToolInvocationService 执行。
3. 模型基于 Tool display schema 和 execution summary 生成最终中文回答。

Tool 的输入 schema 包括：

- toolName
- description
- parameters
- required fields
- domain
- category
- routeTags
- readOnly
- requiredPermissions

Tool 的输出分两层：

- `rawData`：保留原始业务数据，用于可追溯。
- display schema：给模型和前端优先使用，包括 `displayTitle`、`displaySummary`、`displayFields`、`displayItems`。

这种设计避免模型直接读取大段原始业务对象，也方便前端展示和后续 Orchestrator 扩展。

## 8. 工具调用失败怎么处理？

工具失败不能被模型“美化”。项目中会保留真实失败语义。

处理方式：

- 参数不足：返回参数缺失，当前 step SKIPPED 或 FAILED，不调用真实 Tool。
- 权限不足：返回 403 语义，不执行真实 Tool，写 audit。
- 下游失败：记录真实 errorCode / errorMessage。
- runtime 熔断：返回稳定失败结构，不执行真实 Tool，写 audit。
- answer 阶段：模型可以生成更自然的失败说明，但必须保留真实失败原因。

例如库存服务返回 `Inventory balance not found`，最终回答可以说“库存查询失败，原因是未找到对应库存余额”，而不能说“库存正常”。

## 9. 工具参数错误怎么兜底？

我做了多层参数兜底。

- Planner 阶段要求模型输出结构化 Tool Plan。
- 服务端解析失败时走 fallback。
- ToolInvocationService 做必填参数校验。
- Orchestrator / Workflow 中有 ParameterResolver。
- 二步 Tool 场景中，允许从前置 step 的 safeFields 中白名单提取参数。

项目里有一个典型场景：先调用 `mdm.getMaterial` 查物料，返回的 `id` 作为 `inventory.getBalance` 的 `materialId`；仓库 ID 和库位 ID 从用户问题或 request parameters 解析。resolver 不读取完整 rawData，只读取安全摘要和白名单字段。

## 10. 如何防止模型调用危险工具？

核心是模型不能直接执行工具，只能提出计划。

服务端限制包括：

- ToolDefinition 标记 `readOnly`。
- requiredPermissions / requiredRoles 权限校验。
- MCP 暴露层只暴露白名单只读 Tool。
- ToolInvocationService 统一接入权限、审计、runtime protection。
- requestedTool 虽然优先，但仍必须经过 ToolRegistry 和权限校验。
- 当前阶段不开放写操作 Tool。

即使模型输出了危险 toolName，服务端也会拒绝。

## 11. Tool 是直接暴露给模型，还是通过服务端分发？

通过服务端分发。

模型只看到安全的 Tool schema，不知道内部 HTTP URL、API Key、token、header、adapter 实现。真正执行由 ToolInvocationService 完成，并统一接入权限、审计、超时、重试、熔断、display schema 构建。

这是企业级 Tool Calling 的重要边界：模型负责规划，服务端负责执行和治理。

## 12. 如何判断是 Prompt 问题还是模型能力问题？

我会从可观测链路逐层排查。

1. 固定输入和 Tool schema，看模型是否稳定输出合法 JSON。
2. 降低任务复杂度，看简单问题是否还会错。
3. 换模型或降低 temperature，看结果是否明显改善。
4. 看错误类型：
   - JSON 格式错、字段名错，通常是 prompt / schema / parser 问题。
   - 意图理解错，可能是模型能力或上下文不足。
   - Tool 执行正确但 answer 漏信息，通常是 answer prompt 上下文缺失。
   - RAG 召回不准，可能是 query、chunk、embedding、topK 或 rerank 问题。

项目里曾遇到过二步 Tool 都成功，但 answer 只总结物料没有总结库存的问题。定位后发现 answer summary 没有充分使用 Orchestrator steps，后续通过把多步安全摘要注入 Prompt Context 解决。

## 13. 项目中有没有遇到模型不听指令？

有，典型 bad case 包括：

- 模型没有按 JSON 输出 Tool Plan。
- 模型选错 Tool。
- Tool 已经成功但最终回答遗漏关键事实。
- RAG 没召回时模型编造知识库规则。
- Tool 失败时模型把失败说成成功。

解决方式不是只改 prompt，而是组合治理：

- Prompt 明确输出格式和失败语义。
- 低 temperature 提高稳定性。
- 服务端 parser 和 fallback 兜底。
- display schema 减少大段 rawData 干扰。
- ReviewerAgent 检查最终回答是否遗漏 Tool displaySummary、是否泄露敏感字段、是否掩盖失败原因。
- 必要时进行一次受控修正。

## 14. 解决后有没有评测数据证明有效？

当前项目主要是面试展示和工程实践，评测方式以单元测试、配置隔离测试和固定回归用例为主。

已有覆盖：

- Tool 成功 / 失败分支。
- answer summary 模型失败 fallback。
- display schema 构建。
- RAG + Tool 组合问答。
- 只查物料不触发库存 Tool。
- 查询物料并看库存触发二步 Tool。
- Prompt Context 不泄露 rawData、token、authorization、cookie。
- Multi-Agent Reviewer 识别敏感信息和遗漏事实。
- Memory 只保存安全摘要。

生产级我会再补 golden dataset，统计 Tool 选择准确率、参数准确率、RAG Recall@K、Precision@K、答案忠实度和人工评审通过率。

## 15. 反思结果会不会污染上下文？

会有风险，所以反思不能无脑写入长期记忆。

我的原则：

- 当前 run 的反思只作为临时上下文。
- 长期 Memory 只保存安全摘要。
- 失败原因可以进入 audit，但不能直接变成业务知识。
- 只有经过验证或人工审核的经验，才允许进入可复用经验库。
- Memory 按 conversationId、tenantId、userId 隔离。
- 对 memory-read-limit 和 memory-max-records 做裁剪。

当前项目 Phase 10.5 已实现会话级 in-memory 安全摘要 Memory，但不保存完整 prompt、模型响应、rawData 或敏感字段。

## 16. Claude Code 的 Memory 机制你了解吗？为什么要分层记忆？

了解。Claude Code 这类 coding agent 的 Memory 不是简单保存聊天记录，而是把不同稳定性的上下文分层。

通常可以分为：

- 项目级规则：长期稳定，例如代码规范、架构原则、测试命令。
- 用户偏好：相对稳定，例如回答风格、是否偏好精简。
- 会话上下文：短期有效，例如当前正在改哪个模块。
- 任务状态：当前 run 的执行进度、失败原因、下一步计划。

分层的原因是避免上下文污染。项目级规则应该长期生效，会话中的临时猜测不应该写入长期记忆。上下文太长也会影响性能和模型注意力，所以需要摘要、裁剪、优先级和敏感过滤。

当前项目的 Multi-Agent Memory 也是类似思路，只保存 conversation 级安全摘要，并通过 readLimit 和 maxRecords 控制上下文长度。

## 17. RAG 做过哪些优化？

当前项目实现了 RAG 文档导入、检索、RAG Chat、RAG + Tool 组合问答，支持 in-memory / Milvus、mock / real embedding、registry in-memory / mysql。

优化点包括：

- 文档切分后写入向量库。
- 检索结果返回 `documentId`、`chunkId`、`title`、`source`、`contentSnippet`、`score`。
- contentSnippet 做长度限制和脱敏。
- RAG + Tool 场景下，知识库只解释规则，实时事实以 Tool 为准。
- 无召回时明确提示模型不要编造知识库内容。
- Prompt Context 中把 RAG section 和 Tool section 分区渲染，避免混淆。

## 18. 为什么要加 Rerank？

向量检索擅长语义召回，但不一定能把最相关的 chunk 排到最前。Rerank 的作用是对初召回结果做二次排序，提高 topK 质量。

我一般会用两阶段：

1. 先用向量检索或混合检索召回较多候选，例如 top 20。
2. 再用 rerank 模型或规则对候选排序，取 top 3 到 top 5 进入 prompt。

Rerank 的价值在长文档、术语相近、问题复杂时更明显；缺点是成本和延迟增加。企业项目里可以按场景开关，例如 FAQ 简单问答不一定需要，复杂规则解释可以开启。

## 19. Recall@K 和 Precision@K 怎么取舍？

Recall@K 关注“相关内容有没有被召回”，Precision@K 关注“召回结果里相关内容占比高不高”。

RAG 场景里一般先保证 Recall，再优化 Precision。因为没召回到正确知识，模型很难答对；召回多了还能通过 rerank、裁剪、prompt 约束降低噪声。

取舍方式：

- 问答准确性优先：适当提高 topK，提高 Recall。
- 延迟和成本敏感：降低 topK，提高 Precision。
- 复杂问题：先高召回，再 rerank。
- 简单事实型问题：低 topK 即可。

当前项目支持 topK 和 scoreThreshold，后续可以加动态 topK 和 rerank。

## 20. Top-K 怎么动态调整？

可以根据问题类型和召回质量动态调整。

- 简单定义类问题：topK 取 3。
- 复杂流程、规则解释：topK 取 5 到 8。
- 如果最高分低于阈值，可以扩大 topK 或 query rewrite 后重查。
- 如果 topK 内分数都很高，可以减少进入 prompt 的 chunk，降低 token 成本。
- 如果问题同时需要 RAG + Tool，RAG chunk 数量要控制，避免挤占 Tool 结果上下文。

当前项目里 topK 是请求可选参数，面试演示可以传 3。生产级可以在 intent router 中动态设置。

## 21. 如果 BM25 已经很好，向量检索还有必要吗？

有必要，但不一定所有场景都必须用。

BM25 对关键词精确匹配、编码、术语检索很好，例如物料编码、订单号、字段名。向量检索对语义相似、用户表达不标准、同义词问题更好，例如“还能用多少库存”匹配“库存可用数量口径”。

企业 RAG 最稳的是混合检索：

- BM25 保证关键词命中。
- 向量检索保证语义召回。
- Rerank 做最终排序。

当前项目先做向量检索和基础 RAG，后续可以扩展 hybrid search。

## 22. Agentic RAG 和传统 RAG 有什么区别？

传统 RAG 通常是固定流程：

```text
用户问题 -> 检索 -> 拼 prompt -> 生成答案
```

Agentic RAG 会让 Agent 决定是否需要检索、怎么改写 query、是否多轮检索、是否需要结合 Tool、检索失败后是否换策略。

Query Rewrite 可以算 Agentic RAG 的一个组成能力，但只有 rewrite 本身不一定就是完整 Agentic RAG。真正的 Agentic RAG 还包括策略选择、状态管理、结果评估和失败恢复。

当前项目的 `/api/v1/ai/agent/chat` 已经具备轻量 Agentic RAG 特征：先做 intent routing，再决定 RAG_ONLY、TOOL_ONLY、RAG_TOOL；如果 RAG 无召回，会约束模型不要编造；如果 Tool 成功，会优先使用 Tool 实时事实。

## 23. 多轮检索如何设计？

我会把多轮检索设计成受控循环，而不是无限检索。

基本流程：

1. 初始 query 检索。
2. 判断 retrievedCount 和 score 是否满足阈值。
3. 如果不足，做 query rewrite，例如补充业务关键词、字段名、同义词。
4. 再检索一次。
5. 合并去重、rerank、裁剪。
6. 达到 maxRetrieveRounds 后停止。

关键约束：

- maxRounds 限制成本。
- 每轮保留 query、topK、score、retrievedCount。
- 最终进入 prompt 的 chunk 必须脱敏和截断。
- 检索失败不能让模型编造。

## 24. 检索失败后 Agent 应该怎么调整策略？

可以按几类策略调整：

- 降低 scoreThreshold 或扩大 topK。
- 做 query rewrite。
- 使用关键词检索补充向量检索。
- 如果问题是实时数据类，转向 Tool 查询。
- 如果知识库没有召回，最终回答明确说明“知识库未召回相关规则”。
- 记录 bad case，后续补充文档或调整切分策略。

当前项目里已经做了“不召回不编造”的约束，后续可以加 query rewrite 和 rerank。

## 25. Agentic RAG 的成本和稳定性问题怎么控制？

主要靠约束和观测。

- 限制 maxRetrieveRounds、topK、chunk 长度。
- 对 RAG-only、Tool-only、RAG+Tool 做意图路由，避免所有问题都走最重链路。
- Prompt Context 做 priority、maxLength、truncated 标记。
- RAG 结果进入模型前脱敏和裁剪。
- 记录 retrievedCount、latencyMs、score 分布，方便排查。
- 对复杂 Agentic RAG 开关化，默认用稳定流程，必要时再开启多轮策略。

## 26. 你用过哪些 Agent 框架？LangGraph 和 LangChain 的区别是什么？

当前项目主要使用 Java / Spring AI 体系，没有直接在项目中引入 LangChain 或 LangGraph。但我了解它们的定位。

LangChain 更像一套 LLM 应用开发工具箱，提供模型封装、Prompt、Retriever、Tool、Chain、Agent 等组件，适合快速搭 RAG 和 Tool Calling 应用。

LangGraph 更强调有状态的图编排，核心是 State、Node、Edge、Conditional Edge、Checkpoint，适合做多步骤 Agent、循环、Reviewer、Human-in-the-loop、多 Agent 协作。

简单说：

- LangChain 偏组件和链式调用。
- LangGraph 偏状态机和 Agent 编排。

我当前 Java 项目里的 Orchestrator、Workflow、Multi-Agent Run/Step/Status，思想上更接近 LangGraph 的 StateGraph，只是用 Java 服务端方式自研了受控版本。

## 27. LangGraph 里的 State、Node、Edge 分别解决什么问题？

- State：保存当前图运行中的共享状态，例如用户问题、RAG 结果、Tool 结果、Reviewer 结果。
- Node：执行一个具体步骤，例如 PlannerNode、RetrieverNode、ToolNode、ReviewNode。
- Edge：定义节点之间的流转关系。
- Conditional Edge：根据状态决定下一步，例如 Tool 成功走 Answer，失败走 Repair。

这套模型解决的是“Agent 多步骤执行可控”的问题。相比普通链式调用，它更适合表达分支、循环、重试、审查和中断恢复。

## 28. 为什么选择 LangGraph，而不是 AutoGen、CrewAI 或手写状态机？

如果是 Python Agent 项目，我会优先考虑 LangGraph，原因是：

- 它对状态、节点、边、条件分支表达清晰。
- 更适合企业级可控 Agent，而不是自由聊天式 Multi-Agent。
- Checkpoint、interrupt、human-in-the-loop 等能力对生产化有帮助。
- 和 LangChain 生态衔接自然。

AutoGen 和 CrewAI 更偏多 Agent 协作范式，适合研究和快速实验，但企业业务系统更看重可控性、可观测性、权限和审计。手写状态机简单场景可以，但复杂后会难维护。当前 Java 项目就是先手写受控 Orchestrator，后续如果转 Python，会优先用 LangGraph 表达类似能力。

## 29. 框架能力不满足时你怎么扩展？

我一般不会直接魔改框架，而是包一层 adapter 或 extension。

例如：

- Tool 调用不直接走框架默认工具，而是接入自己的 ToolInvocationService。
- RAG 检索结果进入统一 Prompt Context。
- Memory 不直接保存原文，而是保存安全摘要。
- Reviewer、权限、审计、runtime protection 放在框架外层治理。
- 对框架输出做 schema 校验和 fallback。

这样既能利用框架能力，又不会把企业治理逻辑绑死在框架内部。

## 30. 你怎么看扣子、Dify 这类产品？

这类低代码 Agent 平台的优势是上手快，适合业务人员或应用团队快速搭建知识库问答、流程编排、客服机器人和轻量自动化。

局限是：

- 深度定制复杂业务系统时不够灵活。
- 权限、审计、数据隔离、复杂事务和灰度治理通常需要二次开发。
- 对企业内部已有系统的集成深度有限。
- 流程复杂后可维护性和可测试性会下降。

我会把它们看作快速验证和运营平台，而不是完全替代后端工程。对于核心业务 Agent，我更倾向用 Spring AI / LangGraph 这类代码框架做可控实现，再根据需要对接 Dify 作为运营配置层。

## 31. 低代码 Agent 平台和 LangGraph 这类代码框架怎么选？

如果目标是快速验证、知识库问答、业务人员配置，选 Dify / 扣子更快。

如果目标是深度集成企业业务系统、权限审计、复杂 Tool 编排、生产级可观测，选代码框架更稳。

我当前项目选择 Spring AI + 自研 Orchestrator / Workflow，是因为我想展示企业级 Java 后端如何把 Agent 接入真实业务系统，而不只是搭一个页面配置机器人。

## 32. 平时怎么使用 AI 编程工具？如何保证生成代码质量？

我会把 AI 编程工具当成结对助手，而不是直接替我提交代码。

我的流程：

1. 先给清楚背景、边界、文件路径和测试目标。
2. 让 AI 先读代码结构，再改代码。
3. 生成代码后自己做 review，重点看边界条件、异常分支、安全问题和是否符合项目风格。
4. 补充单元测试或配置绑定测试。
5. 运行影响范围内测试。
6. 对复杂功能补文档和接口示例。

当前项目里我也沉淀了开发规则文档，例如 roadmap、technology decisions、codex working rules、operations docs，用来约束 AI 不偏离既定路线。

## 33. 有没有规则文件、测试、Code Review 兜底？

有。当前项目长期按文档驱动推进，关键文档包括：

- `docs/architecture/ai-agent-roadmap.md`
- `docs/architecture/ai-agent-technology-decisions.md`
- `docs/architecture/ai-agent-codex-working-rules.md`
- `docs/operations/ai-agent-tools.md`
- `docs/architecture/ai-agent-interview-delivery-plan.md`

测试方面，每个阶段都要求不依赖真实模型、真实业务服务、MySQL、Milvus、Embedding API 或外部网络。通过 mock / stub / fake 保证测试稳定。

Code Review 我会重点看：

- 是否绕过权限和审计。
- 是否泄露 prompt、token、rawData。
- 是否改变已有 API 返回结构。
- 是否增加不可控模型行为。
- 是否有测试覆盖成功、失败、fallback 和敏感信息过滤。
