# AI Agent Tools 能力说明

## 1. 文档目的

本文档说明当前项目 AI Agent Phase 4 到 Phase 4.4 的 Tools 能力建设情况，重点覆盖：

- Tool 抽象设计
- mock / http adapter 切换方式
- inventory / material / sales / purchase / warehouse ToolClient 设计
- Tool 调用审计设计
- Spring AI Tool Calling 适配层设计
- Tool Calling Chat 最小闭环设计
- 通过 gateway `18080` 的验证方式
- 后续如何衔接 Spring AI Tool Calling、MCP、Workflow 和 Orchestrator

## 2. 当前阶段结论

截至 Phase 4.4，`scm-ai-agent` 已具备一套可扩展的 Tools 基础底座：

- 已有统一 Tool 协议：`ToolDefinition`、`ToolRequest`、`ToolResponse`、`ToolExecutor`
- 已有统一 Tool 注册与调用入口：`ToolRegistry`、`ToolInvocationService`
- 已有统一 API：
  - `GET /api/v1/ai/tools`
  - `POST /api/v1/ai/tools/invoke`
  - `GET /api/v1/ai/tools/invocations`
  - `GET /api/v1/ai/tool-calling/schema`
  - `POST /api/v1/ai/tool-calling/execute`
  - `POST /api/v1/ai/tool-calling/chat`
- 已支持 `mock` / `http` 两种 adapter 模式切换
- 已支持 5 个只读 Tool：
  - `inventory.getBalance`
  - `mdm.getMaterial`
  - `sales.getOrder`
  - `purchase.getOrder`
  - `mdm.getWarehouse`
- 已引入轻量级 Tool 调用审计能力，默认使用 in-memory 存储
- 已引入 Spring AI Tool Calling 适配层，可把 ToolDefinition 转成模型可消费 schema
- 已实现最小 Tool Calling Chat 闭环，可完成“问题 -> 选 Tool -> 执行 -> 回答”

本阶段仍然只做只读 Tool，不实现写操作 Tool，不实现 MCP、Workflow、多 Agent 和长任务编排，也不做真实 LLM 自动多轮 Tool Calling 编排。

## 3. 当前边界

当前默认行为仍然是“本地可启动、测试可通过、无外部依赖也能跑”：

- 默认使用 `mock` adapter
- 单元测试不依赖真实业务服务
- 单元测试不依赖 Nacos
- 单元测试不依赖 MySQL、Milvus、Embedding API 或外部网络
- Tool 审计默认使用 in-memory，不要求数据库

这样做的原因是先把 Tool 协议、上下文透传、异常包装、审计链路打稳，再逐步替换为真实业务调用。

## 4. Tool 抽象设计

当前 `scm-ai-agent` 中的 Tool 基础抽象如下：

- `ToolDefinition`
  - 描述工具名称、领域、说明、只读标记、参数定义
- `ToolRequest`
  - 描述一次工具调用请求
  - 系统统一补齐 `tenantId`、`userId`、`runId`
- `ToolResponse`
  - 描述一次工具调用结果
  - 统一返回 `success`、`toolName`、`data`、`errorCode`、`errorMessage`、`latencyMs`
- `ToolExecutor`
  - 具体工具执行器接口
- `ToolRegistry`
  - 启动时收集所有 `ToolExecutor`
  - 按 `toolName` 索引工具
- `ToolInvocationService`
  - 统一处理工具查找、执行、异常包装、日志和审计

统一响应字段：

```text
success
toolName
runId
data
errorCode
errorMessage
latencyMs
```

## 5. 当前 Tool 清单

| toolName | 领域 | 说明 | 当前支持的 adapter |
| --- | --- | --- | --- |
| `inventory.getBalance` | inventory | 查询库存余额 | mock / http |
| `mdm.getMaterial` | mdm | 查询物料信息 | mock / http |
| `sales.getOrder` | sales | 查询销售订单 | mock / http |
| `purchase.getOrder` | purchase | 查询采购订单 | mock / http |
| `mdm.getWarehouse` | mdm | 查询仓库信息 | mock / http |

## 6. 请求上下文与身份透传

