# AI Agent Phase 2 模型 Provider 与动态路由说明

## 1. 本阶段目标

本阶段在 Phase 1 的 `scm-ai-agent` 基础上，补齐模型 Provider 配置骨架和动态模型路由能力。

本阶段落地内容：

- 增加模型 Provider 配置结构
- 预留 Qwen DashScope、OpenAI-compatible、DeepSeek-compatible provider
- 保持默认 `mock` 模式，默认启动和单元测试不依赖真实 API Key
- 增强 `ModelRouter`，支持 `requestedModel`、`taskType`、模型能力标签、`fallbackModels`、`providerMode`
- Chat API 响应返回本次选择的逻辑模型、真实 provider 模型、provider 类型和 fallback 列表

本阶段明确不做：

- 不实现 RAG
- 不接入 Milvus
- 不实现 Tools
- 不实现 MCP
- 不实现 Workflow
- 不实现多 Agent 协作

## 2. Provider 配置

当前配置入口：

```yaml
ai:
  agent:
    provider-mode: mock
    default-model: qwen-plus
    providers:
      - name: mock
        type: mock
        enabled: true
      - name: dashscope
        type: dashscope
        enabled: true
        base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
        api-key-env: DASHSCOPE_API_KEY
      - name: openai
        type: openai-compatible
        enabled: true
        base-url: https://api.openai.com/v1
        api-key-env: OPENAI_API_KEY
      - name: deepseek
        type: openai-compatible
        enabled: true
        base-url: https://api.deepseek.com
        api-key-env: DEEPSEEK_API_KEY
```

字段说明：

- `provider-mode`：当前模型调用模式，默认 `mock`
- `providers.name`：Provider 唯一名称，模型配置通过该名称关联 Provider
- `providers.type`：Provider 类型，例如 `dashscope`、`openai-compatible`、`mock`
- `providers.base-url`：OpenAI-compatible 接口地址
- `providers.api-key-env`：真实 API Key 所在环境变量名称，不把真实 key 写入仓库

## 3. 模型配置

当前预留模型：

```yaml
models:
  - name: qwen-plus
    provider-model: qwen-plus
    provider: dashscope
    provider-modes: [mock, spring-ai]
    capabilities: [CHAT, RAG, TOOL_CALLING, STRUCTURED_OUTPUT]
    task-types: [simple_chat, rag_qa, tool_calling]
    fallback-models: [qwen-turbo, chatgpt-main]

  - name: qwen-turbo
    provider-model: qwen-turbo
    provider: dashscope
    provider-modes: [mock, spring-ai]
    capabilities: [CHAT, LOW_COST]
    task-types: [summary]
    fallback-models: [chatgpt-main]

  - name: chatgpt-main
    provider-model: ${AI_OPENAI_CHAT_MODEL:gpt-4.1-mini}
    provider: openai
    provider-modes: [mock, spring-ai]
    capabilities: [CHAT, TOOL_CALLING, STRUCTURED_OUTPUT, PLANNING, HIGH_QUALITY]
    task-types: [workflow_planning, complex_reasoning]
    fallback-models: [qwen-plus, deepseek-chat]

  - name: deepseek-chat
    provider-model: deepseek-chat
    provider: deepseek
    provider-modes: [mock, spring-ai]
    capabilities: [CHAT, STRUCTURED_OUTPUT, PLANNING]
    task-types: [complex_reasoning, code_reasoning]
    fallback-models: [qwen-plus]
```

字段说明：

- `name`：项目内逻辑模型名称，接口层和路由层使用该名称
- `provider-model`：Provider 侧真实模型名称，可通过配置替换
- `provider`：绑定的 Provider 名称
- `provider-modes`：该模型支持的调用模式
- `capabilities`：模型能力标签，用于能力路由
- `task-types`：模型优先承接的任务类型
- `fallback-models`：当前模型失败后可降级尝试的逻辑模型列表
- `priority`：路由优先级，数值越小越优先

## 4. 路由策略

当前 `DefaultModelRouter` 的路由顺序：

1. `requestedModel` 显式指定模型
2. `taskType` 任务类型匹配
3. `requiredCapabilities` 能力标签匹配
4. `defaultModel` 默认模型兜底

路由过滤条件：

- 模型必须启用
- 模型绑定的 Provider 必须启用
- 模型必须支持本次 `providerMode`
- 如果请求指定 `costLevel`，模型成本等级必须匹配
- 如果请求指定 `maxLatencyMs`，模型配置的 `maxLatencyMs` 不能超过请求限制

## 5. Chat API 请求扩展

请求示例：

```json
{
  "message": "帮我规划一个库存异常诊断流程",
  "taskType": "workflow_planning",
  "requestedModel": "chatgpt-main",
  "providerMode": "mock",
  "requiredCapabilities": [
    "CHAT",
    "PLANNING"
  ],
  "costLevel": "high",
  "maxLatencyMs": 30000
}
```

响应新增字段：

```json
{
  "modelName": "chatgpt-main",
  "providerModel": "gpt-4.1-mini",
  "provider": "openai",
  "providerType": "openai-compatible",
  "providerMode": "mock",
  "routeReason": "requested_model",
  "fallbackModels": [
    "qwen-plus",
    "deepseek-chat"
  ]
}
```

## 6. Mock 与真实调用边界

默认模式：

```yaml
ai:
  agent:
    provider-mode: mock
```

默认 `mock` 模式不会访问外部模型，也不要求配置真实 API Key。

真实模型调用预留模式：

```yaml
ai:
  agent:
    provider-mode: spring-ai
```

`spring-ai` 模式当前只保留 `ChatClient.Builder` 调用骨架。后续如果要做到每次请求动态切换底层模型，需要继续在 `RoutingChatModelClient` 中按 provider 构造或选择具体 `ChatModel`。

## 7. 验证方式

单模块验证：

```bash
mvn -pl scm-ai-agent -am test
```

包含网关模块验证：

```bash
mvn -pl scm-gateway,scm-ai-agent -am test
```

测试覆盖点：

- 显式指定模型路由
- 按任务类型路由
- 按能力标签路由
- `providerMode` 请求覆盖
- fallback 模型列表返回
- mock 模式不依赖真实 API Key
- `spring-ai` 模式缺少 `ChatClient.Builder` 时给出明确异常

## 8. 后续阶段

下一阶段建议进入 RAG 基座，但仍要保持边界清晰：

- 新增 `scm-ai-rag`
- 接入 Milvus Standalone
- 导入 `docs/architecture` 文档
- 实现文档切分、Embedding、检索和引用来源返回

不要在 Phase 2 代码中提前塞入 RAG、Tools、MCP、Workflow 逻辑。
