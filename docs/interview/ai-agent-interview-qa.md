# AI Agent 高频面试题回答稿

本文根据面试题截图整理，优先结合当前 `saas-wms-scm` 项目的 AI Agent 实现回答；项目暂未覆盖的内容，给出企业级实践中的标准回答口径。

当前项目可结合的核心能力：

- `/api/v1/ai/chat`：基础模型聊天。
- `/api/v1/ai/rag/**`：RAG 文档导入、检索、问答。
- `/api/v1/ai/tool-calling/chat`：Tool Calling 闭环。
- `/api/v1/ai/agent/chat`：RAG + Tool 组合 Agent。
- `/api/v1/ai/tool-calling/orchestrations/**`：Orchestrator run / plan / step 状态。
- `/api/v1/ai/workflows/**`：Workflow Engine + Tool / Summary Step。
- `/api/v1/ai/mcp/**`：MCP-style Tool Adapter。
- Tool 治理：权限、审计、runtime timeout / retry / circuit breaker、display schema。
- Prompt Context：Advisor 风格 Provider / Assembler / Renderer。

## 1. 介绍一下做过的 Agent 项目整体流程

### 短答

我这个项目不是单纯调用大模型接口，而是在一个 SCM/WMS 后端项目里落地企业级 AI Agent。整体流程分三条主线：

1. RAG 负责企业知识，比如库存口径、物料状态、补货规则。
2. Tool Calling 负责实时业务数据，比如查物料、查库存、查订单。
3. Orchestrator / Workflow 负责编排步骤、记录状态、控制风险。

最终形成的闭环是：用户问题进入 Agent，先做意图判断，再按需检索 RAG、规划 Tool、执行 Tool，最后通过 Prompt Context 把知识片段、Tool display schema、编排步骤摘要统一注入模型，让模型生成中文回答。

### 为什么这么拆流程？

因为企业 Agent 里有两类信息：

- 规则类信息：例如“可用库存怎么算”，适合 RAG。
- 实时事实信息：例如“MAT-001 当前库存是多少”，必须走 Tool 查业务系统。

如果全部交给 LLM，容易编造；如果全部写死流程，又缺少自然语言理解能力。所以我把确定性强的部分放到后端流程和 Tool，把需要理解、总结、表达的部分交给 LLM。

### 哪些节点是确定性 Workflow，哪些交给 LLM？

确定性节点：

- 权限校验、租户上下文、用户上下文。
- Tool 是否存在、是否只读、是否允许调用。
- Tool 参数解析和参数校验。
- Tool 执行、审计、runtime retry / circuit breaker。
- Workflow 中固定步骤，比如 `query_material -> query_inventory_balance -> generate_advice`。

交给 LLM 的节点：

- Tool Calling 第一阶段的 Tool 规划。
- Tool 执行后的自然语言总结。
- RAG + Tool 组合回答中的语言组织。
- Workflow Summary 阶段的补货建议草案生成。

### 有没有失败分支和重试机制？

有。项目里 Tool runtime protection 做了三类保护：

- timeout 配置：避免 Tool 长时间卡住。
- retry：只对可重试异常做有限重试，非可重试异常不重复调用。
- circuit breaker：支持 CLOSED / OPEN / HALF_OPEN，失败过多时短期开路，避免持续打爆下游服务。

失败后不会吞掉真实原因，会保留 `errorCode` / `errorMessage`，并写入 Tool audit。Workflow 中如果前置 Tool 失败，Summary 步骤会 `SKIPPED`，不会强行生成完整建议。

### 状态怎么保存和恢复？

当前项目分层保存：

- Tool audit：支持 in-memory / mysql，记录 tenantId、userId、runId、toolName、success、errorCode、latencyMs。
- Orchestrator run store：记录 Tool Calling run / plan / step，用于调试和状态查询。
- Workflow run store：记录 workflow run、steps、safeFields、finalAnswer。

