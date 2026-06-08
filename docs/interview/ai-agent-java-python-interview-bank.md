# AI 应用开发 / Agent 开发岗位面试题库

本文面向“多年 Java 后端经验、Python 初学者、正在转 AI Agent 开发岗位”的候选人准备。问题覆盖项目挖掘、Java 后端八股、AI Agent 高频问题、Spring AI、Python / LangChain / LangGraph、RAG、Tool Calling、Workflow、MCP、Multi-Agent、工程治理和面试表达。

## 一、项目挖掘类

### 1. 你这个 AI Agent 项目解决了什么问题？

参考回答：

这个项目解决的是企业内部“知识解释 + 实时业务查询 + 流程建议”的问题。传统系统只能查数据，普通大模型只能生成文本，容易编造。我做的是把 Spring AI、RAG、Tool Calling、Workflow、MCP、Multi-Agent 结合起来，让模型能先查知识库，再调用受控业务 Tool，最后基于真实数据生成回答。

项目不是简单 ChatBot，而是一个企业级 Agent 工程实践，重点在 Tool 治理、权限审计、runtime protection、上下文脱敏、Workflow 编排和可观测性。

### 2. 你的项目和普通 RAG ChatBot 有什么区别？

参考回答：

普通 RAG ChatBot 主要解决知识库问答，流程通常是检索文档再生成回答。我的项目除了 RAG，还接入了 Tool Calling，可以查真实业务系统，例如物料、库存、订单。并且引入 Orchestrator、Workflow、MCP 和 Multi-Agent，用于工具编排、流程编排、外部工具暴露和多角色协作。

简单说，RAG 解决“知识在哪里”，Tool 解决“实时数据是多少”，Workflow 解决“确定性业务流程怎么走”，Multi-Agent 解决“复杂任务如何分工和审查”。

### 3. 这个项目里最能体现企业级开发的点是什么？

参考回答：

最能体现企业级的是治理能力，不是模型调用本身。包括：

- Tool 不直接暴露给模型，而是通过服务端统一分发。
- Tool 有权限、只读标识、routeTags、audit、timeout、retry、circuit breaker。
- Prompt Context 做上下文裁剪、分区渲染和敏感信息过滤。
- Orchestrator 和 Workflow 都有 run / step / status。
- Multi-Agent 有 maxRounds、maxToolCalls、Reviewer、Memory 和 traceSummary。
- 测试不依赖真实模型和外部服务，保证 CI 稳定。

### 4. 如果面试官问你项目是不是过度设计，怎么回答？

参考回答：

我会区分 Demo、POC 和生产级。这个项目是面向 Java AI Agent 岗位的面试展示项目，确实覆盖了较完整的 Agent 技术栈，但每个模块都控制在最小闭环，不做复杂业务扩展。

例如 Workflow 只做最小 Engine 抽象，不做 BPMN 平台；Multi-Agent 只做受控单轮协作，不做无约束自治；MCP 先做最小 transport，不做复杂 IDE 集成。这样既能展示企业级设计能力，又不会把业务复杂度做得过重。

## 二、Java 后端八股与架构问题

### 5. Spring Boot 和 Spring Cloud 在你的项目里分别解决什么问题？

参考回答：

Spring Boot 负责单服务快速开发，包括 Controller、Service、配置、依赖注入、测试等。Spring Cloud 负责微服务体系里的服务治理，例如 Gateway 路由、Nacos 注册配置、服务间调用、鉴权和统一入口。

在当前项目里，AI Agent 服务本身是 Spring Boot 应用，对外通过 Gateway 18080 暴露接口，方便统一认证、租户上下文和用户上下文传递。

### 6. 你怎么理解幂等？Agent 项目里哪里需要幂等？

参考回答：

幂等是同一个请求重复执行多次，结果和执行一次一致。传统业务里订单、库存、支付需要幂等。Agent 项目里 Tool 调用也需要幂等，尤其是未来开放写操作 Tool 时。

当前项目只开放只读 Tool，风险较低。但 Workflow run、Tool audit、MCP tools/call 仍然需要 runId 做追踪。未来如果做创建单据类 Tool，必须增加 requestId、业务唯一键、状态机和补偿机制。