客户端不能在请求体里随意覆盖租户和用户身份。

Tools API 统一复用 gateway 鉴权后的透传上下文：

- `X-Tenant-Id`
- `X-User-Id`
- `X-User-Name`
- `X-User-Roles`
- `X-Agent-Run-Id` 或 `runId`

其中：

- `tenantId` 来自网关透传后的租户上下文
- `userId` / `username` / `roles` 来自网关透传后的用户头
- `runId` 由调用方显式传入，未传时由 `ToolInvocationService` 自动生成

## 7. Tool 调用流程

```text
Client
  -> Gateway 18080
  -> JWT 鉴权
  -> 网关透传租户和用户上下文
  -> scm-ai-agent /api/v1/ai/tools/**
  -> ToolInvocationService
  -> ToolRegistry 查找 ToolExecutor
  -> ToolExecutor 调用对应 ToolClient
  -> mock 或 http adapter
  -> ToolResponse
  -> ToolInvocationAuditService 记录审计
```

## 8. 为什么先做只读 Tool

真实企业场景里，一旦 Agent Tool 能直接改业务数据，风险会立刻上升，包括：

- 误操作库存或订单
- 越权修改业务数据
- 缺少审批和确认流程
- 缺少幂等和补偿机制

所以当前阶段先只做只读 Tool，目的是：

- 先把协议和链路跑稳
- 先把身份透传和审计打通
- 先把模型到 Tool 的调用契约固定下来
- 为后续 Spring AI Tool Calling / MCP / Workflow 做低风险准备

## 9. Adapter 模式设计

配置项：

```yaml
ai:
  agent:
    tools:
      adapter-mode: mock
```

支持的取值：

| adapter-mode | 说明 |
| --- | --- |
| `mock` | 默认模式，使用本地 mock 数据，不依赖真实业务服务 |
| `http` | 通过 HTTP 调用真实 SCM/WMS 服务 |

后续预留扩展方向：

- `feign`
- `webclient`
- `gateway`

## 10. ToolClient 设计

### 10.1 已有 Client 抽象

当前已引入以下业务服务客户端抽象：

- `InventoryToolClient`
- `MdmToolClient`
- `SalesToolClient`
- `PurchaseToolClient`
- `WarehouseToolClient`

每个抽象都至少有两套实现：

- `Mock*ToolClient`
- `Http*ToolClient`

### 10.2 设计原则

ToolExecutor 不直接写死 mock 数据，而是统一委托给 ToolClient。

这样做的好处是：

- API 协议层不需要关心底层调用方式
- mock 切 http 时不用改 Controller 和 ToolRegistry
- 后续切到 Feign / Gateway 时也只需要替换 Client 实现
- 更适合后续接 Tool Calling / MCP

## 11. HTTP Adapter 真实接口映射

当前是根据已有业务服务 Controller 做最小适配：

| Tool | 服务 | 真实接口 |
| --- | --- | --- |
| `inventory.getBalance` | `scm-inventory` | `GET /api/v1/inventory/balances?materialId=&warehouseId=&locationId=` |
| `mdm.getMaterial` | `scm-mdm` | `GET /api/v1/materials/{materialId}` |
| `sales.getOrder` | `scm-sales` | `GET /api/v1/sales-orders/{id}` 或 `GET /api/v1/sales-orders/by-order-no?orderNo=` |
| `purchase.getOrder` | `scm-purchase` | `GET /api/v1/purchase-orders/{id}` 或 `GET /api/v1/purchase-orders/by-order-no?orderNo=` |
| `mdm.getWarehouse` | `scm-mdm` | `GET /api/v1/warehouses/{id}` |

说明：

- `mdm.getWarehouse` 当前按 `warehouseId` 查询
- 如果后续主数据服务新增按仓库编码查询接口，再扩展 `warehouseCode` 分支

## 12. HTTP Adapter 配置

`application.yml` 默认配置：

