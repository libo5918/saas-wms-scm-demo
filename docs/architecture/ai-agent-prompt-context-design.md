# AI Agent Prompt Context / Advisor 风格上下文设计

## 1. 设计目标

Phase 5.2 将 RAG + Tool 组合回答从直接拼接字符串升级为结构化上下文治理：

- RAG、Tool、Orchestrator 分别通过 Provider 输出上下文片段。
- Assembler 统一排序、裁剪、脱敏和过滤。
- Renderer 将治理后的上下文渲染为最终模型输入。
- 后续可平滑迁移到 Spring AI Advisor 或自研 advisor chain。

## 2. 核心模型

- `AgentPromptContext`：一次模型回答所需的完整上下文。
- `AgentPromptSection`：单个上下文片段，包含 type、source、title、content、structuredData、priority、maxLength、included、truncated、sensitive。
- `AgentPromptContextType`：支持 `user_message`、`rag_context`、`tool_execution`、`orchestration_steps`、`system_instructions`、`safety_constraints`。
- `AgentPromptContextProvider`：Advisor 风格扩展点，只产出 section，不调用模型。

## 3. Provider 与 Advisor 对应关系

- `RagPromptContextProvider`：对应知识库问答 Advisor，负责注入 RAG retrieve 片段。
- `ToolPromptContextProvider`：对应工具结果 Advisor，负责注入 Tool display schema。
- `OrchestrationPromptContextProvider`：对应执行轨迹 Advisor，负责注入 step summary。
- `SystemInstructionsPromptContextProvider` / `SafetyPromptContextProvider`：对应系统约束 Advisor。

## 4. 安全边界

Prompt Context 不应包含完整原始业务对象、完整模型回包、敏感凭证、内部请求头或调试链路。状态接口和日志也只输出统计信息，例如 section 数量、裁剪数量、RAG 命中数量、Tool 名称和 Orchestration step 数量。

## 5. 后续演进

当项目需要更贴近 Spring AI 原生能力时，可以将各 Provider 包装为 Spring AI Advisor，并在 `ChatClient` 调用前注入上下文。当前设计先保留自研编排层，是为了同时复用现有 RAG、Tool、Orchestrator 能力，并保持面试演示路线的可控推进。
