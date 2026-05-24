# AI Agent 面试演示指南

## 1. 演示定位

本指南用于 Phase 8.1 面试交付收敛。当前项目已经具备一条可运行、可讲解、可演示的 Java 企业级 AI Agent 闭环：

- 基础 LLM Chat
- RAG 知识库检索与问答
- Tool Calling 与真实业务 Tool 执行
- RAG + Tool 组合 Agent Chat
- Prompt Context / Advisor 风格上下文治理
- Orchestrator 受控 Tool 编排
- Workflow 业务流程编排
- MCP-style Tool Adapter
- 权限、审计、runtime protection、配置隔离和稳定测试

面试时建议强调：项目不是只调用大模型接口，而是把企业应用中常见的认证、租户、权限、审计、稳定性、可观测性、RAG、Tool、Workflow 和外部工具暴露统一到了 Java 后端工程里。

## 2. 推荐讲解顺序

1. 项目背景：SaaS WMS / SCM 后端项目，围绕物料、仓库、库存、采购、销售构建业务语境。
2. 技术架构：Spring Boot 多模块、Gateway、租户上下文、统一返回体、配置隔离。
3. RAG：文档导入、切片、Embedding、Milvus / in-memory 双模式、知识库问答。
4. Tool Calling：模型规划 Tool，服务端执行真实业务 Tool，模型总结最终 answer。
5. 工程治理：Tool 权限、审计、runtime timeout / retry / circuit breaker、display schema。
6. Agent Chat：用 RAG 解释规则，用 Tool 查询实时业务数据，用 Prompt Context 统一注入上下文。
7. Orchestrator：记录 run / plan / step，支持受控二步只读 Tool 执行和 stepRef 安全摘要。
8. Workflow：固定业务流程补货建议，复用 Tool 和 RAG，展示业务流程编排。
9. MCP-style Adapter：把内部已治理 Tool 以标准化方式暴露给外部 Agent / IDE / Client。
10. 后续扩展：标准 MCP Server、Multi-Agent、长任务编排、外部平台集成。

## 3. 本地启动与推荐配置

推荐面试演示统一走 gateway：

- Gateway：`http://localhost:18080`
- AI Agent：`http://localhost:18087`

本地 profile 重点：

- `scm-ai-agent/src/main/resources/application.yml`
  - 默认 `provider-mode=mock`
  - 默认 RAG embedding 使用 mock
  - 默认 Tool audit 使用 in-memory
  - 默认 Tool Calling planner 为 mock，answer 为 template
- `scm-ai-agent/src/main/resources/application-local.yml`
  - local profile 默认启用真实 DashScope Chat / Embedding
  - RAG vector store 默认 Milvus
  - Tool Calling 默认 `planner-mode=spring-ai`
  - Tool Calling 默认 `answer-mode=spring-ai`
  - Orchestrator 默认开启 controlled 二步只读执行

推荐演示配置：

```yaml
ai:
  agent:
    provider-mode: spring-ai
    tool-calling:
      planner-mode: spring-ai
      answer-mode: spring-ai
      orchestrator:
        enabled: true
        plan-mode: MULTI_STEP_CONTROLLED
        multi-step-enabled: true
        controlled-execution-enabled: true
        max-executable-steps: 2
```

如果真实模型不可用，可切回 mock，并在面试中说明：单元测试和本地兜底不依赖外部模型、MySQL、Milvus、Embedding API 或外部网络，这是企业项目配置隔离和稳定测试的体现。

## 4. 统一请求头