```yaml
ai:
  agent:
    tools:
      adapter-mode: ${AI_AGENT_TOOLS_ADAPTER_MODE:mock}
      http:
        inventory-base-url: ${INVENTORY_SERVICE_BASE_URL:http://localhost:18084}
        mdm-base-url: ${MDM_SERVICE_BASE_URL:http://localhost:18082}
        sales-base-url: ${SALES_SERVICE_BASE_URL:http://localhost:18085}
        purchase-base-url: ${PURCHASE_SERVICE_BASE_URL:http://localhost:18083}
        connect-timeout-ms: ${AI_AGENT_TOOLS_HTTP_CONNECT_TIMEOUT_MS:3000}
        read-timeout-ms: ${AI_AGENT_TOOLS_HTTP_READ_TIMEOUT_MS:5000}
```

### 12.1 IDEA / application-local.yml 配置示例

如果你本地要联调真实服务，可以在 [application-local.yml](E:/ideaProject/saas-wms-scm/scm-ai-agent/src/main/resources/application-local.yml) 中配置：

```yaml
ai:
  agent:
    tools:
      adapter-mode: http
      http:
        inventory-base-url: http://localhost:18084
        mdm-base-url: http://localhost:18082
        sales-base-url: http://localhost:18085
        purchase-base-url: http://localhost:18083
```

也可以用环境变量：

```text
AI_AGENT_TOOLS_ADAPTER_MODE=http
INVENTORY_SERVICE_BASE_URL=http://localhost:18084
MDM_SERVICE_BASE_URL=http://localhost:18082
SALES_SERVICE_BASE_URL=http://localhost:18085
PURCHASE_SERVICE_BASE_URL=http://localhost:18083
```

## 13. Tool 调用审计设计

### 13.1 审计目标

Phase 4.2 新增 Tool 调用审计，是为了给后续能力提供观测基础：

- Tool Calling 排障
- Workflow 节点追踪
- MCP 调用记录
- Agent run 回放
- 运维审计与问题定位

### 13.2 当前审计字段

当前最小审计记录 `ToolInvocationAuditRecord` 包含：

- `tenantId`
- `userId`
- `runId`
- `toolName`
- `adapterMode`
- `success`
- `errorCode`
- `latencyMs`
- `createdAt`

### 13.3 当前审计实现

当前默认实现为：

- `ToolInvocationAuditStore`
- `InMemoryToolInvocationAuditStore`
- `ToolInvocationAuditService`

配置项：

```yaml
ai:
  agent:
    tools:
      audit:
        mode: in-memory
        max-records: 500
```

当前边界：

- 服务重启后审计记录会丢失
- 不保存大段业务响应
- 不保存敏感头、密码、prompt、模型响应

### 13.4 为什么当前先用 in-memory

当前先用 in-memory 的原因：

- 保持本地启动简单
- 不给 Phase 4.2 引入额外数据库依赖
- 先验证审计字段和查询接口是否合理
- 后续切 MySQL 时可以复用同一套服务接口

### 13.5 后续如何升级为 MySQL 审计

后续如果要持久化 Tool 审计，可以新增：

- `MysqlToolInvocationAuditStore`
- `tool_invocation_audit` 表
- 条件装配 `ai.agent.tools.audit.mode=mysql`

这样就可以在不改 Controller 和 Service 的前提下切换存储实现。

## 14. Spring AI Tool Calling 适配层设计

### 14.1 当前阶段目标

Phase 4.3 的目标不是直接让真实大模型自动完成多轮 Tool Calling，而是先把下面两层打通：

- 把当前项目里的 `ToolDefinition` 转成模型可识别的 Tool schema
- 提供统一的服务端执行入口，模拟“模型返回 toolName + arguments 后”的实际执行链路

这样后续接入真实 Spring AI `ChatClient` 或模型 function calling 时，不需要重写工具执行主链路。

### 14.2 当前新增组件

当前新增了以下适配层组件：

- `ToolSchemaConverter`
  - 把 `ToolDefinition` 转成 `SpringAiToolDescriptor`
- `SpringAiToolDescriptor`
  - 表示模型侧可见的工具描述
- `SpringAiToolInputSchema`
  - 表示工具输入 schema
- `SpringAiToolParameterSchema`
  - 表示单个参数 schema
- `SpringAiToolCallingService`
  - 提供 schema 查询和服务端执行入口