### 7. 如何设计接口的统一返回结构？

参考回答：

一般会统一成 `success`、`code`、`message`、`data`。这样前端和调用方可以统一判断成功失败。

当前项目的 AI 接口也沿用这种风格，但各模块 data 内部保持自己的语义，例如 Tool Calling 返回 runId、selectedTool、execution、answer；Workflow 返回 workflowCode、steps、finalAnswer；Multi-Agent 返回 agents、steps、metrics、traceSummary。

### 8. 如何做日志和链路追踪？

参考回答：

关键是所有链路都有 runId，并且日志记录上下文但不记录敏感数据。

我会记录 tenantId、userId、runId、toolName、workflowCode、agentName、status、errorCode、latencyMs。不会打印 API Key、Authorization、Cookie、完整 prompt、完整模型响应和完整 rawData。

Agent 项目里 runId 特别重要，因为一次用户请求可能包含 RAG、Tool、Orchestrator、Workflow、Multi-Agent 多个阶段。

## 三、Spring AI 与 Agent 开发

### 9. Spring AI 在项目里主要用来做什么？

参考回答：

Spring AI 主要承担模型调用、ChatClient / 模型路由、Tool Calling Planner 和回答总结能力。项目里默认本地联调使用真实 Spring AI Planner，测试中使用 mock / stub，避免依赖真实模型。

我没有把所有逻辑都塞进 Spring AI，而是在服务端保留 ToolInvocationService、Prompt Context、Orchestrator、Workflow、Multi-Agent 等治理层，这样更适合企业级落地。

### 10. Spring AI Advisor 和你项目里的 Prompt Context 有什么关系？

参考回答：

Advisor 可以理解为模型调用前后的上下文增强和拦截机制。当前项目没有强制切换到 Spring AI Advisor，而是先实现了 Advisor 风格的 Prompt Context。

Prompt Context 由 RagPromptContextProvider、ToolPromptContextProvider、OrchestrationPromptContextProvider 提供 section，再由 Assembler 排序、裁剪、过滤，最后 Renderer 渲染成 prompt。这个设计后续可以平滑迁移到 Spring AI Advisor。

### 11. Spring AI 和 LangChain / LangGraph 的区别怎么讲？

参考回答：

Spring AI 更适合 Java / Spring 生态，方便和 Spring Boot、配置、Bean、测试、企业后端体系集成。

LangChain 是 Python 生态里常用的 LLM 应用组件库，适合快速做 RAG、Tool、Chain。LangGraph 更强调有状态图编排，适合复杂 Agent、循环、Reviewer、多 Agent。

我目前项目用 Spring AI 做 Java 版企业级 Agent。如果用 Python 复刻，我会用 FastAPI + LangChain + LangGraph，把当前 Orchestrator / Multi-Agent 的 run / step / state 思想迁移过去。

## 四、RAG 高频问题

### 12. RAG 的基本流程是什么？

参考回答：

RAG 基本流程是：

1. 文档导入。
2. 文档切分。
3. 生成 embedding。
4. 写入向量库。
5. 用户提问时向量检索。
6. 召回 chunk 后拼入 prompt。
7. 模型基于上下文生成回答。

当前项目支持文档 import、retrieve、rag chat、RAG + Tool 组合问答，向量存储支持 in-memory / Milvus。

### 13. Chunk 怎么切分？

参考回答：

Chunk 要兼顾语义完整性和 token 成本。太小会丢上下文，太大召回不精准、成本高。

一般策略：

- 按标题、段落、列表优先切。
- 保留 source、title、documentId、chunkId。
- 设置 chunkSize 和 overlap。
- 对规则文档、接口文档、FAQ 可以采用不同切分策略。

当前项目面试演示中，SCM/WMS 规则文档会作为知识库，用于解释库存可用数量口径和补货规则。

### 14. RAG 为什么会答错？

参考回答：

主要有几类：

- 没召回正确文档。
- 召回了但排序靠后。
- chunk 太碎或太长。
- prompt 没约束模型忠实使用上下文。
- 用户问题需要实时数据，但系统只走了 RAG。
- 知识库过期或互相冲突。

