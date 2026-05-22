# AI Agent Tools 能力说明

## 1. 文档目的

本文档说明当前项目 AI Agent Phase 4 到 Phase 4.5 的 Tools 能力建设情况，重点覆盖：

- Tool 抽象设计
- mock / http adapter 切换方式
- inventory / material / sales / purchase / warehouse ToolClient 设计
- Tool 调用审计设计
- Spring AI Tool Calling 适配层设计
- Tool Calling Chat 最小闭环设计
- 真实 Spring AI Planner 设计
- 通过 gateway `18080` 的验证方式
- 后续如何衔接 Spring AI Tool Calling、MCP、Workflow 和 Orchestrator

## 2. 当前阶段结论

截至 Phase 4.5，`scm-ai-agent` 已具备一套可扩展的 Tools 基础底座：

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
- 已实现真实 Spring AI Planner，可由模型基于 Tool schema 输出结构化 Tool Plan

本阶段仍然只做只读 Tool，不实现写操作 Tool，不实现 MCP、Workflow、多 Agent 和长任务编排，也不做真实 LLM 自动多轮 Tool Calling 编排。

## 3. 当前边界

当前默认行为区分“本地联调”和“测试验证”两套路径：

- 本地 `application-local.yml` 默认使用真实 Spring AI Planner
- 本地 Tools adapter 默认使用 `http`
- 单元测试仍不依赖真实业务服务
- 单元测试不依赖 Nacos
- 单元测试不依赖 MySQL、Milvus、Embedding API 或外部网络
- Tool 审计默认使用 in-memory，不要求数据库

这样做的原因是：