当前为了面试演示，Orchestrator 和 Workflow run 主要是 in-memory，不做长任务恢复。企业生产级可以把 run / step 状态持久化到 MySQL，并增加幂等键、恢复策略和定时补偿。

### 多 Agent 如何分工、通信和终止？

当前项目没有实现 Multi-Agent，但设计上我会这样做：

- Planner Agent：负责理解任务和拆步骤。
- Retrieval Agent：负责 RAG 检索和 query rewrite。
- Tool Agent：负责调用受控 Tool。
- Critic / Reviewer Agent：负责检查回答是否引用了真实数据、是否越权。

通信不建议自由聊天式互相喊话，而是通过共享的 `RunState` / `StepResult` / `Event` 传递结构化状态。终止条件要明确：

- 达到目标。
- 达到 maxSteps / maxCost / maxLatency。
- 出现不可恢复错误。
- Reviewer 判断结果可信。

### 如何避免多个 Agent 相互扯皮或死循环？

核心是把“自由协作”收敛成“受控状态机”：

- 每个 Agent 有明确职责和输出 schema。
- Orchestrator 控制 step 顺序和 maxSteps。
- 每轮执行后写入状态，下一轮只能基于状态推进。
- 增加终止条件和失败降级。
- 对重复计划做去重，比如连续两次选择同一个失败 Tool 就终止或换策略。

当前项目虽然不是 Multi-Agent，但 Orchestrator 的 run / plan / step、stepRef、安全摘要、max executable steps 已经体现了这类治理思路。

## 2. 你怎么设计 Tool Calling？

### 短答

我把 Tool Calling 设计成三段式闭环：

1. 模型基于 Tool schema 和用户问题规划 Tool。
2. 服务端校验权限、参数和 runtime 保护后执行 Tool。
3. 模型基于 Tool display schema 和 execution summary 生成最终中文回答。

核心原则是：Tool 不直接暴露给模型执行，模型只输出计划，真正调用必须经过服务端治理链路。

### Tool 的输入输出 schema 怎么定义？

输入 schema：

- toolName
- description
- parameters
- required fields
- domain / category / routeTags
- readOnly
- requiredPermissions

输出 schema：

- 业务原始结果保留在 `rawData`，用于可追溯。
- 模型和前端优先使用 `display schema`：
  - displayTitle
  - displaySummary
  - displayFields
  - displayItems
  - rawData

项目里 `mdm.getMaterial`、`inventory.getBalance` 等 Tool 都会转换为统一 display schema，避免模型直接读大段原始业务对象。

### 工具调用失败怎么处理？

失败分三类：

- 参数错误：返回稳定错误语义，不调用真实 Tool 或调用失败后保留原因。
- 权限失败：不执行真实 Tool，写 audit，返回 403 语义。
- 下游失败：保留真实失败原因，runtime protection 记录失败次数，必要时进入 circuit open。

最终 answer 阶段不会掩盖失败，而是用自然语言解释：“库存查询失败，原因是……”

### 工具参数错误怎么兜底？

项目里用几层兜底：

- Planner 阶段让模型按 JSON 输出参数。
- 服务端 `ToolPlanParser` 解析失败时走 fallback。
- Tool executor 做参数校验。
- Orchestrator / Workflow 的 parameter resolver 支持从用户问题、前置 step safeFields 中解析白名单参数。

例如 `mdm.getMaterial` 返回的 `id` 会作为 `inventory.getBalance` 的 `materialId`，仓库 id 和库位 id 从用户问题或 parameters 中解析。

### 如何防止模型调用危险工具？

关键是模型不直接执行工具。服务端有治理：

- ToolDefinition 标记 `readOnly`。
- `requiredPermissions` / `requiredRoles` 做权限控制。
- MCP-style adapter 只暴露白名单只读 Tool。
- requestedTool 虽然优先，但仍要经过 ToolRegistry、Permission、Runtime Protection。
- 写操作 Tool 当前不开放给 Agent。