- `AiToolCallingController`
  - 暴露 `/api/v1/ai/tool-calling/**` 调试接口

### 14.3 Tool schema 结构

当前每个工具会被转换为一份简化的 schema，至少包含：

- `toolName`
- `description`
- `readOnly`
- `inputSchema.type`
- `inputSchema.properties`
- `inputSchema.required`
- `inputSchema.oneOfRequiredGroups`

这里的 `oneOfRequiredGroups` 用于表达“多个参数至少传一个”的场景，例如：

```text
sales.getOrder: orderId / orderNo 至少传一个
purchase.getOrder: orderId / orderNo 至少传一个
mdm.getMaterial: materialId / materialCode 至少传一个
mdm.getWarehouse: warehouseId / warehouseCode 至少传一个
```

### 14.4 和 ToolInvocationService 的关系

Phase 4.3 没有新起一套平行执行体系，而是继续复用现有的 `ToolInvocationService`：

- schema 查询走 `ToolRegistry + ToolSchemaConverter`
- execute 调试接口做参数校验
- 校验通过后仍然调用 `ToolInvocationService`
- Tool 审计继续复用已有 `ToolInvocationAuditService`

这样后续接真实模型时，模型侧只负责“选哪个 tool、带什么 arguments”，服务端仍然走统一执行链路。

## 15. Tool Calling Chat 最小闭环

### 15.1 当前阶段目标

Phase 4.4 的目标是在 Phase 4.3 的 Tool schema 和执行入口之上，再补一层最小聊天闭环：

- 用户输入问题
- planner 选择合适 Tool
- 服务端执行 Tool
- 拼装最终 answer

当前阶段先确保链路通，不追求复杂自然语言参数抽取，也不追求多轮、多 Tool 自动编排。

### 15.2 plannerMode 设计

当前支持两种 plannerMode：

| plannerMode | 说明 |
| --- | --- |
| `mock` | 默认模式，不依赖真实模型，按规则把问题映射到 Tool |
| `spring-ai` | 预留真实模型规划入口；当前阶段先回退到 mock 规则规划，不强依赖外部模型 |

配置项：

```yaml
ai:
  agent:
    tool-calling:
      planner-mode: mock
```

也可以通过环境变量配置：

```text
AI_AGENT_TOOL_CALLING_PLANNER_MODE=mock
```

### 15.3 mock-planner 路由规则

当前 mock-planner 至少支持以下规则：

- 包含 `库存 / balance / available`
  - 路由到 `inventory.getBalance`
- 包含 `物料 / material`
  - 路由到 `mdm.getMaterial`
- 包含 `销售订单 / sales order`
  - 路由到 `sales.getOrder`
- 包含 `采购订单 / purchase order`
  - 路由到 `purchase.getOrder`
- 包含 `仓库 / warehouse`
  - 路由到 `mdm.getWarehouse`

如果请求体显式传了 `requestedTool`，则优先级高于规则路由。

### 15.4 参数来源策略

当前阶段参数来源策略如下：

- 优先使用请求体中的 `toolArguments`
- 如果调用方没有传 `toolArguments`，则 mock-planner 会补一组最小默认参数
- 参数校验仍然在 `SpringAiToolCallingService.execute` 中统一完成

这样既能满足本地快速验证，也能保证进入真正 Tool 执行前仍然有统一校验。

### 15.5 和现有服务的关系

当前主链路分层如下：

- `MockToolPlanner`
  - 决定选择哪个 Tool
  - 生成最小可执行参数
- `ToolCallingChatService`
  - 组织 planner、执行和 answer 拼装
- `SpringAiToolCallingService`
  - 负责参数校验和服务端统一 Tool 执行入口
- `ToolInvocationService`
  - 复用现有 Tool 执行主链路
- `ToolInvocationAuditService`
  - 继续记录 Tool 调用审计

也就是说，Phase 4.4 没有绕开现有 Tools 主链路，而是在其上叠加一层 Chat orchestration。

## 14. Gateway 18080 验证方式

### 14.1 登录获取 Token