解决方式包括 query rewrite、hybrid search、rerank、动态 topK、scoreThreshold、知识库版本管理和“不召回不编造”约束。

### 15. RAG 和 Tool 如何结合？

参考回答：

RAG 负责规则解释，Tool 负责实时事实。

例如用户问“按库存可用数量口径解释，并查 MAT-001 在仓库 2001、库位 3001 的库存”。系统先用 RAG 检索“库存可用数量口径”，再调用 `mdm.getMaterial` 获取 materialId，再调用 `inventory.getBalance` 查询实时库存，最后模型综合两部分回答。

最终回答中，业务事实以 Tool 为准，规则解释以 RAG 为准。

## 五、Tool Calling 高频问题

### 16. Tool Calling 的核心风险是什么？

参考回答：

核心风险是模型可能选错工具、传错参数、调用危险工具、泄露敏感信息，或者把失败结果说成成功。

所以 Tool Calling 必须服务端治理：

- Tool 白名单。
- readOnly 标识。
- 权限校验。
- 参数校验。
- 审计。
- 超时、重试、熔断。
- 输出 display schema。
- answer 阶段保留失败原因。

### 17. 为什么 Tool 不应该直接暴露内部接口给模型？

参考回答：

因为模型不应该知道内部 URL、token、header 和系统实现细节。模型只需要知道工具名称、描述和安全 schema。真正执行必须由服务端统一分发。

这样可以保证权限、审计、限流、熔断、脱敏都在服务端完成。

### 18. 如何设计 Tool 的输出？

参考回答：

我会分 rawData 和 display schema。

rawData 保留原始业务对象，用于追溯，但不直接给模型大段使用。display schema 面向模型和前端，包含 title、summary、fields、items。

这样可以降低模型上下文噪声，也便于前端展示。

## 六、Workflow、Orchestrator、Multi-Agent

### 19. Orchestrator 和 Workflow 有什么区别？

参考回答：

Orchestrator 更偏 Agent 工具编排，处理模型规划出来的 Tool 调用、stepRef、候选工具、controlled 二步执行等。

Workflow 更偏确定性业务流程，比如补货建议草案固定是查物料、查库存、生成建议。Workflow 的步骤更稳定，适合业务流程沉淀。

一句话：Orchestrator 管 Agent 动作编排，Workflow 管确定性业务流程编排。

### 20. Multi-Agent 和 Workflow 有什么区别？

参考回答：

Workflow 是流程驱动，步骤通常确定。Multi-Agent 是角色协作驱动，每个 Agent 有职责，例如 Planner、Knowledge、Tool、Reviewer。

但企业级 Multi-Agent 仍然要受控，不能让 Agent 自由聊天。当前项目通过 Coordinator 统一调度，并用 maxRounds、maxToolCalls、Reviewer 和 Memory 控制边界。

### 21. ReviewerAgent 的价值是什么？

参考回答：

ReviewerAgent 负责兜底检查最终回答：

- Tool 成功但答案是否遗漏关键 displaySummary。
- Tool 失败时是否保留 errorMessage。
- RAG 无召回时是否编造“根据知识库”。
- 是否包含 token、authorization、cookie、api key、rawData、prompt 等敏感内容。

它不是为了让系统复杂，而是为了提高最终回答的安全性和忠实度。

## 七、MCP 相关问题

### 22. MCP 解决什么问题？

参考回答：

MCP 可以理解为外部 Agent / IDE / Client 调用工具的一种标准协议。它解决的是 Tool 如何标准化暴露的问题。

当前项目先做 HTTP MCP-style Adapter，再做 JSON-RPC MCP Server transport。无论哪种方式，内部仍复用 ToolInvocationService，不重复造 Tool 执行体系。

### 23. MCP-style Adapter 和标准 MCP Server 有什么区别？

参考回答：

MCP-style Adapter 是项目内 HTTP 风格接口，例如 `/api/v1/ai/mcp/tools` 和 `/api/v1/ai/mcp/tools/{toolName}/invoke`，方便业务系统调用和演示。

标准 MCP Server transport 更贴近 MCP JSON-RPC 语义，例如 `tools/list`、`tools/call`，更适合外部 MCP Client 或 IDE 集成。