所以即使模型输出了危险 toolName，服务端也会拒绝。

### Tool 是直接暴露给模型，还是通过服务端分发？

通过服务端分发。模型只看到安全的 Tool schema，不知道内部 HTTP URL、token、header、adapter 细节。真实调用由 `ToolInvocationService` 完成，并统一接入权限、审计、runtime protection。

这是企业级项目里必须坚持的边界：模型负责规划，服务端负责执行和治理。

### 如何判断是 Prompt 问题还是模型能力问题？

我一般按四步排查：

1. 固定输入和 Tool schema，看模型是否稳定输出目标 JSON。
2. 降低任务复杂度，如果简单问题也错，可能是 schema 或 prompt 不清晰。
3. 换更强模型或降低 temperature，如果明显改善，说明模型能力占比更大。
4. 看错误类型：
   - 字段名错、JSON 格式错：多半是 prompt / schema / parser 约束问题。
   - 业务意图理解错：可能是模型能力或上下文不足。
   - 正确规划但回答错：可能是 answer prompt 或上下文注入问题。

项目里通过 mock / spring-ai 双模式、template / spring-ai answer 双模式，可以隔离模型问题和服务端流程问题。

## 3. 项目中有没有遇到模型不听指令？

### 短答

会遇到，尤其是让模型输出结构化 Tool Plan 或基于 Tool 结果总结时。我的处理不是只靠 prompt，而是 prompt、模型参数、parser、后处理和流程约束一起做。

### 具体 bad case

典型 bad case：

- 模型没有按 JSON 输出 Tool Plan。
- 模型选错 Tool，比如用户问库存却只查物料。
- Tool 已经查到库存，但 answer 只总结了第一步物料结果。
- RAG 没召回时，模型编造知识库规则。
- Tool 失败时，模型把失败说成成功。

项目里曾出现过二步 Tool 都成功，但 answer summary 没总结第二步库存信息的问题，最后是通过把 Orchestrator steps 安全摘要注入 Prompt Context，并让 answer summary 优先读取多步骤 summary 来解决。

### 怎么定位？

定位时我会看：

- planner 输入：Tool schema 是否完整、候选 Tool 是否被过滤错。
- planner 输出：toolName、arguments、reason 是否正确。
- Tool execution：success、errorCode、data 是否正确。
- display schema：是否把关键字段暴露给模型。
- answer prompt：是否包含第二步 Tool 结果或 RAG 片段。
- final answer：是否忠实使用上下文。

项目里每一步都有 runId、selectedTool、execution、orchestration steps，可追踪。

### 通过 Prompt、模型参数、后处理还是流程约束解决？

优先级是：

1. 流程约束：危险操作、权限、写操作不能靠 prompt，必须服务端拦截。
2. Schema 和 parser：结构化输出尽量用明确 schema 和解析器兜底。
3. Prompt：明确角色、上下文、输出要求、失败语义。
4. 模型参数：低 temperature 提升稳定性。
5. 后处理：校验字段、补充模板 fallback。

### 解决后有没有评测数据证明有效？

项目当前主要是单元测试和回归用例，不是大规模离线评测。已有测试覆盖：

- Tool 成功和失败分支。
- answer summary fallback。
- RAG + Tool intent router。
- 只查物料不触发库存第二步。
- 查物料并看库存触发二步执行。
- Prompt Context 不泄露 rawData、token、authorization、cookie。

生产级会继续补一套 golden dataset，统计 Tool 选择准确率、参数准确率、最终回答忠实度。

### 反思结果会不会污染上下文？

会有风险。反思内容如果直接进入长期记忆，可能把错误推理也保存下来。我的原则是：

- 反思只进入当前 run 的临时上下文。
- 只有经过验证的事实、规则、偏好才能进入长期记忆。
- 失败原因可以记录到 audit，但不直接作为业务知识。
- Prompt Context 要区分 source、priority、sensitive、included、truncated。