```http
POST http://localhost:18080/api/v1/auth/login
Content-Type: application/json
X-Tenant-Id: 1
```

```json
{
  "username": "admin",
  "password": "admin123"
}
```

从返回结果中取：

```text
data.accessToken
```

### 14.2 查询 Tool 列表

```http
GET http://localhost:18080/api/v1/ai/tools
Authorization: Bearer <accessToken>
```

关键预期字段：

```json
{
  "success": true,
  "code": "200",
  "data": {
    "tenantId": 1,
    "toolCount": 5,
    "tools": [
      {
        "name": "sales.getOrder",
        "domain": "sales",
        "readOnly": true
      }
    ]
  }
}
```

### 14.3 调用销售订单 Tool

```http
POST http://localhost:18080/api/v1/ai/tools/invoke
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "toolName": "sales.getOrder",
  "runId": "run-tools-sales-001",
  "parameters": {
    "orderId": 1
  }
}
```

mock 模式关键预期字段：

```json
{
  "success": true,
  "code": "200",
  "data": {
    "success": true,
    "toolName": "sales.getOrder",
    "runId": "run-tools-sales-001",
    "data": {
      "adapterMode": "mock"
    }
  }
}
```

http 模式关键预期字段：

```json
{
  "success": true,
  "data": {
    "success": true,
    "toolName": "sales.getOrder",
    "data": {
      "adapterMode": "http"
    }
  }
}
```

### 14.4 调用采购订单 Tool

```http
POST http://localhost:18080/api/v1/ai/tools/invoke
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "toolName": "purchase.getOrder",
  "runId": "run-tools-purchase-001",
  "parameters": {
    "orderNo": "PO-20260520-001"
  }
}
```

关键预期字段：

```json
{
  "success": true,
  "data": {
    "success": true,
    "toolName": "purchase.getOrder",
    "data": {
      "adapterMode": "mock"
    }
  }
}
```

### 14.5 调用仓库信息 Tool

```http
POST http://localhost:18080/api/v1/ai/tools/invoke
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "toolName": "mdm.getWarehouse",
  "runId": "run-tools-warehouse-001",
  "parameters": {
    "warehouseId": 1
  }
}
```

关键预期字段：

```json
{
  "success": true,
  "data": {
    "success": true,
    "toolName": "mdm.getWarehouse",
    "data": {
      "adapterMode": "mock"
    }
  }
}
```

### 14.6 查询 Tool 调用审计

```http
GET http://localhost:18080/api/v1/ai/tools/invocations?toolName=sales.getOrder&runId=run-tools-sales-001&limit=10
Authorization: Bearer <accessToken>
```

关键预期字段：

```json
{
  "success": true,
  "code": "200",
  "data": {
    "tenantId": 1,
    "count": 1,
    "records": [
      {
        "toolName": "sales.getOrder",
        "runId": "run-tools-sales-001",
        "adapterMode": "mock",
        "success": true,
        "latencyMs": 12
      }
    ]
  }
}
```

### 14.7 工具不存在时的行为

```http
POST http://localhost:18080/api/v1/ai/tools/invoke
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "toolName": "unknown.tool",
  "runId": "run-tools-404",
  "parameters": {}
}
```

关键预期字段：

```json
{
  "success": true,
  "data": {
    "success": false,
    "toolName": "unknown.tool",
    "errorCode": "404",
    "errorMessage": "Tool not found: unknown.tool"
  }
}
```

同时这次失败调用也会写入审计记录。

### 14.8 查询 Tool Calling schema

```http
GET http://localhost:18080/api/v1/ai/tool-calling/schema
Authorization: Bearer <accessToken>
```

关键预期字段：

```json
{
  "success": true,
  "code": "200",
  "data": {
    "tenantId": 1,
    "toolCount": 5,
    "tools": [
      {
        "toolName": "sales.getOrder",
        "description": "查询销售订单概要和明细",
        "readOnly": true,
        "inputSchema": {
          "type": "object",
          "properties": {
            "orderId": {
              "type": "integer",
              "description": "销售订单 ID",
              "required": false
            }
          },
          "oneOfRequiredGroups": [
            ["orderId", "orderNo"]
          ]
        }
      }
    ]
  }
}
```