两者的共同点是都只暴露安全只读 Tool，并复用 Tool 权限、审计、runtime protection。

## 八、Python / LangChain / LangGraph 迁移问题

### 24. 你是 Java 背景，Python 初学，如何胜任 Agent 开发？

参考回答：

我的优势是已经理解企业级 Agent 的工程化问题，例如 RAG、Tool、Workflow、MCP、Multi-Agent、Memory、权限、审计、熔断和测试隔离。Python 对我来说主要是换一种表达方式。

Java Spring Boot Controller 对应 FastAPI Router，DTO 对应 Pydantic，Spring AI ChatClient 对应 OpenAI SDK / LangChain ChatModel，Orchestrator 对应 LangGraph StateGraph。

我会先用 Python 复刻当前 Java 项目的轻量版：FastAPI + LangChain + LangGraph + RAG + Tool + Reviewer。

### 25. LangGraph 的 StateGraph 怎么理解？

参考回答：

StateGraph 就是有状态的 Agent 流程图。State 保存共享上下文，Node 执行具体动作，Edge 决定流转，Conditional Edge 根据状态做分支。

这和当前 Java 项目里的 MultiAgentRun / MultiAgentStep 很像，只是 LangGraph 在 Python 生态里提供了更标准的表达方式。

### 26. Dify 需要掌握到什么程度？

参考回答：

如果岗位要求 AI 应用开发，Dify 至少要会：

- 创建 Chat 应用。
- 配置知识库。
- 配置 Workflow。
- 配置 HTTP Tool。
- 调试变量传递。
- 发布 API。

但 Dify 是低代码平台，适合快速验证。企业核心系统如果要深度权限、审计、复杂 Tool 治理，还是需要代码框架兜底。

## 九、模型效果与评测

### 27. 如何评估 Agent 效果？

参考回答：

可以分层评估：

- Planner：Tool 选择准确率、参数准确率。
- RAG：Recall@K、Precision@K、MRR、人工相关性评分。
- Tool：成功率、失败原因分布、平均耗时。
- Answer：忠实度、完整性、是否引用真实 Tool 结果。
- Safety：敏感信息泄露率、越权调用率。
- 系统：延迟、成本、fallback 率。

当前项目主要做单元测试和固定回归用例，生产级需要补 golden dataset 和人工评审。

### 28. 如何处理幻觉？

参考回答：

幻觉不能只靠 prompt。

处理方式：

- RAG 无召回时明确不允许编造。
- 实时事实必须来自 Tool。
- Tool 失败必须保留失败原因。
- Prompt Context 分区，让模型知道哪些是知识、哪些是事实。
- ReviewerAgent 检查答案是否忠实。
- 对高风险回答做模板 fallback 或人工确认。

## 十、AI 编程工具与工程实践

### 29. 你怎么使用 AI 编程工具？

参考回答：

我会先写清楚项目文档、阶段目标、边界和测试要求，再让 AI 辅助实现。过程中要求 AI 先读代码结构，再改代码，最后运行影响范围内测试。

我不会直接相信生成结果，会重点 review 安全边界、接口兼容、异常分支、敏感信息泄露和测试覆盖。

### 30. 如何保证 AI 生成代码质量？

参考回答：

主要靠规则文件、测试和 review。

- 规则文件约束架构路线和边界。
- 单元测试覆盖新增功能。
- 配置隔离测试避免真实外部依赖。
- Code Review 检查是否绕过权限、审计、runtime protection。
- 文档记录接口示例和验证方式。

当前项目每个阶段都会要求 Maven test 稳定通过，并且测试不依赖真实模型、真实业务服务、MySQL、Milvus、Embedding API 或外部网络。

## 十一、反问面试官的问题

### 31. 可以反问哪些问题？

建议反问：

- 团队目前 Agent 项目主要是 RAG、Tool Calling，还是 Workflow / Multi-Agent？
- 当前用 Spring AI、LangChain、LangGraph、Dify 还是自研框架？
- Tool 调用是否已经接入权限、审计和熔断？
- RAG 是否有评测集和人工标注数据？
- Agent 是内部提效工具，还是面向客户的生产系统？
- 团队更看重 Java 工程化能力，还是 Python Agent 生态经验？