当前项目的 Prompt Context 已有 section 来源和敏感过滤，后续可以扩展记忆分层。

## 4. Claude Code 的 Memory 机制你了解吗？

### 短答

了解。Claude Code / AI Coding Agent 的 Memory 本质是把长期项目规则、用户偏好和当前会话上下文分层管理，避免每次都从零开始，也避免把临时信息污染长期记忆。

### 为什么要分层记忆？

因为不同信息生命周期不同：

- 项目级规则：长期有效，比如代码规范、目录结构、架构约束。
- 用户偏好：中长期有效，比如回答风格、是否要中文、是否要先跑测试。
- 会话上下文：短期有效，比如这次任务改了哪些文件。
- 临时推理：只用于当前步骤，不应该持久化。

混在一起会导致上下文膨胀、过期信息干扰、错误经验污染后续任务。

### 项目级规则、用户偏好、会话上下文怎么隔离？

企业级实践可以这样分：

- Repo docs：项目级规则，例如本项目的 `ai-agent-codex-working-rules.md`。
- User memory：用户偏好，例如中文回答、阶段推进方式。
- Session memory：当前任务目标、已执行命令、测试结果。
- Scratchpad：临时推理，不落盘。

当前项目通过多份架构文档和 operations 文档承载项目级记忆，Phase 8.1 又新增 demo guide 作为面试演示记忆。

### 上下文过长性能下降怎么处理？

处理方式：

- 检索式加载：只加载相关文档，不把所有文档塞给模型。
- 摘要压缩：长历史转成结构化摘要。
- 优先级裁剪：保留系统规则、当前任务、关键文件，裁掉低优先级历史。
- 分层上下文：长期规则、短期状态、临时推理分开。

项目里的 Prompt Context Assembler 就是类似思路：section 有 priority、maxLength、included、truncated、sensitive。

## 5. 你的 RAG 做过哪些优化？

### 短答

当前项目已经做了基础工程优化：文档切片、metadata、scoreThreshold、topK、Milvus / in-memory 双模式、mock / real embedding 隔离、检索结果脱敏和长度裁剪。更深入的优化包括 query rewrite、hybrid search、rerank、动态 topK 和离线评测。

### 为什么要加 Rerank？

向量召回强调召回率，topK 前几条不一定最适合回答。Rerank 可以在初筛后用更强的相关性模型重新排序，提高 Precision@K，减少无关 chunk 进入上下文。

典型流程：

1. 向量 / BM25 召回 top50。
2. Rerank 选 top5。
3. 再进入 Prompt Context。

当前项目还没接 rerank，但 Prompt Context 已经预留了 section 裁剪和优先级机制。

### Recall@K 和 Precision@K 怎么取舍？

- Recall@K：答案相关文档是否被召回，适合衡量“不漏”。
- Precision@K：召回结果里有多少真正有用，适合衡量“不乱”。

企业问答一般先保证 Recall，再通过 rerank 和裁剪提高 Precision。因为召不回来，后面模型再强也没用；召太多无关内容，又会污染回答。

### TopK 怎么动态调整？

可以按问题类型调整：

- 简单事实问答：topK 小一点，比如 3。
- 流程解释 / 规则总结：topK 大一点，比如 5 到 8。
- 召回分数很高且集中：减少 topK。
- 分数低或分散：扩大 topK，或触发 query rewrite。

项目里的 API 已支持 topK 和 scoreThreshold，后续可以在 intent router 中动态设置。

### 如果 BM25 已经很好，向量检索还有必要吗？

有必要，但不一定永远优先。BM25 擅长关键词精确匹配，比如物料编码、接口名、错误码；向量检索擅长语义匹配，比如“还能用多少库存”匹配“库存可用数量口径”。

企业级 RAG 最好做 hybrid：

- BM25 负责精确召回。
- 向量负责语义召回。
- Rerank 统一排序。

当前项目先做向量检索和 Milvus，是为了展示 AI Agent 中语义检索链路；后续可以补 BM25 / hybrid。