- 运行时优先验证真实链路
- 测试时保持稳定、低成本和可重复
- 把真实模型波动对 CI 的影响隔离在测试之外

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
| `mdm.getMaterial` | `scm-mdm` | `GET /api/v1/materials/{materialId}` 或 `GET /api/v1/materials/by-code?materialCode=` |
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
    "planningSource": "mock",
    "selectedTool": "sales.getOrder",
    "toolArguments": {
      "orderNo": "SO-001"
    },
    "execution": {
      "success": true,
      "toolName": "sales.getOrder"
    },
    "answer": "已查询到销售订单 SO-001，状态 ALLOCATED。"
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
    },
    "execution": {
      "success": true,
      "toolName": "mdm.getMaterial"
    }
  }
}
```

关键预期字段：

```json
{
  "success": true,
  "data": {
    "selectedTool": "inventory.getBalance",
    "execution": {
      "success": false,
      "toolName": "inventory.getBalance",
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
- Spring AI Planner 的 JSON 输出可被解析成 Tool Plan
- Spring AI Planner 失败后的 fallback 策略可被验证

## 16. Phase 4.5：真实 Spring AI Planner

### 16.1 阶段目标

Phase 4.5 的重点是把之前的 `spring-ai` 占位分支升级成真实模型规划链路：

- 模型读取当前 Tool schema
- 模型只输出 JSON Tool Plan
- 服务端解析 JSON
- 服务端继续复用现有 Tool Calling 执行链路

### 16.2 plannerMode 设计

当前 `tool-calling/chat` 支持两种 plannerMode：

| plannerMode | 用途 |
| --- | --- |
| `spring-ai` | 本地联调默认模式，使用真实模型规划 Tool |
| `mock` | 测试和兜底模式，按规则规划 Tool |

同时新增两个返回字段：

- `planningSource`
  - `requested`
  - `spring-ai`
  - `mock`
  - `mock-fallback`
- `fallbackUsed`

### 16.3 requestedTool 优先级

如果请求显式传入 `requestedTool`，服务端直接执行该工具，不走模型规划。

### 16.4 Spring AI Planner 设计

新增以下组件：

- `ToolPlanningPromptBuilder`
- `ToolPlanParser`
- `SpringAiToolPlanner`

规划链路：

```text
tool-calling/chat
  -> requestedTool 优先判断
  -> SpringAiToolPlanner
  -> ToolSchemaConverter 生成 schema
  -> ToolPlanningPromptBuilder 组装提示词
  -> RoutingChatModelClient 调真实模型
  -> ToolPlanParser 解析 JSON
  -> SpringAiToolCallingService.execute
  -> ToolInvocationService
```

### 16.5 JSON 输出协议

当前要求模型只返回一个 JSON 对象：

```json
{
  "toolName": "mdm.getMaterial",
  "arguments": {
    "materialCode": "MAT-001"
  },
  "reason": "用户在查询物料信息"
}
```

### 16.6 fallback 策略

新增配置：

```yaml
ai:
  agent:
    tool-calling:
      planner-mode: spring-ai
      answer-mode: spring-ai
      spring-ai-planner:
        enabled: true
        fallback-to-mock: false
        max-retries: 1
        task-type: tool_calling
      spring-ai-answer:
        enabled: true
        fallback-to-template: true
        max-retries: 1
        task-type: tool_calling_answer
```

说明：

- 本地 `application-local.yml` 默认 `fallback-to-mock=false`
- 这样可以更早暴露真实模型规划问题
- 测试时通过 mock/stub 单独验证 fallback 分支

### 16.7 本地运行配置

当前 [application-local.yml](E:/ideaProject/saas-wms-scm/scm-ai-agent/src/main/resources/application-local.yml) 已默认设置：

- `ai.agent.tool-calling.planner-mode=spring-ai`
- `ai.agent.tool-calling.answer-mode=spring-ai`
- `ai.agent.tool-calling.spring-ai-planner.enabled=true`
- `ai.agent.tool-calling.spring-ai-planner.fallback-to-mock=false`
- `ai.agent.tool-calling.spring-ai-answer.enabled=true`
- `ai.agent.tool-calling.spring-ai-answer.fallback-to-template=true`
- `ai.agent.tools.adapter-mode=http`

### 16.8 gateway 18080 验证示例

```http
POST http://localhost:18080/api/v1/ai/tool-calling/chat
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "message": "帮我查物料 MAT-001",
  "runId": "run-tool-chat-phase45-001",
  "plannerMode": "spring-ai"
}
```

关键预期字段：

```json
{
  "success": true,
  "data": {
    "runId": "run-tool-chat-phase45-001",
    "plannerMode": "spring-ai",
    "planningSource": "spring-ai",
    "fallbackUsed": false,
    "selectedTool": "mdm.getMaterial"
  }
}
```

## 17. Phase 4.6：Chat 返回结构与答案表达优化

### 17.1 阶段目标

Phase 4.6 的重点是把当前 Tool Calling Chat 从“能跑通”提升到“更适合真实联调和展示”：

- 压平 chat 接口返回结构
- 明确区分规划结果、执行结果、最终回答
- 让 answer 更充分利用 Tool data

### 17.2 chat 返回结构

当前 `/api/v1/ai/tool-calling/chat` 关键返回字段：

- `runId`
- `plannerMode`
- `planningSource`
- `fallbackUsed`
- `selectedTool`
- `toolArguments`
- `planningReason`
- `execution`
- `answer`
- `latencyMs`

其中 `execution` 结构为：

- `success`
- `toolName`
- `errorCode`
- `errorMessage`
- `data`
- `latencyMs`

### 17.3 answer 生成策略

当前先使用服务端模板增强，不强制引入第二次 LLM 总结：

- `mdm.getMaterial`
  - 优先输出物料名称、物料编码、状态、分类、单位
- `inventory.getBalance`
  - 优先输出物料、仓库、库位、可用量、锁定量、单位
- `sales.getOrder`
  - 优先输出订单号、状态、客户、明细行数
- `purchase.getOrder`
  - 优先输出订单号、状态、供应商、明细行数
- `mdm.getWarehouse`
  - 优先输出仓库名称、仓库编码、类型、状态

如果 Tool 执行失败，则保留明确失败原因，不吞掉下游错误。

## 18. Phase 4.7：Tool 执行后再由模型总结答案

### 18.1 阶段目标

Phase 4.7 的重点是在 Phase 4.6 已有 execution 结构稳定的前提下，引入“第二阶段模型总结答案”：

- 第一阶段：模型规划 Tool
- 第二阶段：服务端执行 Tool
- 第三阶段：模型基于 Tool 执行结果总结最终 answer

这样可以让 `/api/v1/ai/tool-calling/chat` 的最终回答从“模板增强”升级成“真实模型总结 + 结构化执行结果”的闭环。

### 18.2 answer-mode 设计

当前支持两种 answer-mode：

| answer-mode | 说明 |
| --- | --- |
| `template` | 直接复用服务端模板回答，不依赖第二次模型总结 |
| `spring-ai` | Tool 执行后再次调用真实模型，根据 execution 结果总结最终中文答案 |

当前本地 [application-local.yml](E:/ideaProject/saas-wms-scm/scm-ai-agent/src/main/resources/application-local.yml) 默认使用：

- `ai.agent.tool-calling.answer-mode=spring-ai`
- `ai.agent.tool-calling.spring-ai-answer.enabled=true`
- `ai.agent.tool-calling.spring-ai-answer.fallback-to-template=true`
- `ai.agent.tool-calling.spring-ai-answer.task-type=tool_calling_answer`

### 18.3 二阶段回答生成设计

当前主链路关系如下：

- `SpringAiToolPlanner`
  - 负责第一阶段工具规划
- `SpringAiToolCallingService`
  - 负责第二阶段工具执行
- `ToolCallingAnswerSummaryService`
  - 负责第三阶段最终答案生成
- `ToolCallingAnswerPromptBuilder`
  - 负责构造“工具执行结果总结”提示词
- `ToolCallingAnswerBuilder`
  - 作为模板回答兜底

第二阶段模型总结时，输入上下文至少包含：

- 用户原始问题
- `selectedTool`
- `toolArguments`
- `execution.success`
- `execution.data`
- `execution.errorCode`
- `execution.errorMessage`

### 18.4 fallback 策略

当前运行时策略：

- 如果 `answer-mode=template`
  - 直接走模板回答
- 如果 `answer-mode=spring-ai`
  - 优先尝试真实模型总结
  - 如果模型总结失败，且 `fallback-to-template=true`
    - 回退到模板回答
  - 如果模型总结失败，且 `fallback-to-template=false`
    - 直接抛出错误，暴露真实问题

### 18.5 execution 结构保持稳定

Phase 4.7 不改变 `execution` 结构，仍然保持：

- `success`
- `toolName`
- `errorCode`
- `errorMessage`
- `data`
- `latencyMs`

也就是说，前端或联调脚本即使切换了 `answer-mode`，也不需要改 `execution` 解析逻辑。

### 18.6 gateway 18080 验证示例

```http
POST http://localhost:18080/api/v1/ai/tool-calling/chat
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "message": "帮我查物料 MAT-001",
  "runId": "run-tool-chat-phase47-001",
  "plannerMode": "spring-ai"
}
```

关键预期字段：

```json
{
  "success": true,
  "data": {
    "runId": "run-tool-chat-phase47-001",
    "plannerMode": "spring-ai",
    "planningSource": "spring-ai",
    "selectedTool": "mdm.getMaterial",
    "execution": {
      "success": true,
      "toolName": "mdm.getMaterial"
    },
    "answer": "这里会是模型基于物料查询结果生成的中文回答"
  }
}
```

失败场景示例：

```http
POST http://localhost:18080/api/v1/ai/tool-calling/chat
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "message": "帮我查物料 MAT-404",
  "runId": "run-tool-chat-phase47-002",
  "plannerMode": "spring-ai",
  "requestedTool": "mdm.getMaterial",
  "toolArguments": {
    "materialCode": "MAT-404"
  }
}
```

关键预期字段：

```json
{
  "success": true,
  "data": {
    "selectedTool": "mdm.getMaterial",
    "execution": {
      "success": false,
      "toolName": "mdm.getMaterial",
      "errorCode": "404",
      "errorMessage": "MDM service failed: Material not found"
    },
    "answer": "这里会是模型或模板基于失败原因生成的中文说明"
  }
}
```

### 18.7 当前边界

本阶段仍然不实现：

- 多轮 Tool Calling
- 多 Tool 自动编排
- MCP Server
- Workflow
- Multi-Agent
- 长任务编排

## 19. Phase 4.8：Tool 执行结果统一展示 schema

### 19.1 阶段目标

Phase 4.8 的重点是在 Phase 4.7 已有“模型总结 answer”闭环基础上，把不同 Tool 的执行结果收敛成统一展示结构：

- 继续保留 `execution` 顶层结构稳定
- 在成功场景下把 `execution.data` 包装为展示 schema
- 展示字段优先服务于模型总结、前端展示和后续 Orchestrator 上下文沉淀
- 原始 Tool 返回数据通过 `rawData` 保留，不丢失追溯能力

### 19.2 display schema 结构

成功场景下，`execution.data` 统一包含：

| 字段 | 说明 |
| --- | --- |
| `displayTitle` | 展示标题，例如“物料信息”“库存余额” |
| `displaySummary` | 展示摘要，用于模型总结和前端快速展示 |
| `displayFields` | 展示字段列表，每项包含 `key`、`label`、`value` |
| `displayItems` | 展示明细列表，用于订单行、库存明细等列表数据 |
| `rawData` | Tool 原始返回数据 |

当前已为以下 Tool 提供专用展示适配：

- `mdm.getMaterial`
- `mdm.getWarehouse`
- `inventory.getBalance`
- `sales.getOrder`
- `purchase.getOrder`

如果 Tool 暂无专用适配器，则使用通用 fallback schema：`displayTitle` 为工具名，`displaySummary` 为“已完成工具查询”，并从原始 Map 的简单标量字段生成 `displayFields`。

### 19.3 display schema 与 rawData 的关系

`displayTitle`、`displaySummary`、`displayFields`、`displayItems` 是稳定展示层，不要求完整覆盖业务返回。

`rawData` 是原始业务数据，用于：

- 排查展示字段遗漏
- 后续扩展更细粒度 prompt
- Orchestrator 保存完整执行上下文
- 前端在需要时查看原始字段

本阶段不删除原始 Tool 返回数据，不把模型总结限制在裁剪后的字段内。

### 19.4 answer prompt 使用方式

`ToolCallingAnswerPromptBuilder` 会优先把 display schema 放入总结上下文，使模型更稳定地读取标题、摘要、字段和明细。

同时，prompt 仍保留：

- `selectedTool`
- `toolArguments`
- `execution.success`
- `execution.errorCode`
- `execution.errorMessage`
- `execution.latencyMs`
- `rawData`

这样既能提升回答质量，又不破坏 Phase 4.7 的失败说明和模板 fallback 语义。

### 19.5 gateway 18080 验证示例

```http
POST http://localhost:18080/api/v1/ai/tool-calling/chat
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "message": "帮我查物料 MAT-001",
  "runId": "run-tool-chat-phase48-001",
  "plannerMode": "spring-ai"
}
```

关键预期字段：

```json
{
  "success": true,
  "data": {
    "runId": "run-tool-chat-phase48-001",
    "plannerMode": "spring-ai",
    "planningSource": "spring-ai",
    "selectedTool": "mdm.getMaterial",
    "execution": {
      "success": true,
      "toolName": "mdm.getMaterial",
      "data": {
        "displayTitle": "物料信息",
        "displaySummary": "已查询到物料 MAT-001",
        "displayFields": [
          {
            "key": "materialCode",
            "label": "物料编码",
            "value": "MAT-001"
          }
        ],
        "displayItems": [],
        "rawData": {
          "materialCode": "MAT-001"
        }
      }
    },
    "answer": "模型基于展示 schema 和原始数据生成的中文回答"
  }
}
```

### 19.6 当前边界

本阶段仍然不实现：

- 多轮 Tool Calling
- 多 Tool 自动编排
- MCP Server
- Workflow
- Multi-Agent
- 长任务编排
- 严格 JSON answer

## 20. Phase 4.9：按 Tool 类型优化 answer prompt 与 Tool 审计 MySQL 持久化

### 20.1 阶段目标

Phase 4.9 在 Phase 4.8 统一展示 schema 基础上继续增强两件事：

- 按 Tool 类型细化模型总结提示词，让 answer 更贴近业务对象
- 为 Tool 调用审计增加 MySQL 存储模式，保留默认 in-memory 测试路径

本阶段不改变 `/api/v1/ai/tool-calling/chat` 顶层返回字段，不改变 `execution` 顶层字段，也不删除 `execution.data.rawData`。

### 20.2 answer prompt strategy 设计

当前新增 `ToolCallingAnswerPromptStrategy` 和 `ToolCallingAnswerPromptStrategyRegistry`：

- `ToolCallingAnswerPromptBuilder` 仍负责拼装最终 prompt
- strategy 只提供“Tool 类型专项要求”
- strategy 不参与 Tool 执行、不参与 Planner、不改变 answer-mode
- 未匹配 Tool 时使用 fallback 策略

当前策略差异：

| Tool | Prompt 重点 |
| --- | --- |
| `mdm.getMaterial` | 物料编码、物料名称、状态、单位、分类 |
| `mdm.getWarehouse` | 仓库编码、仓库名称、仓库类型、状态 |
| `inventory.getBalance` | 可用数量、锁定数量、仓库、库位、单位 |
| `sales.getOrder` | 订单号、订单状态、客户、明细行数 |
| `purchase.getOrder` | 订单号、订单状态、供应商、明细行数 |
| fallback | 优先引用展示摘要和展示字段，不扩展猜测 |

prompt 中仍然只包含必要的 display schema、执行状态和错误信息，不写入 API Key、用户 token、敏感请求头、系统环境变量、完整 prompt 日志或模型响应全文。

### 20.3 Tool audit MySQL 持久化

当前 Tool 审计支持两种模式：

```yaml
ai:
  agent:
    tools:
      audit:
        mode: in-memory
        max-records: 500
```

MySQL 模式：

```yaml
ai:
  agent:
    tools:
      audit:
        mode: mysql
```

环境变量示例：

```bash
AI_AGENT_TOOLS_AUDIT_MODE=mysql
AI_AGENT_RAG_REGISTRY_MYSQL_URL=jdbc:mysql://127.0.0.1:3306/scm_ai_agent?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
AI_AGENT_RAG_REGISTRY_MYSQL_USERNAME=root
AI_AGENT_RAG_REGISTRY_MYSQL_PASSWORD=你的本地密码
```

当前最小持久化字段：

- `id`
- `tenant_id`
- `user_id`
- `run_id`
- `tool_name`
- `adapter_mode`
- `success`
- `error_code`
- `latency_ms`
- `created_at`

SQL 脚本位置：

- [deploy/sql/ai-agent-tool-audit.sql](E:/ideaProject/saas-wms-scm/deploy/sql/ai-agent-tool-audit.sql)

该表不保存 API Key、用户 token、敏感请求头、完整 prompt、完整模型响应或大段业务数据。

### 20.4 gateway 18080 验证示例

```http
POST http://localhost:18080/api/v1/ai/tool-calling/chat
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "message": "帮我查库存，物料 1001 在仓库 1 的余额",
  "runId": "run-tool-chat-phase49-001",
  "plannerMode": "spring-ai"
}
```

关键预期字段：

```json
{
  "success": true,
  "data": {
    "runId": "run-tool-chat-phase49-001",
    "plannerMode": "spring-ai",
    "planningSource": "spring-ai",
    "selectedTool": "inventory.getBalance",
    "execution": {
      "success": true,
      "toolName": "inventory.getBalance",
      "data": {
        "displayTitle": "库存余额",
        "displaySummary": "已查询到库存余额",
        "displayFields": [
          {
            "key": "availableQty",
            "label": "可用数量",
            "value": 128
          }
        ],
        "displayItems": [],
        "rawData": {}
      }
    },
    "answer": "模型会按库存策略优先说明可用数量、锁定数量、仓库、库位和单位"
  }
}
```

如果 `ai.agent.tools.audit.mode=mysql`，调用完成后可在 `tool_invocation_audit` 表看到对应 `tenant_id`、`user_id`、`run_id`、`tool_name`、`adapter_mode`、`success`、`error_code`、`latency_ms`。

### 20.5 当前边界

本阶段仍然不实现：

- 多轮 Tool Calling
- 多 Tool 自动编排
- MCP Server
- Workflow
- Multi-Agent
- 长任务编排
- 严格 JSON answer

### 20.6 Tool Calling 包结构约定

为避免 `toolcalling.service` 继续承载过多职责，当前 Tool Calling 相关类按职责拆分：

| 包 | 职责 |
| --- | --- |
| `toolcalling.controller` | HTTP 入口与调试接口 |
| `toolcalling.application` | Tool Calling Chat 应用编排，串联规划、执行、展示 schema 与 answer |
| `toolcalling.planning` | Spring AI Planner、Mock Planner、规划 prompt 与 plan 解析 |
| `toolcalling.answer` | answer 生成、template fallback 与模型总结 prompt 组装 |
| `toolcalling.answer.strategy` | 按 Tool 类型细化 answer prompt 的策略 |
| `toolcalling.display` | Tool 执行结果统一展示 schema 构建 |
| `toolcalling.schema` | ToolDefinition 到 Spring AI tool schema 的转换 |
| `toolcalling.dto` | 接口请求与响应 DTO |
| `toolcalling.model` | 规划、展示 schema、Spring AI descriptor 等内部模型 |

后续新增类优先放入对应职责包；只有跨多个子域的应用编排逻辑才放入 `toolcalling.application`。

## 21. Phase 4.10：Tool 权限标签、路由标签与运行时保护

### 21.1 阶段目标

Phase 4.10 在 Phase 4.9 的 answer prompt strategy 和 Tool audit MySQL 持久化基础上，继续增强 Tool Calling Chat 的工程治理能力：

- 为 ToolDefinition 增加权限标签和租户/用户作用域标记
- 为 ToolDefinition 增加 domain、category、routeTags 等路由元数据
- 在 ToolInvocationService 主链路增加权限校验
- 增加 Tool runtime timeout / retry / 轻量熔断预留配置
- 保持 `/api/v1/ai/tool-calling/chat` 顶层返回字段和 `execution` 顶层字段不变

本阶段仍然只治理只读 Tool，不新增写操作 Tool，不实现 MCP、Workflow、Multi-Agent、长任务编排、多轮 Tool Calling 或多 Tool 自动编排。

### 21.2 Tool 权限标签设计

当前 ToolDefinition 已增加以下治理字段：

- `requiredPermissions`
- `requiredRoles`
- `tenantScoped`
- `userScoped`

当前只读 Tool 默认携带通用权限和业务域权限：

| Tool | requiredPermissions |
| --- | --- |
| `mdm.getMaterial` | `ai.tool.read`, `ai.tool.mdm.read` |
| `mdm.getWarehouse` | `ai.tool.read`, `ai.tool.mdm.read` |
| `inventory.getBalance` | `ai.tool.read`, `ai.tool.inventory.read` |
| `sales.getOrder` | `ai.tool.read`, `ai.tool.sales.read` |
| `purchase.getOrder` | `ai.tool.read`, `ai.tool.purchase.read` |

权限校验由 `ToolPermissionService` 负责。默认配置保持本地只读联调放行；开启严格模式后，会基于 `AgentRequestContext.roles` 中的角色/权限标签判断是否允许执行。

权限失败时：

- 不执行真实 Tool
- 返回稳定的 ToolResponse 失败结构
- `errorCode=403`
- `errorMessage` 保留权限不足语义
- 继续写入 Tool audit

### 21.3 Tool 路由标签设计

当前 ToolDefinition 已增加：

- `domain`
- `category`
- `routeTags`
- `adapterMode`
- `readOnly`

`routeTags` 主要服务后续 Orchestrator / 多轮 Tool Calling 的工具分流，不改变当前 Planner 输出格式。Tool schema 暴露给模型时仅包含安全的 `domain`、`category`、`readOnly`、`routeTags` 等信息，不暴露内部 URL、token、密钥或敏感请求头。

### 21.4 runtime 保护策略

当前新增配置：

```yaml
ai:
  agent:
    tools:
      runtime:
        timeout-ms: 5000
        retry-enabled: true
        max-retries: 1
        circuit-breaker-enabled: false
        failure-threshold: 5
        open-duration-ms: 30000
```

Phase 4.10 已实现：

- `timeout-ms` 配置绑定与日志记录
- `retry-enabled` / `max-retries` 配置绑定
- 对 `ToolClientException` 的最小 retry 封装
- 非可重试 RuntimeException 不重复执行
- 轻量熔断配置预留，不引入复杂第三方依赖

runtime 失败不改变 ToolResponse / execution 顶层结构，并继续写入 Tool audit。

### 21.5 权限配置示例

默认本地联调配置：

```yaml
ai:
  agent:
    tools:
      access-control:
        strict-enabled: false
        default-allow-read-only: true
        admin-roles:
          - ROLE_ADMIN
```

严格权限校验示例：

```yaml
ai:
  agent:
    tools:
      access-control:
        strict-enabled: true
        default-allow-read-only: false
        admin-roles:
          - ROLE_ADMIN
```

严格模式下，调用上下文需要包含对应权限标签，例如 `ai.tool.inventory.read` 或管理员角色 `ROLE_ADMIN`。

### 21.6 gateway 18080 验证示例

```http
POST http://localhost:18080/api/v1/ai/tool-calling/chat
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "message": "帮我查库存，物料 1001 在仓库 1 的余额",
  "runId": "run-tool-chat-phase410-001",
  "plannerMode": "spring-ai"
}
```

关键预期字段：

```json
{
  "success": true,
  "data": {
    "runId": "run-tool-chat-phase410-001",
    "plannerMode": "spring-ai",
    "planningSource": "spring-ai",
    "selectedTool": "inventory.getBalance",
    "execution": {
      "success": true,
      "toolName": "inventory.getBalance",
      "errorCode": null,
      "errorMessage": null,
      "data": {
        "displayTitle": "库存余额",
        "displaySummary": "已查询到库存余额",
        "displayFields": [],
        "displayItems": [],
        "rawData": {}
      },
      "latencyMs": 0
    },
    "answer": "模型基于展示 schema 和原始数据生成的中文回答"
  }
}
```

严格权限校验开启且权限不足时，关键预期字段：

```json
{
  "success": true,
  "data": {
    "selectedTool": "inventory.getBalance",
    "execution": {
      "success": false,
      "toolName": "inventory.getBalance",
      "errorCode": "403",
      "errorMessage": "Tool permission denied: missing_permission",
      "data": null
    }
  }
}
```

### 21.7 后续升级方向

Phase 4.11 可以在当前治理元数据基础上继续推进：

- Orchestrator 使用 `domain/category/routeTags` 做工具候选集过滤
- 在多轮 Tool Calling 中复用权限校验和 runtime 保护
- 将轻量熔断配置升级为可观测状态和半开探测
- 增加更细粒度的 Tool policy 和租户级策略

## 22. Phase 4.11：Tool 候选集过滤与 runtime 状态观测

### 22.1 阶段目标

Phase 4.11 在 Phase 4.10 的权限标签、路由标签和 runtime 保护基础上继续向 Orchestrator 过渡：

- 基于 `domain`、`category`、`routeTags`、`readOnly` 缩小 Planner 可见的 Tool schema 候选集
- 支持 Tool Calling Chat 请求携带可选 route hint，并在缺省时从用户问题做轻量关键词推断
- 增加 Tool runtime 内存状态统计和只读查询接口
- 在 `circuit-breaker-enabled=true` 时启用轻量熔断状态机

本阶段不实现完整 Orchestrator，不改变 `/api/v1/ai/tool-calling/chat` 顶层返回字段，不改变 `execution` 顶层字段，不删除 `execution.data.rawData`。

### 22.2 Tool 候选集过滤设计

`ToolCandidateFilterService` 负责候选集过滤，输入只包含安全路由提示：

- `userMessage`
- `requestedDomain`
- `requestedCategory`
- `routeTags`
- `readOnlyOnly`
- `maxCandidates`

过滤规则：

- 先按 `readOnlyOnly` 保留只读工具
- 再按 `requestedDomain` / `requestedCategory` / `routeTags` 过滤
- 如果过滤结果为空，安全回退到全量只读 Tool，避免 Planner 无工具可选
- 日志只记录租户、用户、runId、过滤前后数量和是否 fallback，不记录 prompt 全文、token、内部 header 或业务 rawData

### 22.3 route hint 与关键词推断

Tool Calling Chat 请求兼容旧字段，并可选增加：

```json
{
  "requestedDomain": "inventory",
  "requestedCategory": "stock",
  "routeTags": ["inventory", "balance"]
}
```

如果请求未显式传入 route hint，当前按用户问题做轻量关键词推断：

| 关键词 | inferred domain |
| --- | --- |
| 库存 / 余额 / 可用 | `inventory` |
| 物料 / 仓库 / 主数据 | `mdm` |
| 销售订单 / 销售 | `sales` |
| 采购订单 / 采购 | `purchase` |

`SpringAiToolPlanner` 构造 prompt 时只把过滤后的 Tool schema 传给模型，但 Planner 输出 JSON 格式保持不变。显式 `requestedTool` 仍按原优先级直接执行，不被候选过滤覆盖。

### 22.4 runtime 状态字段

`ToolRuntimeProtectionService` 维护内存态 `ToolRuntimeStatus`：

| 字段 | 含义 |
| --- | --- |
| `toolName` | Tool 名称 |
| `totalCalls` | 总调用次数，包含被熔断拒绝的调用 |
| `successCount` | 成功次数 |
| `failureCount` | 失败次数 |
| `retryCount` | retry 次数 |
| `lastFailureAt` | 最近一次失败时间 |
| `lastErrorType` | 最近一次失败异常类型 |
| `circuitState` | `CLOSED` / `OPEN` / `HALF_OPEN` |
| `openedAt` | 最近一次进入 OPEN 的时间 |

runtime 状态接口不返回请求参数原文、业务 rawData、prompt、模型响应、API Key、用户 token 或敏感请求头。

### 22.5 轻量熔断状态机

默认 `circuit-breaker-enabled=false`，不影响本地真实模型和真实 Tool 联调主路径。

开启后状态流转：

- `CLOSED`：正常执行 Tool；失败次数达到 `failure-threshold` 后进入 `OPEN`
- `OPEN`：不执行真实 Tool，返回稳定失败结构并写入 Tool audit
- `HALF_OPEN`：`open-duration-ms` 到期后允许下一次调用探测；成功恢复 `CLOSED`，失败回到 `OPEN`

熔断打开时仍保持 ToolResponse / `execution` 顶层字段稳定；当前错误语义为 `Tool circuit is open: <toolName>`，audit 继续记录失败。

### 22.6 gateway 18080 Tool Calling Chat 验证

```http
POST http://localhost:18080/api/v1/ai/tool-calling/chat
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "message": "帮我查 MAT-001 的库存余额",
  "runId": "run-tool-chat-phase411-001",
  "plannerMode": "spring-ai",
  "requestedDomain": "inventory",
  "routeTags": ["inventory", "balance"]
}
```

关键预期字段：

```json
{
  "success": true,
  "data": {
    "runId": "run-tool-chat-phase411-001",
    "plannerMode": "spring-ai",
    "planningSource": "spring-ai",
    "selectedTool": "inventory.getBalance",
    "execution": {
      "success": true,
      "toolName": "inventory.getBalance",
      "errorCode": null,
      "errorMessage": null,
      "data": {
        "displayTitle": "库存余额",
        "displaySummary": "已查询到库存余额",
        "displayFields": [],
        "displayItems": [],
        "rawData": {}
      },
      "latencyMs": 0
    },
    "answer": "模型基于过滤后的 Tool schema、展示 schema 和原始数据生成的中文回答"
  }
}
```

### 22.7 gateway 18080 runtime status 验证

查询全部 runtime 状态：

```http
GET http://localhost:18080/api/v1/ai/tools/runtime/status
Authorization: Bearer <accessToken>
```

关键预期字段：

```json
{
  "success": true,
  "data": [
    {
      "toolName": "inventory.getBalance",
      "totalCalls": 1,
      "successCount": 1,
      "failureCount": 0,
      "retryCount": 0,
      "lastFailureAt": null,
      "lastErrorType": null,
      "circuitState": "CLOSED",
      "openedAt": null
    }
  ]
}
```

查询单个 Tool：

```http
GET http://localhost:18080/api/v1/ai/tools/runtime/status/inventory.getBalance
Authorization: Bearer <accessToken>
```

关键预期字段同上，但 `data` 为单个对象。

### 22.8 runtime 熔断配置示例

```yaml
ai:
  agent:
    tools:
      runtime:
        timeout-ms: 5000
        retry-enabled: true
        max-retries: 1
        circuit-breaker-enabled: true
        failure-threshold: 3
        open-duration-ms: 30000
```

### 22.9 后续升级方向

Phase 4.12 可继续推进：

- 将候选过滤结果作为 Orchestrator 的可复用前置能力
- 增加多轮 Tool Calling 的上下文状态和步骤记录
- 将 runtime 状态纳入更完整的运维监控面板
- 在保持安全边界的前提下引入更细粒度的租户级 Tool policy

## 23. Phase 4.12：单步 Orchestrator Run/Step 记录

### 23.1 目标与边界

Phase 4.12 在 Phase 4.11 的 Tool 候选集过滤、权限治理、审计和 runtime 保护基础上，启动 Orchestrator 的最小落地。本阶段只做单步 Tool Calling 的 run/step 结构化记录，让现有“规划 -> 执行 -> 总结”链路具备后续扩展多轮 Tool Calling 的状态骨架。

本阶段保持 `/api/v1/ai/tool-calling/chat` 顶层返回字段不变，保持 `execution` 顶层字段不变，不删除 `execution.data.rawData`，不改变 requestedTool 优先级，不改变 Spring AI Planner 主路径，也不实现自动多 Tool 编排。

### 23.2 Orchestrator 在本项目中的定位

Orchestrator 不是替代 Planner，也不是本阶段的多 Agent 编排器。它在当前项目中的定位是：

- 记录一次 Tool Calling Chat 的 run 生命周期
- 记录每个 Tool 执行 step 的状态、耗时和概要结果
- 复用现有 Planner、ToolInvocationService、display schema、answer summary、audit、permission、runtime protection
- 为后续多步、多 Tool、跨步骤上下文传递预留统一模型

当前版本只承载单步记录，避免在业务链路稳定前引入复杂自动编排。

### 23.3 Run / Step 模型

`ToolOrchestrationRun` 表示一次 Tool Calling Chat 运行，核心字段包括：

- `runId`
- `tenantId`
- `userId`
- `userMessage`
- `plannerMode`
- `answerMode`
- `requestedTool`
- `requestedDomain`
- `requestedCategory`
- `routeTags`
- `steps`
- `finalAnswer`
- `success`
- `createdAt`
- `finishedAt`
- `latencyMs`

`ToolOrchestrationStep` 表示一次 Tool 执行步骤，核心字段包括：

- `stepId`
- `stepNo`
- `toolName`
- `arguments`
- `reason`
- `status`
- `execution`
- `startedAt`
- `finishedAt`
- `latencyMs`

`ToolOrchestrationStepStatus` 当前包含：

- `PENDING`
- `RUNNING`
- `SUCCESS`
- `FAILED`
- `SKIPPED`

状态接口中的 `execution` 使用脱敏概要，不返回完整 `rawData`、prompt、模型响应、内部请求头或敏感信息。概要字段包括：

- `success`
- `toolName`
- `errorCode`
- `errorMessage`
- `latencyMs`
- `displayTitle`
- `displaySummary`

### 23.4 Orchestrator 配置

默认保持向后兼容，`orchestrator.enabled=false` 时 chat 主链路保持 Phase 4.11 行为。

```yaml
ai:
  agent:
    tool-calling:
      orchestrator:
        enabled: false
        record-runs: true
        max-records: 100
```

本地联调可开启单步 run/step 记录：

```yaml
ai:
  agent:
    tool-calling:
      orchestrator:
        enabled: true
        record-runs: true
        max-records: 100
```

配置说明：

- `enabled`：是否启用 Orchestrator 单步记录。
- `record-runs`：是否写入 in-memory run store。
- `max-records`：最多保留的 run 数量，超过后裁剪最早记录。

### 23.5 与候选过滤、权限、审计、runtime 的关系

Phase 4.12 的 Orchestrator 不绕过任何既有治理能力：

- Tool candidate filter 仍参与 Planner schema 裁剪。
- requestedTool 仍优先于模型规划结果。
- ToolPermissionService 仍在 ToolInvocationService 主链路执行权限校验。
- 权限失败时 step 标记为 `FAILED`，真实 Tool 不执行，Tool audit 仍写入。
- runtime retry 和 circuit breaker 仍在 ToolInvocationService 主链路生效。
- 熔断打开或运行时失败时 step 标记为 `FAILED`，Tool audit 仍写入。
- answer summary 和 template fallback 行为保持不变。

### 23.6 gateway 18080 Tool Calling Chat 验证

```http
POST http://localhost:18080/api/v1/ai/tool-calling/chat
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "message": "帮我查 MAT-001 的库存余额",
  "runId": "run-tool-chat-phase412-001",
  "plannerMode": "spring-ai",
  "requestedDomain": "inventory",
  "routeTags": [
    "inventory",
    "balance"
  ]
}
```

关键预期返回字段：

```json
{
  "success": true,
  "data": {
    "runId": "run-tool-chat-phase412-001",
    "plannerMode": "spring-ai",
    "planningSource": "spring-ai",
    "fallbackUsed": false,
    "selectedTool": "inventory.getBalance",
    "toolArguments": {},
    "planningReason": "基于用户问题选择库存余额查询工具",
    "execution": {
      "success": true,
      "toolName": "inventory.getBalance",
      "errorCode": null,
      "errorMessage": null,
      "data": {
        "displayTitle": "库存余额",
        "displaySummary": "已查询到库存余额",
        "displayFields": [],
        "displayItems": [],
        "rawData": {}
      },
      "latencyMs": 0
    },
    "answer": "模型基于展示 schema 和原始数据生成的中文回答",
    "latencyMs": 0
  }
}
```

### 23.7 gateway 18080 Orchestration status 验证

查询最近的 Orchestration run：

```http
GET http://localhost:18080/api/v1/ai/tool-calling/orchestrations?limit=20
Authorization: Bearer <accessToken>
```

关键预期返回字段：

```json
{
  "success": true,
  "data": [
    {
      "runId": "run-tool-chat-phase412-001",
      "tenantId": 1,
      "userId": 10001,
      "plannerMode": "spring-ai",
      "answerMode": "spring-ai",
      "requestedDomain": "inventory",
      "routeTags": [
        "inventory",
        "balance"
      ],
      "steps": [
        {
          "stepNo": 1,
          "toolName": "inventory.getBalance",
          "status": "SUCCESS",
          "execution": {
            "success": true,
            "toolName": "inventory.getBalance",
            "errorCode": null,
            "errorMessage": null,
            "latencyMs": 0,
            "displayTitle": "库存余额",
            "displaySummary": "已查询到库存余额"
          }
        }
      ],
      "finalAnswer": "模型基于展示 schema 和原始数据生成的中文回答",
      "success": true,
      "latencyMs": 0
    }
  ]
}
```

按 runId 查询单个 Orchestration run：

```http
GET http://localhost:18080/api/v1/ai/tool-calling/orchestrations/run-tool-chat-phase412-001
Authorization: Bearer <accessToken>
```

关键预期字段同上，但 `data` 为单个 run 对象。状态接口不会返回完整 `rawData`、完整 prompt、完整模型响应、用户 token 或内部 HTTP header。

### 23.8 后续升级方向

Phase 4.13 可继续推进：

- 引入真正的多步骤 Orchestration plan，但仍保持自动编排低风险开关。
- 增加步骤间上下文传递和上一步执行结果摘要。
- 增加 step-level answer / final answer 的更细粒度观测事件。
- 评估 Orchestration run 从 in-memory 升级到 MySQL 持久化的表结构。

## 24. Phase 4.13：显式 Orchestration Plan 与 dry-run 多步骤骨架

### 24.1 目标与边界

Phase 4.13 在 Phase 4.12 的单步 Orchestrator run/step 记录基础上，增加显式 `ToolOrchestrationPlan` 模型、步骤间安全摘要和受控 dry-run 多步骤表达。默认主路径仍是单步 Tool Calling，不自动执行第二个及后续真实 Tool。

本阶段继续保持 `/api/v1/ai/tool-calling/chat` 顶层返回字段不变，保持 `execution` 顶层字段不变，不删除 `execution.data.rawData`，不改变 requestedTool 优先级，不改变 Spring AI Planner 主路径。

### 24.2 Plan 模型设计

`ToolOrchestrationPlan` 用于表达一次 run 的执行计划，核心字段包括：

- `planId`
- `runId`
- `mode`
- `objective`
- `steps`
- `maxSteps`
- `generatedBy`
- `createdAt`

Plan mode 当前包含：

- `SINGLE_STEP`：默认模式，只构造并执行当前选中的一个 Tool。
- `MULTI_STEP_DRY_RUN`：受控 dry-run 模式，可表达后续计划步骤，但第二个及后续步骤标记为 `SKIPPED`，不会执行真实 Tool。
- `MULTI_STEP_CONTROLLED`：预留模式，本阶段不作为默认主路径。

### 24.3 Step 上下文摘要

`ToolOrchestrationStep` 在 Phase 4.13 增加：

- `dependsOnStepIds`
- `inputSummary`
- `outputSummary`
- `skipReason`

`outputSummary` 由 `ToolOrchestrationStepSummaryBuilder` 基于脱敏 execution 概要生成，只包含：

- `success`
- `toolName`
- `errorCode`
- `errorMessage`
- `displayTitle`
- `displaySummary`
- `latencyMs`

摘要不会读取或返回完整 `rawData`、完整 prompt、完整模型响应、用户 token、敏感请求头或内部 HTTP header。后续步骤的 `inputSummary` 可以引用前置步骤的 `outputSummary`，为后续真正多轮 Tool Calling 做上下文传递准备。

### 24.4 Orchestrator plan 配置

默认配置仍保持单步：

```yaml
ai:
  agent:
    tool-calling:
      orchestrator:
        enabled: true
        record-runs: true
        max-records: 100
        plan-mode: single-step
        max-steps: 1
        multi-step-enabled: false
        dry-run-enabled: false
```

受控 dry-run 示例：

```yaml
ai:
  agent:
    tool-calling:
      orchestrator:
        enabled: true
        record-runs: true
        max-records: 100
        plan-mode: multi-step-dry-run
        max-steps: 2
        multi-step-enabled: true
        dry-run-enabled: true
```

配置行为：

- `multi-step-enabled=false` 时只允许单步计划。
- `dry-run-enabled=true` 且 `plan-mode=multi-step-dry-run` 时，可以构造后续计划步骤。
- dry-run 后续步骤只记录为 `SKIPPED`，不会调用真实 Tool。
- 用户显式传入 `requestedTool` 时，仍强制构造 `SINGLE_STEP` plan。

### 24.5 失败与审计规则

- 第一个真实执行步骤成功时，后续 dry-run 步骤仍保持 `SKIPPED`，`skipReason=multi-step dry-run only; real Tool is not executed`。
- 第一个真实执行步骤失败、权限拒绝、参数校验失败或 runtime circuit open 时，当前步骤进入 `FAILED`。
- 当前步骤失败后，后续计划步骤进入或保持 `SKIPPED`，`skipReason=previous step failed; real Tool is not executed`。
- Tool audit 仍只记录真实执行过的 Tool 调用，或在主链路中被权限/runtime 保护拒绝的 Tool 调用。
- 纯 dry-run 的 `SKIPPED` 步骤不会伪造真实 Tool audit。

### 24.6 gateway 18080 Tool Calling Chat 验证

```http
POST http://localhost:18080/api/v1/ai/tool-calling/chat
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "message": "帮我查 MAT-001 的库存余额",
  "runId": "run-tool-chat-phase413-001",
  "plannerMode": "spring-ai",
  "requestedDomain": "inventory",
  "routeTags": [
    "inventory",
    "balance"
  ]
}
```

关键预期返回字段仍保持 Phase 4.12 兼容：

```json
{
  "success": true,
  "data": {
    "runId": "run-tool-chat-phase413-001",
    "plannerMode": "spring-ai",
    "planningSource": "spring-ai",
    "fallbackUsed": false,
    "selectedTool": "inventory.getBalance",
    "execution": {
      "success": true,
      "toolName": "inventory.getBalance",
      "data": {
        "displayTitle": "库存余额",
        "displaySummary": "已查询到库存余额",
        "displayFields": [],
        "displayItems": [],
        "rawData": {}
      },
      "latencyMs": 0
    },
    "answer": "模型基于展示 schema 和原始数据生成的中文回答"
  }
}
```

### 24.7 gateway 18080 Orchestration status 验证

```http
GET http://localhost:18080/api/v1/ai/tool-calling/orchestrations/run-tool-chat-phase413-001
Authorization: Bearer <accessToken>
```

单步模式关键预期字段：

```json
{
  "success": true,
  "data": {
    "runId": "run-tool-chat-phase413-001",
    "plan": {
      "planId": "run-tool-chat-phase413-001-plan-1",
      "mode": "SINGLE_STEP",
      "objective": "帮我查 MAT-001 的库存余额",
      "maxSteps": 1,
      "generatedBy": "service-single-step"
    },
    "steps": [
      {
        "stepNo": 1,
        "toolName": "inventory.getBalance",
        "status": "SUCCESS",
        "outputSummary": "tool=inventory.getBalance, success=true, displayTitle=库存余额, displaySummary=已查询到库存余额, latencyMs=0",
        "execution": {
          "success": true,
          "toolName": "inventory.getBalance",
          "displayTitle": "库存余额",
          "displaySummary": "已查询到库存余额"
        }
      }
    ]
  }
}
```

dry-run 模式下可看到后续跳过步骤：

```json
{
  "data": {
    "plan": {
      "mode": "MULTI_STEP_DRY_RUN",
      "maxSteps": 2,
      "generatedBy": "service-dry-run"
    },
    "steps": [
      {
        "stepNo": 1,
        "status": "SUCCESS"
      },
      {
        "stepNo": 2,
        "toolName": "orchestrator.futureStep",
        "status": "SKIPPED",
        "inputSummary": "previousStep=run-tool-chat-phase413-001-step-1, outputSummary=...",
        "skipReason": "multi-step dry-run only; real Tool is not executed"
      }
    ]
  }
}
```

状态接口不会返回完整 `rawData`、完整 prompt、完整模型响应、用户 token 或内部 HTTP header。

### 24.8 后续升级方向

Phase 4.14 可继续推进：

- 引入受控多步骤 planner，但默认仍可关闭。
- 为步骤间上下文增加更稳定的引用格式，例如 `stepRef`。
- 评估 Orchestration run / plan / step 的 MySQL 持久化。
- 在只读 Tool 范围内验证真正多步骤执行的权限、审计和 runtime 保护闭环。

## 25. 当前阶段不做的事情

本阶段不实现：

- 写操作 Tool
- 真实 LLM 自动多轮 Tool Calling 编排
- 自动多 Tool 编排
- MCP Server
- Workflow
- Multi-Agent
- 长任务编排

## 26. 后续建议

Phase 4.14 可以继续往下面推进：

- 引入受控多步骤 planner 与 stepRef 引用
- 在只读 Tool 范围内执行第二个真实步骤
- 为 Orchestrator run 设计 MySQL 持久化与分页查询