后续所有 gateway 18080 示例默认使用：

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
X-Tenant-Id: 1
X-User-Id: 10001
X-Username: admin
X-User-Roles: ROLE_ADMIN
```

示例中的 `<accessToken>` 是占位符，不要在文档中保存真实 token。

## 5. 完整 Gateway 18080 演示顺序

### 5.1 基础 LLM Chat

```http
POST http://localhost:18080/api/v1/ai/chat
Authorization: Bearer <accessToken>
Content-Type: application/json
X-Tenant-Id: 1
X-User-Id: 10001
X-Username: admin
X-User-Roles: ROLE_ADMIN
```

```json
{
  "message": "用一句话介绍当前 SCM AI Agent 项目",
  "taskType": "simple_chat"
}
```

关键预期返回字段：

```json
{
  "success": true,
  "data": {
    "answer": "模型返回的中文回答",
    "model": "qwen-plus",
    "provider": "dashscope",
    "latencyMs": 100
  }
}
```

### 5.2 RAG 文档导入

```http
POST http://localhost:18080/api/v1/ai/rag/import/docs
Authorization: Bearer <accessToken>
Content-Type: application/json
X-Tenant-Id: 1
X-User-Id: 10001
X-Username: admin
X-User-Roles: ROLE_ADMIN
```

```json
{
  "knowledgeBaseId": "kb-scm-demo",
  "scanRoot": "docs/examples",
  "maxFiles": 20,
  "overwrite": true
}
```

关键预期返回字段：

```json
{
  "success": true,
  "data": {
    "knowledgeBaseId": "kb-scm-demo",
    "importedCount": 1,
    "chunkCount": 1
  }
}
```

### 5.3 RAG Retrieve

```http
POST http://localhost:18080/api/v1/ai/rag/retrieve
Authorization: Bearer <accessToken>
Content-Type: application/json
X-Tenant-Id: 1
X-User-Id: 10001
X-Username: admin
X-User-Roles: ROLE_ADMIN
```

```json
{
  "knowledgeBaseId": "kb-scm-demo",
  "query": "库存可用数量口径是什么",
  "topK": 3,
  "scoreThreshold": 0.1
}
```

关键预期返回字段：

```json
{
  "success": true,
  "data": {
    "knowledgeBaseId": "kb-scm-demo",
    "retrievedCount": 1,
    "chunks": [
      {
        "title": "SCM/WMS 规则示例知识库",
        "source": "docs/examples/scm-wms-rules.md",
        "contentSnippet": "库存可用数量通常等于现存数量减去锁定数量...",
        "score": 0.7
      }
    ]
  }
}
```

### 5.4 RAG Chat

```http
POST http://localhost:18080/api/v1/ai/rag/chat
Authorization: Bearer <accessToken>
Content-Type: application/json
X-Tenant-Id: 1
X-User-Id: 10001
X-Username: admin
X-User-Roles: ROLE_ADMIN
```

```json
{
  "knowledgeBaseId": "kb-scm-demo",
  "question": "请解释库存可用数量和锁定数量的区别",
  "topK": 3,
  "scoreThreshold": 0.1
}
```

关键预期返回字段：

```json
{
  "success": true,
  "data": {
    "answer": "模型基于知识库片段生成的中文回答",
    "retrievedCount": 1,
    "chunks": []
  }
}
```

### 5.5 Tool Calling Chat

```http
POST http://localhost:18080/api/v1/ai/tool-calling/chat
Authorization: Bearer <accessToken>
Content-Type: application/json
X-Tenant-Id: 1
X-User-Id: 10001
X-Username: admin
X-User-Roles: ROLE_ADMIN
```

```json
{
  "runId": "run-demo-tool-chat-001",
  "message": "帮我查物料 MAT-001",
  "plannerMode": "spring-ai",
  "requestedDomain": "mdm",
  "routeTags": ["mdm", "material"]
}
```

关键预期返回字段：

```json
{
  "success": true,
  "data": {
    "runId": "run-demo-tool-chat-001",
    "plannerMode": "spring-ai",
    "selectedTool": "mdm.getMaterial",
    "execution": {
      "success": true,
      "toolName": "mdm.getMaterial",
      "data": {
        "displayTitle": "物料信息",
        "displaySummary": "已查询到物料 MAT-001（螺丝）",
        "rawData": {}
      }
    },
    "answer": "模型基于 Tool 结果总结的中文回答"
  }
}
```

### 5.6 RAG + Tool Agent Chat

```http
POST http://localhost:18080/api/v1/ai/agent/chat
Authorization: Bearer <accessToken>
Content-Type: application/json
X-Tenant-Id: 1
X-User-Id: 10001
X-Username: admin
X-User-Roles: ROLE_ADMIN
```

```json
{
  "runId": "run-demo-agent-001",
  "knowledgeBaseId": "kb-scm-demo",
  "topK": 3,
  "scoreThreshold": 0.1,
  "message": "按库存可用数量口径解释，并查物料 MAT-001 在仓库ID 2001、库位ID 3001 的库存",
  "plannerMode": "spring-ai",
  "requestedDomain": "mdm",
  "routeTags": ["mdm", "material"]
}
```

关键预期返回字段：

```json
{
  "success": true,
  "data": {
    "runId": "run-demo-agent-001",
    "intentType": "RAG_TOOL",
    "answer": "模型结合知识库规则和实时库存数据生成的中文回答",
    "rag": {
      "knowledgeBaseId": "kb-scm-demo",
      "retrievedCount": 1
    },
    "tool": {
      "selectedTool": "mdm.getMaterial"
    },
    "orchestration": {
      "enabled": true,
      "stepCount": 2,
      "steps": [
        { "toolName": "mdm.getMaterial", "status": "SUCCESS" },
        { "toolName": "inventory.getBalance", "status": "SUCCESS" }
      ]
    }
  }
}
```

### 5.7 Runtime Status

```http
GET http://localhost:18080/api/v1/ai/tools/runtime/status
Authorization: Bearer <accessToken>
X-Tenant-Id: 1
X-User-Id: 10001
X-Username: admin
X-User-Roles: ROLE_ADMIN
```

关键预期返回字段：

```json
{
  "success": true,
  "data": {
    "statuses": [
      {
        "toolName": "mdm.getMaterial",
        "totalCalls": 1,
        "successCount": 1,
        "failureCount": 0,
        "circuitState": "CLOSED"
      }
    ]
  }
}
```

### 5.8 Orchestration Status

```http
GET http://localhost:18080/api/v1/ai/tool-calling/orchestrations/run-demo-agent-001
Authorization: Bearer <accessToken>
X-Tenant-Id: 1
X-User-Id: 10001
X-Username: admin
X-User-Roles: ROLE_ADMIN
```

关键预期返回字段：

```json
{
  "success": true,
  "data": {
    "runId": "run-demo-agent-001",
    "plan": {
      "mode": "MULTI_STEP_CONTROLLED"
    },
    "steps": [
      {
        "stepRef": "step-1",
        "toolName": "mdm.getMaterial",
        "status": "SUCCESS",
        "outputSummary": "脱敏步骤摘要"
      }
    ]
  }
}
```

### 5.9 Workflow List

```http
GET http://localhost:18080/api/v1/ai/workflows
Authorization: Bearer <accessToken>
X-Tenant-Id: 1
X-User-Id: 10001
X-Username: admin
X-User-Roles: ROLE_ADMIN
```

关键预期返回字段：

```json
{
  "success": true,
  "data": {
    "workflows": [
      {
        "workflowCode": "scm_stock_replenishment_advice",
        "workflowName": "库存补货建议草案"
      }
    ]
  }
}
```

### 5.10 Workflow Run

```http
POST http://localhost:18080/api/v1/ai/workflows/scm_stock_replenishment_advice/run
Authorization: Bearer <accessToken>
Content-Type: application/json
X-Tenant-Id: 1
X-User-Id: 10001
X-Username: admin
X-User-Roles: ROLE_ADMIN
```

```json
{
  "runId": "run-demo-workflow-001",
  "message": "帮我生成物料 MAT-001 在仓库ID 2001、库位ID 3001 的补货建议草案，并说明库存可用数量口径",
  "knowledgeBaseId": "kb-scm-demo",
  "topK": 3,
  "scoreThreshold": 0.1,
  "parameters": {
    "warehouseId": 2001,
    "locationId": 3001
  }
}
```

关键预期返回字段：

```json
{
  "success": true,
  "data": {
    "runId": "run-demo-workflow-001",
    "workflowCode": "scm_stock_replenishment_advice",
    "status": "SUCCESS",
    "steps": [
      { "stepCode": "query_material", "status": "SUCCESS" },
      { "stepCode": "query_inventory_balance", "status": "SUCCESS" },
      {
        "stepCode": "generate_advice",
        "status": "SUCCESS",
        "safeFields": {
          "rag": {
            "knowledgeBaseId": "kb-scm-demo",
            "retrievedCount": 1
          }
        }
      }
    ],
    "finalAnswer": "模型基于知识库规则和实时 Tool 数据生成的补货建议草案"
  }
}
```

### 5.11 Workflow Status

```http
GET http://localhost:18080/api/v1/ai/workflows/runs/run-demo-workflow-001
Authorization: Bearer <accessToken>
X-Tenant-Id: 1
X-User-Id: 10001
X-Username: admin
X-User-Roles: ROLE_ADMIN
```

关键预期：返回 Workflow run、steps、safeFields 和 finalAnswer，不返回完整 rawData、完整 prompt、完整模型响应、token 或敏感 header。

### 5.12 MCP-style Tool List

```http
GET http://localhost:18080/api/v1/ai/mcp/tools
Authorization: Bearer <accessToken>
X-Tenant-Id: 1
X-User-Id: 10001
X-Username: admin
X-User-Roles: ROLE_ADMIN
```

关键预期返回字段：

```json
{
  "success": true,
  "data": {
    "toolCount": 2,
    "tools": [
      {
        "name": "mdm.getMaterial",
        "readOnly": true,
        "inputSchema": {},
        "displaySchema": {},
        "requiredPermissions": ["ai.tool.read", "ai.tool.mdm.read"]
      }
    ]
  }
}
```

### 5.13 MCP-style Tool Invoke

```http
POST http://localhost:18080/api/v1/ai/mcp/tools/mdm.getMaterial/invoke
Authorization: Bearer <accessToken>
Content-Type: application/json
X-Tenant-Id: 1
X-User-Id: 10001
X-Username: admin
X-User-Roles: ROLE_ADMIN
```

```json
{
  "runId": "run-demo-mcp-001",
  "arguments": {
    "materialCode": "MAT-001"
  }
}
```

关键预期返回字段：

```json
{
  "success": true,
  "data": {
    "runId": "run-demo-mcp-001",
    "toolName": "mdm.getMaterial",
    "success": true,
    "display": {
      "displayTitle": "物料信息",
      "displaySummary": "已查询到物料 MAT-001（螺丝）",
      "displayFields": []
    },
    "latencyMs": 20
  }
}
```

## 6. 能力矩阵

| 能力 | 当前实现 | 关键模块 | 演示接口 | 面试讲解点 | 后续扩展 |
| --- | --- | --- | --- | --- | --- |
| Chat | 基础模型聊天 | `AgentChatService` | `/api/v1/ai/chat` | 模型路由、provider mock / real 隔离 | 多模型质量评估 |
| RAG | 文档导入、检索、问答 | `rag` 包 | `/api/v1/ai/rag/**` | 切片、Embedding、Milvus / in-memory | 混合检索、重排 |
| Tool Calling | 模型规划 + Tool 执行 + answer 总结 | `toolcalling` 包 | `/api/v1/ai/tool-calling/chat` | Tool schema、真实业务查询、结构化执行结果 | 更多 Tool 类型 |
| Agent Chat | RAG + Tool 组合问答 | `agent` 包 | `/api/v1/ai/agent/chat` | 知识规则 + 实时数据闭环 | 更强意图识别 |
| Orchestrator | run / plan / step / status，受控二步执行 | `toolcalling.orchestrator` | `/api/v1/ai/tool-calling/orchestrations/**` | stepRef、安全摘要、受控执行 | 多步受控编排 |
| Workflow | 固定业务流程 + Engine / Executor | `workflow` 包 | `/api/v1/ai/workflows/**` | 业务流程编排，不复制 WorkflowService2 | 配置化 definition |
| MCP-style Adapter | 只读 Tool list / invoke | `mcp` 包 | `/api/v1/ai/mcp/**` | 外部 Agent 标准化调用内部 Tool | 标准 MCP Server transport |
| Audit | in-memory / mysql Tool 审计 | `ToolInvocationAuditService` | `/api/v1/ai/tools/invocations` | 可追溯、租户和用户链路 | 审计查询增强 |
| Permission | Tool requiredPermissions / roles | `ToolPermissionService` | Tool / MCP / Workflow 间接验证 | 权限失败不执行真实 Tool | 接入真实 RBAC |
| Runtime Protection | timeout / retry / circuit breaker | `ToolRuntimeProtectionService` | `/api/v1/ai/tools/runtime/status` | 稳定性和熔断保护 | 指标上报 |
| Prompt Context | Provider / Assembler / Renderer | `agent.prompt` 包 | `/api/v1/ai/agent/chat` | Advisor 风格上下文治理 | Spring AI Advisor 包装 |
| Config Isolation | mock / real、in-memory / mysql / milvus | `AiAgentProperties` | Maven test / local profile | 测试稳定，不依赖外部服务 | 环境模板 |
| Testing | 155 个 scm-ai-agent 测试 | `src/test` | `mvn -pl scm-ai-agent -am test` | 外部依赖隔离、Controller / Service 覆盖 | 集成测试流水线 |

## 7. 可直接使用的面试讲解稿

“这个项目最开始是一个 SaaS WMS / SCM 后端训练项目，有物料、仓库、库存、采购和销售这些业务上下文。我在这个基础上做 AI Agent，不是单独写一个调用大模型的 demo，而是把 Agent 能力嵌入真实企业后端工程里。

第一层是基础模型调用和模型路由，支持 mock 和真实 Spring AI provider 隔离。这样单测不依赖外部模型，本地联调又可以切到真实模型。

第二层是 RAG。项目支持文档导入、切片、Embedding、Milvus 和 in-memory 两种向量存储。RAG 负责回答规则、口径、流程说明，比如库存可用数量怎么理解、锁定数量是什么意思。

第三层是 Tool Calling。模型先基于 Tool schema 规划工具，服务端再执行真实业务 Tool，比如查物料、查库存。Tool 执行不是裸调用，还接入了权限、租户用户上下文、审计、超时重试和熔断。执行结果会包装成 display schema，既方便模型总结，也方便前端展示。

第四层是 RAG + Tool 组合 Agent Chat。它能同时用知识库解释规则，用 Tool 查询实时数据。比如用户问‘按库存可用数量口径解释，并查物料 MAT-001 的库存’，系统会先检索知识库，再查物料和库存，最后用统一 Prompt Context 生成回答。

Prompt Context 是我专门做的一层 Advisor 风格上下文治理。RAG、Tool、Orchestrator 都不是直接拼 prompt，而是由 Provider 生成结构化 section，再统一排序、裁剪、脱敏和渲染。后续如果要接 Spring AI Advisor，可以把这些 Provider 包装成 Advisor。

Orchestrator 主要治理 Agent Tool 调用过程，记录 run、plan、step、stepRef 和安全摘要。当前支持受控二步只读 Tool 执行，例如先查物料，再用物料 id 查库存，但默认不会无限自动编排，避免风险。

Workflow 是另一条线，面向明确业务流程。比如补货建议草案，它固定先查物料、再查库存、最后生成建议。Phase 6.3 把原来写死的三步拆成 Engine、Executor 和 Execution Context，后续新增 Workflow 不需要复制 WorkflowService2。

最后是 MCP-style Adapter。它解决外部 Agent 或 IDE 如何发现和调用企业内部 Tool 的问题。当前用 HTTP 方式实现 tool list 和 invoke，但内部仍复用 ToolInvocationService，所以权限、审计、runtime protection 都不丢。后续升级标准 MCP Server 时，主要替换 transport 层，核心治理链路不用重写。

所以这个项目体现的重点是企业级 AI Agent 工程能力：RAG、Tool、Orchestrator、Workflow、MCP-style adapter 这些能力都有，但每一层都保留了权限、安全、审计、配置隔离和测试稳定性。”

## 8. 已足够面试展示的能力

当前已经足够展示：

- Java 后端项目中如何落地 AI Agent，而不是只写 Python demo。
- RAG 如何结合业务知识库和实时业务数据。
- Tool Calling 如何做 schema、模型规划、服务端执行和 answer 总结。
- Tool 权限、审计、runtime protection 如何进入主链路。
- Prompt Context / Advisor 风格上下文治理如何避免散落拼 prompt。
- Orchestrator 和 Workflow 的职责边界。
- MCP-style adapter 如何把内部 Tool 暴露给外部 Agent。
- Maven test 如何做到不依赖真实模型、真实业务服务、MySQL、Milvus、Embedding API 或外部网络。

## 9. 后续推进建议

Phase 8.2 建议做演示材料增强：

- 补充一份面试用架构图。
- 整理关键接口真实返回 JSON。
- 给出 3 到 5 个常见面试问答。
- 增加一份“讲解时长 5 分钟 / 15 分钟 / 30 分钟”的不同版本话术。

Phase 9.1 如果继续开发，建议进入标准 MCP Server 或 Multi-Agent 二选一：

- 标准 MCP Server：适合补齐“外部 Agent / IDE 标准协议接入”的亮点。
- Multi-Agent：适合展示多角色协作，但实现成本和解释成本更高。

从面试性价比看，建议先做 Phase 8.2 演示材料，再考虑标准 MCP Server。
## 10. Phase 9.1 标准 MCP Server 演示补充

Phase 8.1 的完整演示顺序中已经包含 Phase 7.1 HTTP MCP-style Adapter。进入 Phase 9.1 后，可以在 MCP-style Adapter 之后追加标准 MCP Server transport 演示，用来说明项目已经具备从 REST 风格暴露层演进到 MCP JSON-RPC transport 的能力。

### 10.1 MCP Server tools/list

```http
POST http://localhost:18080/api/v1/ai/mcp/server
Authorization: Bearer <accessToken>
Content-Type: application/json
X-Tenant-Id: 1
X-User-Id: 10001
X-Username: admin
X-User-Roles: ROLE_ADMIN
```

```json
{
  "jsonrpc": "2.0",
  "id": "demo-mcp-tools-list-1",
  "method": "tools/list",
  "params": {}
}
```

关键预期字段：

```json
{
  "jsonrpc": "2.0",
  "id": "demo-mcp-tools-list-1",
  "result": {
    "tools": [
      {
        "name": "mdm.getMaterial",
        "inputSchema": {},
        "annotations": {
          "readOnly": true,
          "domain": "mdm"
        }
      },
      {
        "name": "inventory.getBalance",
        "annotations": {
          "readOnly": true,
          "domain": "inventory"
        }
      }
    ]
  }
}
```

### 10.2 MCP Server tools/call

```http
POST http://localhost:18080/api/v1/ai/mcp/server
Authorization: Bearer <accessToken>
Content-Type: application/json
X-Tenant-Id: 1
X-User-Id: 10001
X-Username: admin
X-User-Roles: ROLE_ADMIN
```

```json
{
  "jsonrpc": "2.0",
  "id": "demo-mcp-call-material-1",
  "method": "tools/call",
  "params": {
    "name": "mdm.getMaterial",
    "runId": "run-demo-mcp-server-material-001",
    "arguments": {
      "materialCode": "MAT-001"
    }
  }
}
```

关键预期字段：

```json
{
  "jsonrpc": "2.0",
  "id": "demo-mcp-call-material-1",
  "result": {
    "content": [
      {
        "type": "text",
        "text": "已查询到物料 MAT-001（螺丝）"
      }
    ],
    "structuredContent": {
      "toolName": "mdm.getMaterial",
      "success": true,
      "displayTitle": "物料信息",
      "displaySummary": "已查询到物料 MAT-001（螺丝）"
    },
    "isError": false
  }
}
```

### 10.3 面试讲解补充

可以这样讲：

“Phase 7.1 做的是 HTTP MCP-style Adapter，便于用 REST 方式演示内部 Tool 的发现和调用。Phase 9.1 则进一步提供 JSON-RPC 风格的 MCP Server transport，支持 `tools/list` 和 `tools/call`。但无论是哪种入口，内部都不绕过 `ToolInvocationService`，所以权限、审计、runtime protection 和 display schema 仍然统一生效。这个设计的重点是协议层可替换，企业治理链路不重复建设。”