## 6. Agentic RAG 和传统 RAG 有什么区别？

### 短答

传统 RAG 是一次性流程：用户问题 -> 检索 -> 拼上下文 -> 回答。Agentic RAG 是由 Agent 动态决定是否改写 query、是否多轮检索、是否调用 Tool、是否验证答案。

### Query Rewrite 是否算 Agentic？

单独的 Query Rewrite 不一定算 Agentic。如果只是固定预处理，它仍是传统 RAG 的增强。如果由 Agent 根据召回结果动态决定是否 rewrite、怎么 rewrite、是否追加检索，那就更接近 Agentic RAG。

### 多轮检索如何设计？

可以设计为受控循环：

1. 首轮检索。
2. 判断召回数量、分数、覆盖度。
3. 如果不足，生成改写 query。
4. 再检索。
5. 合并、去重、rerank。
6. 达到 maxRounds 或质量阈值后停止。

不能无限检索，必须有 maxRounds、maxLatency、maxCost。

### 检索失败后 Agent 怎么调整策略？

可以按顺序降级：

- 放宽 scoreThreshold。
- 扩大 topK。
- Query rewrite。
- 尝试关键词检索 / BM25。
- 切换知识库或提示用户补充范围。
- 如果仍失败，明确告诉用户“知识库未召回相关内容”，不能编造。

项目里的 RAG + Tool answer prompt 已要求：RAG 没召回时不要编造知识库内容。

### Agentic RAG 的成本和稳定性怎么控制？

控制点：

- 限制 maxRounds。
- 限制 topK 和上下文长度。
- 缓存 embedding 和检索结果。
- 对 rewrite / rerank 设开关。
- 对每轮检索记录 trace。
- 低置信度时降级为传统 RAG 或提示用户。

当前项目更偏“受控 Agentic”，没有让模型无限自主循环，符合面试展示优先和稳定性优先。

## 7. 你用过哪些 Agent 框架？

### 短答

这个项目核心是 Java / Spring Boot 自研 Agent 编排层，结合 Spring AI 做模型接入。没有直接用 LangGraph，但我了解 LangGraph、LangChain、AutoGen、CrewAI 的定位。

### LangGraph 和 LangChain 的区别是什么？

- LangChain 更像组件库，提供 LLM、Prompt、Retriever、Tool、Chain 等积木。
- LangGraph 更像有状态图执行框架，强调 State、Node、Edge、条件分支、循环和可恢复执行。

如果只是简单 RAG Chain，LangChain 足够；如果是多步 Agent、循环、条件分支、状态恢复，LangGraph 更合适。

### LangGraph 里的 State、Node、Edge 解决什么问题？

- State：保存全局执行状态，比如用户问题、检索结果、Tool 结果、消息历史。
- Node：一个执行步骤，比如检索、调用 Tool、模型总结。
- Edge：步骤之间的流转，可以是固定边，也可以是条件边。

它解决的是 Agent 流程从“自由调用”变成“可控状态图”的问题。

### 为什么选择 LangGraph，而不是 AutoGen、CrewAI 或手写状态机？

如果是 Python 项目，我会这样选：

- LangGraph：适合生产可控 Agent，状态、分支、循环更清晰。
- AutoGen：适合多 Agent 对话协作实验。
- CrewAI：适合角色分工式 demo，抽象更上层。
- 手写状态机：适合流程非常简单、强定制、团队不想引入框架。

当前 Java 项目没有直接用 LangGraph，是因为目标是 Java AI Agent 面试展示，核心能力要落在 Java 后端里。所以我在项目里实现了轻量 Orchestrator 和 Workflow Engine，借鉴的是 LangGraph 的状态图思想，但没有引入 Python 框架。

### 框架能力不满足时怎么扩展？

原则是把框架当执行骨架，不把业务治理塞进 prompt：

