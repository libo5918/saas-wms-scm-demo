# AI Agent Phase 1 Chat API 说明

## 1. 本阶段目标

当前阶段只实现 `scm-ai-agent` 的基础能力，不提前实现 RAG、Tools、MCP、Workflow。

本阶段落地内容：

- 新增 `scm-ai-agent` 服务模块
- 新增基础 Chat API
- 新增 `ModelRouter` 模型路由骨架
- 新增 `ChatModelClient` 模型调用适配层
- 复用 gateway 透传的 `tenantId`、`userId`、`username`、`roles`
- 预留 Spring AI `ChatClient` 接入点
- 默认使用 `mock` provider，避免开发和测试依赖真实模型 API key

## 2. 服务入口

服务名：

```text
scm-ai-agent
```

默认端口：

```text
18087
```

网关路由：

```text
/api/v1/ai/**
```

## 3. Chat API

请求方式：

```http
POST /api/v1/ai/chat
```

推荐通过网关调用：

```http
POST http://localhost:18080/api/v1/ai/chat
Authorization: Bearer <access_token>
Content-Type: application/json
```

请求体：

```json
{
  "message": "帮我分析当前库存是否可以发货",
  "conversationId": "optional-conversation-id",
  "taskType": "simple_chat",
  "requestedModel": "qwen-plus",
  "metadata": {
    "bizNo": "SO-1001"
  }
}
```

最小请求体：

```json
{
  "message": "hello agent"
}
```

响应示例：

```json
{
  "success": true,
  "code": "200",
  "message": "OK",
  "data": {
    "runId": "uuid",
    "conversationId": "uuid",
    "answer": "AI Agent mock response ...",
    "tenantId": 1,
    "userId": 10001,
    "modelName": "qwen-plus",
    "providerModel": "qwen-plus",
    "provider": "dashscope",
    "providerType": "dashscope",
    "providerMode": "mock",
    "taskType": "simple_chat",
    "routeReason": "task_type:simple_chat",
    "fallbackModels": [
      "qwen-turbo"
    ],
    "latencyMs": 12,
    "createdAt": "2026-05-15T00:00:00Z"
  }
}
```

## 4. 身份与租户上下文

`scm-ai-agent` 不直接解析 JWT。

标准链路是：

```text
Client -> scm-gateway -> JwtAuthGlobalFilter -> scm-ai-agent
```

gateway 校验 JWT 后透传：

```text
X-Tenant-Id
X-User-Id
X-User-Name
X-User-Roles
X-Gateway-Internal
X-Gateway-Secret
```

`scm-ai-agent` 通过 `TenantHeaderInterceptor` 读取 `X-Tenant-Id`，通过 Controller 读取用户相关 header。

## 5. 模型路由

Phase 1 第一版路由规则：

- 如果 `requestedModel` 存在，优先按显式模型选择
- 如果存在 `taskType`，按任务类型选择模型
- 如果都没有命中，使用 `ai.agent.default-model`

默认模型配置：

```text
simple_chat -> qwen-plus
rag_qa -> qwen-plus
tool_calling -> qwen-plus
workflow_planning -> openai-compatible-high-quality
complex_reasoning -> openai-compatible-high-quality
summary -> qwen-turbo
```

Phase 2 已在该骨架上补齐 Provider 配置和能力标签路由，详细说明见：

```text
docs/architecture/ai-agent-phase2-model-routing.md
```

## 6. Spring AI 接入方式

当前模块已经引入 Spring AI Chat Client 基础依赖，并预留 `ChatClient.Builder` 调用入口。

默认配置：

```yaml
ai:
  agent:
    provider-mode: mock
```

后续接入真实模型时切换：

```yaml
ai:
  agent:
    provider-mode: spring-ai
```

真实 Qwen、OpenAI-compatible、DeepSeek 的 provider 配置已在 Phase 2 预留，不在仓库中硬编码 API key。

## 7. 当前不做的事情

本阶段刻意不做：

- 不接 Milvus
- 不做 RAG 文档导入
- 不做 Tool Calling
- 不做 MCP Server
- 不做 Workflow 状态机
- 不做 Multi-Agent
- 不引入真实模型 API key

这些能力会按 `docs/architecture/ai-agent-roadmap.md` 的阶段继续推进。
