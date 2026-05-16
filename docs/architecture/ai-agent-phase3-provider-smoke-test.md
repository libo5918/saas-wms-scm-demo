# AI Agent Phase 3 前置：真实 Provider Smoke Test

## 1. 本阶段目标

本阶段不是正式 RAG 阶段，而是 Phase 3 前置能力：先把真实模型 Provider 调用链路跑通。

目标是在保持默认 `mock` 模式的前提下，让 `scm-ai-agent` 可以通过 `providerMode=spring-ai` 调用真实模型。

本阶段落地内容：

- 引入 Spring AI OpenAI-compatible starter
- 引入 Spring AI Alibaba DashScope starter
- 默认关闭真实 ChatModel，避免本地启动和单元测试依赖真实 API Key
- 保留 `mock` adapter，默认请求仍走本地 mock
- `providerMode=spring-ai` 时通过 Spring AI `ChatClient` 调用真实模型
- 调用真实模型时使用路由结果里的 `providerModel`
- 增加关键日志，不打印 prompt 全文、模型响应全文或 API Key
- 增加 smoke test，验证默认 mock 模式和 Provider 配置骨架

本阶段明确不做：

- 不实现 RAG
- 不接入 Milvus
- 不实现 Tools
- 不实现 MCP
- 不实现 Workflow
- 不实现多 Agent 协作
- 不实现同一进程内多个 Provider 的运行时 baseUrl 动态切换

## 2. 默认运行模式

默认配置仍然是安全的 mock 模式：

```yaml
ai:
  agent:
    provider-mode: mock

spring:
  ai:
    model:
      chat: none
```

含义：

- `ai.agent.provider-mode=mock`：AI Agent 默认不访问外部模型
- `spring.ai.model.chat=none`：Spring AI 不创建真实 ChatModel
- 非 Chat 模型也默认设为 `none`，避免语音、图片、Embedding 等自动配置要求 API Key

## 3. Qwen DashScope Smoke Test

DashScope 是当前项目优先支持的真实模型 Provider。

启动前设置环境变量：

```bash
set AI_AGENT_SPRING_AI_MODEL_CHAT=dashscope
set AI_AGENT_DASHSCOPE_ENABLED=true
set AI_AGENT_DASHSCOPE_CHAT_ENABLED=true
set DASHSCOPE_API_KEY=你的DashScopeKey
set AI_AGENT_DASHSCOPE_CHAT_MODEL=qwen-plus
```

请求时指定：

```json
{
  "message": "用一句话介绍当前 SCM AI Agent 项目",
  "providerMode": "spring-ai",
  "requestedModel": "qwen-plus",
  "taskType": "simple_chat"
}
```

说明：

- `DASHSCOPE_API_KEY` 必须来自环境变量或本地未提交配置
- 不要把真实 API Key 写入代码、文档示例或 Git commit
- `requestedModel` 走项目内逻辑模型名称
- `providerModel` 由模型路由结果传给 Spring AI `ChatOptions`

## 4. OpenAI-compatible Smoke Test

OpenAI-compatible 用于 ChatGPT、DeepSeek 或其它兼容接口。

启动前设置环境变量：

```bash
set AI_AGENT_SPRING_AI_MODEL_CHAT=openai
set OPENAI_API_KEY=你的ProviderKey
set AI_AGENT_OPENAI_BASE_URL=https://api.openai.com
set AI_AGENT_OPENAI_CHAT_MODEL=gpt-4.1-mini
```

如果使用 DeepSeek 兼容接口：

```bash
set AI_AGENT_SPRING_AI_MODEL_CHAT=openai
set OPENAI_API_KEY=你的DeepSeekKey
set AI_AGENT_OPENAI_BASE_URL=https://api.deepseek.com
set AI_AGENT_OPENAI_CHAT_MODEL=deepseek-chat
```

请求示例：

```json
{
  "message": "帮我总结模型路由的作用",
  "providerMode": "spring-ai",
  "requestedModel": "deepseek-chat",
  "taskType": "complex_reasoning"
}
```

## 5. 当前边界

当前阶段的真实调用链路是：

```text
Chat API
  -> AgentChatService
  -> ModelRouter
  -> RoutingChatModelClient
  -> Spring AI ChatClient
  -> 当前启用的 ChatModel
```

当前一个进程只建议启用一个真实 ChatModel：

- `spring.ai.model.chat=dashscope`
- 或 `spring.ai.model.chat=openai`

`ModelRouter` 仍会返回不同的逻辑模型和 `providerModel`，但底层真实请求使用当前 Spring AI 激活的 Provider。

如果后续要做到同一进程内按请求动态切换多个 Provider 的 baseUrl，需要继续实现 Provider 专属 `ChatModel` 工厂或 Provider Client Registry。

## 6. 日志规则

本阶段已在核心链路增加日志：

- `AgentChatService`：请求进入、模型路由结果、调用结束
- `DefaultModelRouter`：模型路由选择结果
- `RoutingChatModelClient`：mock / spring-ai 调用开始、结束、失败

日志会记录：

- `tenantId`
- `userId`
- `runId`
- `modelName`
- `providerModel`
- `provider`
- `providerMode`
- `routeReason`
- `latencyMs`
- prompt 长度
- answer 长度

日志不会记录：

- API Key
- Authorization
- Cookie
- prompt 全文
- 模型响应全文

## 7. 验证方式

默认测试：

```bash
mvn -pl scm-ai-agent -am test
```

包含 gateway 验证：

```bash
mvn -pl scm-gateway,scm-ai-agent -am test
```

测试覆盖点：

- 默认 mock 模式可以启动
- 未配置真实 API Key 时不创建真实 ChatModel
- Provider 配置中保留 DashScope 和 OpenAI-compatible 的 API Key 环境变量
- mock adapter 不依赖真实 API Key
- spring-ai 模式缺少 ChatClient 时返回明确业务异常

## 8. 下一步

下一步可以正式进入 RAG 基座：

- 新增 `scm-ai-rag`
- 接入 Milvus Standalone
- 导入 `docs/architecture`
- 实现文档切分、Embedding、检索和引用来源返回

进入 RAG 前仍要保持原则：

- 默认测试不依赖真实模型 API Key
- Milvus 可以通过本地 Docker 或独立环境提供
- RAG metadata 放 MySQL，向量检索放 Milvus