- 自定义 Node / Tool wrapper。
- 扩展 State schema。
- 增加 checkpoint / persistence。
- 增加 callback / tracing。
- 把权限、审计、限流、熔断放在服务端工具层。

本项目的 `ToolInvocationService`、`ToolRuntimeProtectionService`、`WorkflowStepExecutor` 就是类似的扩展点。

## 8. 你怎么看扣子这类低代码 Agent 产品？

### 短答

低代码 Agent 平台适合快速 demo、运营配置和非研发人员搭流程；但复杂企业系统里，权限、审计、数据边界、私有业务逻辑、测试和部署治理通常还是要落在后端工程里。

### 优势

- 搭建快。
- 可视化流程，方便运营和产品参与。
- 内置模型、知识库、插件市场。
- 适合验证 idea 和轻量客服类场景。

### 局限

- 复杂权限和租户隔离不一定贴合企业内部系统。
- Tool 调用链路、审计、熔断、幂等不一定可控。
- 复杂业务状态和事务边界难表达。
- 测试、版本管理、灰度发布、回滚能力有限。
- 容易形成平台锁定。

### 和 LangGraph / 自研 Java Agent 怎么选？

我会按场景选：

- 快速验证、轻量客服、运营配置：低代码平台。
- 多步推理、有状态 Agent、Python 生态：LangGraph。
- 企业核心业务系统、强权限审计、Java 技术栈：Java 后端自研编排 + Spring AI。

本项目选择 Java 自研主链路，是为了展示企业级 Agent 工程能力；低代码平台可以作为外部演示入口，但不替代核心实现。

## 9. 平时怎么使用 AI 编程工具？

### 短答

我把 AI 编程工具当成协作开发助手，不是直接生成完就合并。我的流程是：先给规则和上下文，再让它改小范围代码，最后通过测试、代码审查和文档确认质量。

### 大概流程

1. 先让 AI 阅读项目文档和相关代码。
2. 明确目标、边界、不能破坏的接口。
3. 要求它给出实现方案或直接小步修改。
4. 每次改动后跑单元测试。
5. 检查 git diff，确认没有无关重构。
6. 更新文档和接口示例。
7. 总结改了什么、怎么验证、下一步做什么。

当前项目就是按 Phase 推进，每阶段都有目标、边界、测试、文档和 commit 文案。

### 如何保证 AI 生成代码质量？

核心手段：

- 规则文件：例如本项目的 `ai-agent-codex-working-rules.md`。
- 小步提交：每个 Phase 控制范围。
- 测试兜底：`mvn -pl scm-ai-agent -am test`。
- 接口兼容：明确不改变已有返回结构。
- 安全边界：不泄露 token、API Key、rawData、完整 prompt。
- Code Review：检查命名、职责、异常、日志、配置、测试。

### 有没有规则文件、测试、Code Review 兜底？

有。本项目里：

- 规则文件：`docs/architecture/ai-agent-codex-working-rules.md`。
- 路线文档：roadmap、technology decisions、prompt context design、workflow design、mcp design。
- 测试：scm-ai-agent 当前 155 个测试，覆盖 Controller / Service / 配置隔离。
- 文档：每阶段都更新 operations 文档和 gateway 18080 示例。

这也是我认为 AI 编程工具适合企业开发的方式：让 AI 提效，但质量门禁仍然由工程规范、测试和 review 控制。

## 10. 面试收尾总结

可以用这段话收尾：

“我做这个项目时，不是只追求大模型效果，而是按企业级 Agent 落地拆成几层：RAG 解决知识，Tool 解决实时数据，Prompt Context 解决上下文治理，Orchestrator 解决 Agent Tool 调用过程治理，Workflow 解决确定性业务流程，MCP-style adapter 解决外部 Agent 调用内部 Tool 的标准化问题。每一层都考虑了权限、审计、runtime protection、配置隔离和测试稳定性。所以它不是一个单点 demo，而是一套 Java 后端里可演进的 AI Agent 工程样板。”