### 14.9 执行 Tool Calling 调试接口

```http
POST http://localhost:18080/api/v1/ai/tool-calling/execute
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "toolName": "sales.getOrder",
  "runId": "run-tool-calling-001",
  "arguments": {
    "orderNo": "SO-001"
  }
}
```

关键预期字段：

```json
{
  "success": true,
  "data": {
    "success": true,
    "toolName": "sales.getOrder",
    "arguments": {
      "orderNo": "SO-001"
    },
    "toolResponse": {
      "success": true,
      "toolName": "sales.getOrder"
    }
  }
}
```

### 14.10 参数缺失时的行为

```http
POST http://localhost:18080/api/v1/ai/tool-calling/execute
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "toolName": "inventory.getBalance",
  "runId": "run-tool-calling-bad-001",
  "arguments": {
    "warehouseId": 1
  }
}
```

### 14.11 Tool Calling Chat 调试接口

```http
POST http://localhost:18080/api/v1/ai/tool-calling/chat
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "message": "帮我查一下销售订单",
  "runId": "run-tool-chat-001",
  "plannerMode": "mock",
  "toolArguments": {
    "orderNo": "SO-001"
  }
}
```

关键预期字段：

```json
{
  "success": true,
  "data": {
    "runId": "run-tool-chat-001",
    "plannerMode": "mock",
    "selectedTool": "sales.getOrder",
    "toolArguments": {
      "orderNo": "SO-001"
    },
    "toolResponse": {
      "success": true,
      "toolName": "sales.getOrder"
    },
    "answer": "已根据你的问题调用工具 `sales.getOrder` 完成查询。"
  }
}
```

### 14.12 requestedTool 优先级验证

```http
POST http://localhost:18080/api/v1/ai/tool-calling/chat
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "message": "帮我查库存",
  "runId": "run-tool-chat-002",
  "plannerMode": "mock",
  "requestedTool": "mdm.getMaterial",
  "toolArguments": {
    "materialCode": "MAT-001"
  }
}
```

关键预期字段：

```json
{
  "success": true,
  "data": {
    "selectedTool": "mdm.getMaterial",
    "toolArguments": {
      "materialCode": "MAT-001"
    }
  }
}
```

关键预期字段：

```json
{
  "success": true,
  "data": {
    "success": false,
    "toolName": "inventory.getBalance",
    "toolResponse": {
      "success": false,
      "errorCode": "400",
      "errorMessage": "Missing required parameter: materialId"
    }
  }
}
```

## 15. 测试方式

执行：

```bash
mvn -pl scm-ai-agent -am test
```

当前测试重点覆盖：

- ToolRegistry 注册和查询
- sales / purchase / warehouse Tool 注册成功
- mock ToolClient 可正常执行
- http adapter 配置可绑定
- ToolInvocationService 会记录成功和失败审计
- `/api/v1/ai/tools/invocations` 查询接口可用
- 工具不存在时也会记录失败审计
- ToolDefinition 能转换为 Tool schema
- `/api/v1/ai/tool-calling/schema` 查询接口可用
- `/api/v1/ai/tool-calling/execute` 能走服务端执行入口
- 参数缺失时会返回明确错误并记录审计
- mock-planner 可按问题路由到正确 Tool
- `requestedTool` 优先级高于规则路由
- `/api/v1/ai/tool-calling/chat` 可返回结构化聊天结果

## 16. 当前阶段不做的事情

本阶段不实现：

- 写操作 Tool
- 真实 LLM 自动多轮 Tool Calling 编排
- 自动多 Tool 编排
- MCP Server
- Workflow
- Multi-Agent
- 长任务编排
- Tool 审计 MySQL 持久化

## 17. 后续建议

Phase 4.5 可以继续往下面推进：

- 把 `spring-ai-planner` 接到真实 `RoutingChatModelClient`
- 支持模型根据 Tool schema 自动输出 toolName 和 arguments
- 为 Tool 审计增加 MySQL 持久化
- 给 Tool 增加权限标签和路由标签
- 增加 Tool 超时、重试和熔断策略
