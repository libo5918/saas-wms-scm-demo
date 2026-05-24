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

## 25. Phase 4.14：受控 Orchestrator Planner 与 stepRef 引用

### 25.1 目标与边界

Phase 4.14 在 Phase 4.13 的显式 plan 和 dry-run 多步骤骨架基础上，增加 Orchestrator 层 Planner、stepRef 上下文引用和 Plan 安全校验。本阶段仍不默认执行多个真实 Tool，SpringAiToolPlanner 仍负责当前单步 Tool 选择主路径。

本阶段继续保持 `/api/v1/ai/tool-calling/chat` 顶层返回字段不变，保持 `execution` 顶层字段不变，不删除 `execution.data.rawData`，不改变 requestedTool 优先级。

### 25.2 Orchestrator Planner 职责边界

`ToolOrchestrationPlannerService` 只负责把当前单步 `ToolCallingPlan` 包装成受控的 `ToolOrchestrationPlan`：

- 默认生成 `SINGLE_STEP` plan。
- `requestedTool` 存在时强制生成 `SINGLE_STEP` plan。
- `plan-mode=multi-step-dry-run` 且 `multi-step-enabled=true` 且 `dry-run-enabled=true` 时，可生成 dry-run 多步骤 plan。
- `plan-mode=multi-step-controlled` 且 `multi-step-enabled=true` 时，可生成 controlled plan，但第二个及后续真实 Tool 仍不执行。
- 候选只读 Tool 不足或校验失败时，安全回退 `SINGLE_STEP`。

SpringAiToolPlanner 的职责不变：它仍只负责根据用户问题和候选 Tool schema 选择当前要执行的一个 Tool。

### 25.3 stepRef / inputRefs / outputRef 设计

Phase 4.14 为 `ToolOrchestrationStep` 增加稳定引用字段：

- `stepRef`：步骤引用名，例如 `step-1`。
- `inputRefs`：当前步骤引用的前置安全摘要，例如 `["step-1.outputSummary"]`。
- `outputRef`：当前步骤输出摘要路径，例如 `$.steps[0].outputSummary`。

这些引用只指向 `outputSummary`，不指向完整 `rawData`。状态接口可以展示 stepRef 信息，用于后续多步骤上下文传递和调试。

### 25.4 Plan Validator 安全校验

`ToolOrchestrationPlanValidator` 在 Orchestrator plan 进入 run 前执行安全校验：

- steps 数量不得超过 `maxSteps`。
- dry-run 后续步骤必须为 `SKIPPED`。
- controlled 后续步骤在 Phase 4.14 必须为 `SKIPPED`。
- Tool 必须存在于 ToolRegistry 且 `readOnly=true`，除明确的 `orchestrator.futureStep` dry-run placeholder 外。
- stepRef/inputRefs 只能引用前置步骤。
- `inputSummary` / `outputSummary` 不得包含 `rawData`、`prompt`、`token`、`authorization`、`cookie`、`secret` 等敏感关键词。

校验失败时，Planner 会安全回退到 `SINGLE_STEP` plan，不影响当前真实 Tool 执行主链路。

### 25.5 dry-run / controlled 行为

dry-run 模式：

- 第一个步骤仍是真实 Tool Calling Chat 当前选中的 Tool。
- 第二个及后续步骤仅作为计划占位。
- 后续步骤状态为 `SKIPPED`。
- 不执行后续真实 Tool。
- 不为后续 `SKIPPED` 步骤写伪 Tool audit。

controlled 模式：

- 当前阶段只生成 controlled plan。
- 后续步骤仍为 `SKIPPED`。
- 真实第二步执行留到后续阶段在只读 Tool 范围内逐步开启。

### 25.6 gateway 18080 Tool Calling Chat 验证

```http
POST http://localhost:18080/api/v1/ai/tool-calling/chat
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "message": "帮我查 MAT-001 的库存余额",
  "runId": "run-tool-chat-phase414-001",
  "plannerMode": "spring-ai",
  "requestedDomain": "inventory",
  "routeTags": [
    "inventory",
    "balance"
  ]
}
```

关键预期返回字段仍保持兼容：

```json
{
  "success": true,
  "data": {
    "runId": "run-tool-chat-phase414-001",
    "plannerMode": "spring-ai",
    "planningSource": "spring-ai",
    "selectedTool": "inventory.getBalance",
    "execution": {
      "success": true,
      "toolName": "inventory.getBalance",
      "data": {
        "displayTitle": "库存余额",
        "displaySummary": "已查询到库存余额",
        "rawData": {}
      }
    },
    "answer": "模型基于展示 schema 和原始数据生成的中文回答"
  }
}
```

### 25.7 gateway 18080 Orchestration status 验证

```http
GET http://localhost:18080/api/v1/ai/tool-calling/orchestrations/run-tool-chat-phase414-001
Authorization: Bearer <accessToken>
```

关键预期字段：

```json
{
  "success": true,
  "data": {
    "runId": "run-tool-chat-phase414-001",
    "plan": {
      "mode": "MULTI_STEP_DRY_RUN",
      "generatedBy": "orchestration-planner-dry-run",
      "maxSteps": 2
    },
    "steps": [
      {
        "stepNo": 1,
        "stepRef": "step-1",
        "outputRef": "$.steps[0].outputSummary",
        "status": "SUCCESS",
        "outputSummary": "tool=inventory.getBalance, success=true, displayTitle=库存余额..."
      },
      {
        "stepNo": 2,
        "stepRef": "step-2",
        "inputRefs": [
          "step-1.outputSummary"
        ],
        "outputRef": "$.steps[1].outputSummary",
        "status": "SKIPPED",
        "skipReason": "multi-step dry-run only; real Tool is not executed"
      }
    ]
  }
}
```

状态接口不会返回完整 `rawData`、完整 prompt、完整模型响应、用户 token、敏感 header 或内部 HTTP header。

### 25.8 后续升级方向

Phase 4.15 可继续推进：

- 在只读 Tool 范围内开启第二个真实步骤的受控执行。
- 引入 stepRef resolver，把前置 `outputSummary` 转成后续 Tool 参数候选。
- 为 Orchestration run / plan / step 设计 MySQL 持久化。
- 增强审计事件，区分真实执行、权限拒绝、runtime 熔断和 dry-run skipped。

## 26. Phase 4.15：受控二步只读 Tool 执行与 Phase 4 收敛

### 26.1 目标与边界

Phase 4.15 开始切换为“Java AI Agent 企业级面试展示优先”推进策略，目标是把 Phase 4 Tools 主线收敛成可运行、可讲解、可演示的企业级闭环：

- 默认行为仍保持单步 Tool Calling，不自动执行多个真实 Tool。
- 仅当显式开启 controlled 配置时，允许执行第二个只读 Tool。
- 最多执行两个真实步骤，不允许第三步真实执行。
- 不新增写操作 Tool，不改变 `/api/v1/ai/tool-calling/chat` 顶层字段，不改变 `execution` 顶层字段。
- 第二步执行必须复用 `ToolInvocationService`，因此权限校验、runtime retry/circuit breaker 和 Tool audit 继续生效。

### 26.2 controlled 二步执行配置

默认配置保持保守：

```yaml
ai:
  agent:
    tool-calling:
      orchestrator:
        enabled: true
        record-runs: true
        plan-mode: single-step
        max-steps: 1
        multi-step-enabled: false
        dry-run-enabled: false
        controlled-execution-enabled: false
        max-executable-steps: 1
        allow-second-step-read-only: true
```

显式验证受控二步只读执行时可使用：

```yaml
ai:
  agent:
    tool-calling:
      orchestrator:
        enabled: true
        record-runs: true
        plan-mode: multi-step-controlled
        max-steps: 2
        multi-step-enabled: true
        dry-run-enabled: false
        controlled-execution-enabled: true
        max-executable-steps: 2
        allow-second-step-read-only: true
```

### 26.3 固定二步组合

Phase 4.15 只实现一个低风险固定组合：

```text
mdm.getMaterial -> inventory.getBalance
```

当第一步为 `mdm.getMaterial`，且 `inventory.getBalance` 是已注册只读 Tool 时，Orchestrator controlled plan 可以生成第二步 `inventory.getBalance`。第二步只有在 controlled 配置显式开启后才真实执行。

参数派生规则：

1. `materialId` 优先从第一步 `mdm.getMaterial` 的 Tool 返回结果中提取。服务端只把 `materialId/materialCode/warehouseId/warehouseCode/locationId/locationCode` 等白名单字段写入 `safeFields` 和 `outputSummary`，不会把完整 `rawData` 透传给后续步骤。
2. `warehouseId` 优先从第一步 arguments、用户原始 `message` 或安全摘要中解析，例如 `仓库ID 1`、`warehouseId=1`。
3. `locationId` 同样支持从第一步 arguments、用户原始 `message` 或安全摘要解析，例如 `库位ID 2`、`locationId=2`；当前作为可选参数传递。

如果无法解析 `materialId` 或 `warehouseId`，第二步保持 `SKIPPED`，`skipReason/inputResolveError` 说明参数不足，不调用真实 Tool，也不写伪 audit。

### 26.4 stepRef resolver 与状态字段

Phase 4.15 新增后续步骤参数解析器，只允许读取：

- `userMessage`
- `previousStep.arguments.materialId/warehouseId/locationId`
- `previousStep.execution.safeFields`
- `previousStep.outputSummary`

不允许读取或输出：

- `rawData`
- prompt
- 模型响应全文
- token / authorization / cookie
- 内部 HTTP header

Orchestration status 中 step 增加执行观测字段：

```json
{
  "stepNo": 2,
  "toolName": "inventory.getBalance",
  "executable": true,
  "executed": true,
  "inputResolved": true,
  "inputResolveError": null,
  "skipReason": null,
  "arguments": {
    "materialId": 1001,
    "warehouseId": 1,
    "locationId": 2
  }
}
```

状态含义：

- `SUCCESS`：第二步参数解析成功、权限/runtime 通过、Tool 执行成功。
- `SKIPPED`：controlled 未开启、参数不足、非只读 Tool、placeholder 或前置步骤失败。
- `FAILED`：第二步真实执行已发起，但权限、runtime、业务调用等返回失败。

### 26.5 Gateway 18080 Chat 验证

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
  "message": "帮我查物料 MAT-001，并看看仓库ID 1、库位ID 2 的库存",
  "runId": "run-tool-chat-phase415-001",
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
    "runId": "run-tool-chat-phase415-001",
    "plannerMode": "spring-ai",
    "planningSource": "spring-ai",
    "selectedTool": "mdm.getMaterial",
    "execution": {
      "success": true,
      "toolName": "mdm.getMaterial",
      "data": {
        "displayTitle": "物料信息",
        "displaySummary": "已查询到物料 MAT-001",
        "rawData": {
          "materialId": 1001,
          "materialCode": "MAT-001"
        }
      }
    },
    "answer": "模型基于物料 Tool 结果总结的中文回答"
  }
}
```

`/chat` 仍只返回第一步主链路 execution，第二步受控执行结果通过 Orchestration status 查看，避免破坏既有接口契约。

### 26.6 Gateway 18080 Orchestration status 验证

```http
GET http://localhost:18080/api/v1/ai/tool-calling/orchestrations/run-tool-chat-phase415-001
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
    "runId": "run-tool-chat-phase415-001",
    "plan": {
      "mode": "MULTI_STEP_CONTROLLED",
      "maxSteps": 2
    },
    "steps": [
      {
        "stepNo": 1,
        "toolName": "mdm.getMaterial",
        "status": "SUCCESS",
        "executed": true,
        "outputSummary": "tool=mdm.getMaterial, success=true, safeFields={materialId=1001, materialCode=MAT-001}..."
      },
      {
        "stepNo": 2,
        "toolName": "inventory.getBalance",
        "status": "SUCCESS",
        "executable": true,
        "executed": true,
        "inputResolved": true,
        "arguments": {
          "materialId": 1001,
          "warehouseId": 1,
          "locationId": 2
        },
        "inputRefs": ["step-1.outputSummary"]
      }
    ]
  }
}
```

状态接口仍不返回完整 `rawData`、完整 prompt、完整模型响应、用户 token、敏感 header 或内部 HTTP header。

### 26.7 Phase 4 Tools 主线收敛结论

Phase 4 到 4.15 已经具备面试可讲的 Tools 企业级能力：

- Tool schema 暴露与真实模型规划。
- Tool 执行、权限、runtime retry/circuit breaker、audit。
- Tool 结果 display schema 与模型总结 answer。
- Orchestrator run / plan / step / status。
- 受控二步只读 Tool 执行，且不破坏主接口契约。

后续不再继续深挖低收益 Tool 细节，优先进入 RAG + Tool 组合问答，让项目更快形成“知识库检索 + 业务工具执行 + Orchestrator 状态”的完整面试演示链路。

## 27. 当前阶段不做的事情

本阶段不实现：

- 写操作 Tool
- 真实 LLM 自动多轮 Tool Calling 编排
- 自动多 Tool 编排
- MCP Server
- Workflow
- Multi-Agent
- 长任务编排

## 28. 后续建议

Phase 4.15 之后，Tools 主线收敛，后续优先进入 RAG + Tool 组合问答。

## 29. Phase 5.1：RAG + Tool 组合问答

### 29.1 目标与边界

Phase 5.1 开始进入“知识库检索 + 实时业务 Tool 查询 + 模型总结”的企业级 Agent 闭环：

- 新增 `POST /api/v1/ai/agent/chat` 作为面试展示入口。
- 不替换已有 `/api/v1/ai/rag/**` 和 `/api/v1/ai/tool-calling/**` 接口。
- 不改变 `/api/v1/ai/tool-calling/chat` 顶层字段和 `execution` 顶层字段。
- 不实现 MCP、Workflow、Multi-Agent、长任务编排或复杂多轮自动规划。
- 新接口只返回 RAG、Tool、Orchestration 的脱敏概要，不返回完整 prompt、模型响应、token、敏感 header 或完整业务 `rawData`。

### 29.2 RAG 与 Tool 职责边界

RAG 负责回答规则、口径、字段含义和流程背景，例如：

- 库存可用数量口径。
- 物料状态含义。
- 仓库和库位字段解释。

Tool 负责查询实时业务数据，例如：

- `mdm.getMaterial` 查询物料主数据。
- `inventory.getBalance` 查询库存余额。
- `sales.getOrder` / `purchase.getOrder` 查询订单。

组合回答时，实时事实优先使用 Tool 数据；规则解释优先使用 RAG 片段。RAG 未召回时不编造知识库内容，Tool 失败时保留真实失败语义。

### 29.3 简单意图路由

Phase 5.1 使用关键词规则实现最小意图路由：

- `RAG_ONLY`：规则、口径、含义、流程说明类问题。
- `TOOL_ONLY`：物料、仓库、库存、销售订单、采购订单等实时查询问题。
- `RAG_TOOL`：同时包含知识解释和实时查询的问题。

受控二步库存查询增加意图门禁：

- “帮我查物料 MAT-001” 只执行 `mdm.getMaterial`，第二步 `inventory.getBalance` 保持 `SKIPPED`。
- “帮我查物料 MAT-001，并看看仓库ID 1、库位ID 2 的库存” 才允许执行 `inventory.getBalance`。

### 29.4 组合 Prompt 设计

`/api/v1/ai/agent/chat` 最终总结 prompt 包含：

- 用户原始问题。
- RAG 检索 chunk 的短摘要。
- Tool execution 的 display schema。
- Orchestrator steps 的安全摘要。
- Tool 失败原因。

Prompt 不包含完整 `rawData`、完整 prompt、完整模型响应、API Key、用户 token、敏感 header 或内部 HTTP header。

### 29.5 Gateway 18080 RAG + Tool 验证

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
  "runId": "run-agent-phase51-001",
  "knowledgeBaseId": "kb-scm-demo",
  "topK": 3,
  "message": "按库存可用数量口径解释，并查物料 MAT-001 在仓库ID 1、库位ID 2 的库存",
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
    "runId": "run-agent-phase51-001",
    "intentType": "RAG_TOOL",
    "answer": "模型基于知识库口径、物料信息和库存信息生成的中文回答",
    "rag": {
      "knowledgeBaseId": "kb-scm-demo",
      "retrievedCount": 1,
      "chunks": [
        {
          "title": "库存可用数量口径",
          "contentSnippet": "库存可用数量通常等于现存数量减去锁定数量..."
        }
      ]
    },
    "tool": {
      "selectedTool": "mdm.getMaterial",
      "execution": {
        "success": true,
        "toolName": "mdm.getMaterial",
        "displayTitle": "物料信息"
      }
    },
    "orchestration": {
      "enabled": true,
      "runId": "run-agent-phase51-001",
      "planMode": "MULTI_STEP_CONTROLLED",
      "stepCount": 2
    }
  }
}
```

### 29.6 Tool Calling Chat 回归验证

```http
POST http://localhost:18080/api/v1/ai/tool-calling/chat
Authorization: Bearer <accessToken>
Content-Type: application/json
X-Tenant-Id: 1
X-User-Id: 10001
X-Username: admin
X-User-Roles: ROLE_ADMIN
```

只查物料：

```json
{
  "message": "帮我查物料 MAT-001",
  "runId": "run-tool-chat-phase51-material-only",
  "plannerMode": "spring-ai",
  "requestedDomain": "mdm",
  "routeTags": ["mdm", "material"]
}
```

预期：`/chat` 返回物料信息；Orchestration status 中第二步为 `SKIPPED`，`skipReason` 表示用户未表达库存查询意图。

查物料并查库存：

```json
{
  "message": "帮我查物料 MAT-001，并看看仓库ID 1、库位ID 2 的库存",
  "runId": "run-tool-chat-phase51-material-inventory",
  "plannerMode": "spring-ai",
  "requestedDomain": "mdm",
  "routeTags": ["mdm", "material"]
}
```

预期：第一步 `mdm.getMaterial` 成功，受控第二步 `inventory.getBalance` 成功，最终 answer 同时总结物料和库存信息。

### 29.7 面试讲解话术

企业 Agent 不能只靠 RAG，也不能只靠 Tool：

- RAG 解决企业知识、制度、字段口径和流程解释。
- Tool 解决实时业务数据查询和系统动作。
- Orchestrator 负责把 Tool 执行过程结构化记录下来，支持审计、观测和后续扩展。
- Phase 5.1 的 `/api/v1/ai/agent/chat` 把三者串成一个可演示闭环，适合 Java AI Agent 面试展示。

## 30. Phase 5.2 Prompt Context / Advisor 风格上下文治理

### 30.1 目标与边界

Phase 5.2 在 `/api/v1/ai/agent/chat` 的 RAG + Tool 组合问答基础上，新增统一 Prompt Context 层，将用户问题、RAG 片段、Tool display schema、Orchestrator step summary 和安全约束先结构化，再统一裁剪、脱敏、排序和渲染。

本阶段不改变 `/api/v1/ai/chat`、`/api/v1/ai/tool-calling/chat`、`/api/v1/ai/agent/chat` 的返回结构，不实现 MCP、Workflow、Multi-Agent、长任务编排或复杂多轮自动规划。

### 30.2 Context Provider 职责

当前 Provider 采用 Advisor 风格扩展点：

- `RagPromptContextProvider`：把 RAG retrieve 结果转换为 `rag_context` section。
- `ToolPromptContextProvider`：把 Tool execution 的 display schema 转换为 `tool_execution` section。
- `OrchestrationPromptContextProvider`：把 Orchestrator steps 的脱敏摘要转换为 `orchestration_steps` section。
- `UserMessagePromptContextProvider`：注入用户原始问题。
- `SystemInstructionsPromptContextProvider` / `SafetyPromptContextProvider`：注入回答策略和安全边界。

Provider 不直接调用模型，不输出完整原始业务对象、完整模型回包、敏感凭证或内部请求头。

### 30.3 Assembler / Renderer 职责

`AgentPromptContextAssembler` 负责收集所有 Provider 输出的 `AgentPromptSection`，按 `priority` 排序，按 `maxLength` 做字符级裁剪，并过滤 `sensitive=true` 或命中敏感关键词的 section。

`AgentPromptContextRenderer` 负责把治理后的 `AgentPromptContext` 渲染为最终模型输入，分区包含用户问题、知识库片段、工具执行结果、编排步骤摘要和安全约束。

### 30.4 与 Spring AI Advisor 的关系

当前实现没有强制切换到 Spring AI Advisor API，而是先沉淀 Advisor 风格的上下文链路：

- `RagPromptContextProvider` 类似 `QuestionAnswerAdvisor` 的知识上下文注入。
- `ToolPromptContextProvider` 类似工具结果上下文注入 Advisor。
- `OrchestrationPromptContextProvider` 类似步骤轨迹 Advisor。
- `AgentPromptContextAssembler` 类似统一 advisor chain 的上下文聚合器。

后续如果切换 Spring AI Advisor，可将各 Provider 包装为 Advisor，在调用 `ChatClient` 前统一注入 context，而不影响 RAG、Tool、Orchestrator 主流程。

### 30.5 Gateway 18080 验证

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
  "runId": "run-agent-phase52-001",
  "knowledgeBaseId": "kb-scm-demo",
  "topK": 3,
  "message": "按库存可用数量口径解释，并查物料 MAT-001 在仓库ID 1、库位ID 2 的库存",
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
    "runId": "run-agent-phase52-001",
    "intentType": "RAG_TOOL",
    "answer": "模型基于 Prompt Context 中的知识库片段、工具结果和编排步骤生成的中文回答",
    "rag": {
      "retrievedCount": 1,
      "chunks": []
    },
    "tool": {
      "selectedTool": "mdm.getMaterial",
      "execution": {
        "success": true,
        "displayTitle": "物料信息"
      }
    },
    "orchestration": {
      "enabled": true,
      "planMode": "MULTI_STEP_CONTROLLED",
      "stepCount": 2
    }
  }
}
```

日志中可观察 `contextSectionCount`、`includedSectionCount`、`truncatedSectionCount`，但不会打印完整模型输入、完整模型响应或大段业务数据。

## 31. Phase 6.1 Workflow 最小闭环

### 31.1 目标与边界

Phase 6.1 在 RAG + Tool + Prompt Context 的基础上，新增一个固定只读 Workflow 示例，用于展示企业级 Agent 如何表达业务流程编排。

本阶段不实现 MCP、Multi-Agent、复杂长任务编排、通用工作流引擎或写操作 Tool，不改变 `/api/v1/ai/chat`、`/api/v1/ai/tool-calling/chat`、`/api/v1/ai/agent/chat` 的返回结构。

### 31.2 Workflow 与 Orchestrator 的区别

- Orchestrator：偏 Agent Tool 执行轨迹，关注 planner、stepRef、runtime 保护、权限校验和审计。
- Workflow：偏业务流程定义，关注业务步骤、输入映射、条件、状态和最终业务结论。

Phase 6.1 的 Workflow 复用 `ToolInvocationService`，因此 Tool 权限、audit 和 runtime protection 仍然生效。

### 31.3 固定只读 Workflow

当前内置流程：

- `workflowCode`: `scm_stock_replenishment_advice`
- `workflowName`: 库存补货建议草案

步骤：

1. `query_material`：调用 `mdm.getMaterial` 查询物料。
2. `query_inventory_balance`：从第一步返回的 `id` 派生 `materialId`，并结合用户问题或 parameters 中的 `warehouseId`、`locationId` 调用 `inventory.getBalance`。
3. `generate_advice`：基于只读查询结果调用模型生成中文补货建议草案。

该流程只生成建议草案，不创建采购单、调拨单、补货单或其他写操作。

### 31.4 Gateway 18080 Workflow List 验证

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
  "data": [
    {
      "workflowCode": "scm_stock_replenishment_advice",
      "workflowName": "库存补货建议草案",
      "enabled": true,
      "steps": [
        { "stepCode": "query_material", "stepType": "TOOL", "toolName": "mdm.getMaterial" },
        { "stepCode": "query_inventory_balance", "stepType": "TOOL", "toolName": "inventory.getBalance" },
        { "stepCode": "generate_advice", "stepType": "SUMMARY" }
      ]
    }
  ]
}
```

### 31.5 Gateway 18080 Workflow Run 验证

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
  "runId": "run-workflow-phase61-001",
  "message": "帮我生成物料 MAT-001 在仓库ID 1、库位ID 2 的补货建议草案",
  "parameters": {
    "warehouseId": 1,
    "locationId": 2
  }
}
```

关键预期返回字段：

```json
{
  "success": true,
  "data": {
    "runId": "run-workflow-phase61-001",
    "workflowCode": "scm_stock_replenishment_advice",
    "workflowName": "库存补货建议草案",
    "status": "SUCCESS",
    "steps": [
      {
        "stepCode": "query_material",
        "status": "SUCCESS",
        "toolName": "mdm.getMaterial",
        "safeFields": {
          "materialId": 1001
        }
      },
      {
        "stepCode": "query_inventory_balance",
        "status": "SUCCESS",
        "toolName": "inventory.getBalance",
        "safeFields": {
          "availableQty": 12
        }
      },
      {
        "stepCode": "generate_advice",
        "status": "SUCCESS",
        "stepType": "SUMMARY"
      }
    ],
    "finalAnswer": "模型生成的中文补货建议草案",
    "latencyMs": 120
  }
}
```

### 31.6 Gateway 18080 Workflow Status 验证

```http
GET http://localhost:18080/api/v1/ai/workflows/runs/run-workflow-phase61-001
Authorization: Bearer <accessToken>
X-Tenant-Id: 1
X-User-Id: 10001
X-Username: admin
X-User-Roles: ROLE_ADMIN
```

预期：返回 run、steps、finalAnswer 和 latencyMs，但不返回完整 `rawData`、完整 prompt、完整模型响应、用户凭证或敏感 header。

### 31.7 参数不足场景

如果用户问题或 `parameters` 中缺少 `warehouseId` / `locationId`，`query_inventory_balance` 会进入 `SKIPPED`，`generate_advice` 也会跳过，`finalAnswer` 会说明缺少库存查询参数。

## 32. Phase 6.2 Workflow + RAG 组合增强

### 32.1 目标与边界

Phase 6.2 在固定只读 Workflow `scm_stock_replenishment_advice` 的 Summary 阶段接入 RAG 检索，让补货建议草案同时参考企业知识库规则和实时 Tool 查询结果。

本阶段仍不实现 MCP、Multi-Agent、复杂长任务编排、通用工作流引擎或写操作 Tool；Workflow Engine 抽象放到 Phase 6.3。Phase 6.2 不改变 `/api/v1/ai/chat`、`/api/v1/ai/tool-calling/chat`、`/api/v1/ai/agent/chat` 的返回结构。

### 32.2 Workflow + RAG + Tool 执行顺序

执行顺序保持固定且可讲解：

1. `query_material`：调用 `mdm.getMaterial`，得到物料安全摘要。
2. `query_inventory_balance`：从物料返回的 `id` 派生 `materialId`，结合 `warehouseId`、`locationId` 调用 `inventory.getBalance`。
3. `generate_advice`：如果请求传入 `knowledgeBaseId`，先检索 RAG；再把用户问题、物料 safeFields、库存 safeFields 和 RAG chunk 摘要交给模型生成中文补货建议草案。

Tool 结果仍是实时事实的最高优先级；RAG 只解释库存可用数量口径、锁定数量含义、物料状态、补货规则和人工确认边界。如果 RAG 未召回，模型不得编造知识库规则。

### 32.3 请求参数

Workflow run 请求支持可选 RAG 参数：

- `knowledgeBaseId`：知识库 ID；不传则不检索 RAG，保持 Phase 6.1 行为。
- `topK`：召回数量。
- `scoreThreshold`：相似度阈值。
- `filters`：检索过滤条件，透传给 RAG retrieve。

### 32.4 RAG 安全视图

RAG 结果不放在 Workflow 顶层字段中，而是放在 `generate_advice` 步骤的 `safeFields.rag` 中，避免破坏既有返回结构。

`safeFields.rag` 只返回脱敏概要：

- `knowledgeBaseId`
- `retrievedCount`
- `chunks[].documentId`
- `chunks[].chunkId`
- `chunks[].title`
- `chunks[].source`
- `chunks[].contentSnippet`
- `chunks[].score`

`contentSnippet` 会限制长度，不返回完整文档原文、完整 prompt、完整模型响应、用户 token、敏感 header 或完整业务 rawData。

### 32.5 Gateway 18080 Workflow + RAG Run 验证

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
  "runId": "run-workflow-phase62-001",
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
    "runId": "run-workflow-phase62-001",
    "workflowCode": "scm_stock_replenishment_advice",
    "status": "SUCCESS",
    "steps": [
      {
        "stepCode": "query_material",
        "status": "SUCCESS",
        "safeFields": {
          "materialCode": "MAT-001",
          "materialId": 1
        }
      },
      {
        "stepCode": "query_inventory_balance",
        "status": "SUCCESS",
        "safeFields": {
          "warehouseId": "2001",
          "locationId": "3001",
          "availableQty": "20.0",
          "lockedQty": "0.0"
        }
      },
      {
        "stepCode": "generate_advice",
        "status": "SUCCESS",
        "safeFields": {
          "rag": {
            "knowledgeBaseId": "kb-scm-demo",
            "retrievedCount": 1,
            "chunks": [
              {
                "title": "SCM/WMS 规则示例知识库",
                "contentSnippet": "库存可用数量通常等于现存数量减去锁定数量..."
              }
            ]
          }
        }
      }
    ],
    "finalAnswer": "模型基于知识库规则和实时库存数据生成的中文补货建议草案"
  }
}
```

### 32.6 Gateway 18080 Workflow Status 验证

```http
GET http://localhost:18080/api/v1/ai/workflows/runs/run-workflow-phase62-001
Authorization: Bearer <accessToken>
X-Tenant-Id: 1
X-User-Id: 10001
X-Username: admin
X-User-Roles: ROLE_ADMIN
```

预期：返回 Workflow run、steps、`generate_advice.safeFields.rag` 和 finalAnswer，但不返回完整 `rawData`、完整 prompt、完整模型响应、用户凭证或敏感 header。

### 32.7 与 /api/v1/ai/agent/chat 的区别

- `/api/v1/ai/agent/chat`：面向自由问答，先做意图路由，再组合 RAG、Tool 和 Orchestrator 上下文生成回答。
- `/api/v1/ai/workflows/{workflowCode}/run`：面向固定业务流程，步骤顺序明确，Summary 阶段可选接入 RAG 解释规则和口径。

Phase 6.2 的价值是把“业务流程 + 企业知识 + 实时业务数据”放到一个可演示闭环里，便于面试中说明 Workflow 和 Agent Chat 的职责边界。

## 33. Phase 6.3 Workflow Engine 最小抽象

### 33.1 目标与边界

Phase 6.3 将 Phase 6.1 / 6.2 中写死在 `AgentWorkflowService` 内的步骤执行逻辑拆成最小 Workflow Engine：

- `AgentWorkflowService`：对外门面，负责 list、run、status。
- `AgentWorkflowEngine`：按 `AgentWorkflowDefinition.steps` 顺序调度步骤。
- `AgentWorkflowStepExecutor`：每类步骤的执行扩展点。
- `AgentWorkflowExecutionContext`：在步骤之间传递安全摘要和必要中间变量。

本阶段不是完整 BPMN / Flowable / Activiti / Temporal 级平台，不实现并行、异步恢复、人工审批、复杂长任务或写操作 Tool。

### 33.2 Engine / Executor / Context 设计

`AgentWorkflowEngine` 接收 workflow definition、run request 和用户上下文，创建 run 后按 stepNo 顺序执行步骤。每个步骤由 `AgentWorkflowStepExecutorRegistry` 找到匹配 executor。

当前内置 executor：

- `ToolWorkflowStepExecutor`：执行 `TOOL` 步骤，负责参数解析、Tool 调用、display schema 构建、safeFields 生成。
- `SummaryWorkflowStepExecutor`：执行 `SUMMARY` 步骤，负责检查前置步骤、按需 RAG retrieve、构造 Summary prompt、调用模型并写入 finalAnswer。

`AgentWorkflowExecutionContext` 只保存安全摘要，例如：

- run / definition / request / AgentRequestContext
- completedSteps
- stepOutputs
- ragSummary
- finalAnswer

禁止在 context 中保存完整 `rawData`、完整 prompt、完整模型响应、token、authorization、cookie 或敏感 header。

### 33.3 参数解析与条件跳过

参数解析继续复用 `AgentWorkflowParameterResolver`：

- `materialCode`：来自 `parameters.materialCode` 或用户 message。
- `materialId`：来自 `query_material.safeFields.materialId`。
- `warehouseId` / `locationId`：来自 parameters 或用户 message。

参数不足时，当前步骤进入 `SKIPPED`：

- `inputResolved=false`
- `skipReason` 说明缺失参数
- 不调用真实 Tool
- 不写伪 audit

Summary 步骤依赖前置步骤成功；如果前置 Tool 未全部成功，Summary 进入 `SKIPPED`，finalAnswer 保留失败语义。

### 33.4 新增 Workflow 的扩展方式

后续新增固定业务 Workflow 时，不再复制 `WorkflowService2`。推荐路径：

1. 在 `AgentWorkflowDefinitionRegistry` 中新增 workflow definition 和 steps。
2. 如果 stepType 已存在，例如 `TOOL` 或 `SUMMARY`，优先复用现有 executor。
3. 如果需要新的步骤类型，再新增一个 `AgentWorkflowStepExecutor` 实现。
4. 参数解析逻辑放到 resolver 或 executor 内的白名单解析，不写进 service 门面。

这样可以在不引入大型工作流平台的前提下，展示企业级可扩展步骤执行框架。

### 33.5 Gateway 18080 Workflow Run 验证

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
  "runId": "run-workflow-phase63-001",
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
    "runId": "run-workflow-phase63-001",
    "workflowCode": "scm_stock_replenishment_advice",
    "status": "SUCCESS",
    "steps": [
      { "stepCode": "query_material", "stepType": "TOOL", "status": "SUCCESS" },
      { "stepCode": "query_inventory_balance", "stepType": "TOOL", "status": "SUCCESS" },
      {
        "stepCode": "generate_advice",
        "stepType": "SUMMARY",
        "status": "SUCCESS",
        "safeFields": {
          "rag": {
            "knowledgeBaseId": "kb-scm-demo",
            "retrievedCount": 1
          }
        }
      }
    ],
    "finalAnswer": "模型基于 Tool 安全摘要和 RAG 规则生成的中文补货建议草案"
  }
}
```

### 33.6 Gateway 18080 Workflow Status 验证

```http
GET http://localhost:18080/api/v1/ai/workflows/runs/run-workflow-phase63-001
Authorization: Bearer <accessToken>
X-Tenant-Id: 1
X-User-Id: 10001
X-Username: admin
X-User-Roles: ROLE_ADMIN
```

预期：返回字段与 Phase 6.2 兼容，仍包含 run、steps、safeFields 和 finalAnswer，不返回完整 rawData、完整 prompt、完整模型响应或敏感凭证。

### 33.7 与 Orchestrator 的职责区别

- Orchestrator：面向 Agent Tool 调用治理，关注模型规划、候选工具、stepRef、runtime protection、权限审计和受控二步执行。
- Workflow Engine：面向明确业务流程，关注 definition steps、executor、参数解析、条件跳过、状态记录和最终业务结论。

两者可以复用 ToolInvocationService，因此权限、audit、runtime protection 都能保持一致。

## 34. Phase 7.1 MCP-style Tool Adapter 最小演示

### 34.1 目标与边界

Phase 7.1 在 Tools + RAG + Orchestrator + Workflow + Prompt Context 已具备面试展示闭环的基础上，新增 HTTP 形式的 MCP-style Tool Adapter，用于说明企业内部已治理 Tool 如何以标准化方式暴露给外部 Agent、IDE 或客户端。

本阶段不是完整 MCP Server 协议栈，不接入真实外部 MCP Client，不新增写操作 Tool，也不重复实现一套 Tool 执行体系。MCP-style adapter 只做安全视图和入口适配，底层继续复用现有 ToolRegistry、ToolInvocationService、权限、audit 和 runtime protection。

### 34.2 MCP 在本项目中的定位

MCP 解决的是“外部 Agent 如何发现和调用企业内部工具”的标准化问题。本项目当前阶段采用 HTTP MCP-style adapter：

- Tool list：返回允许暴露的只读 Tool 定义、安全 input schema 和展示 schema。
- Tool invoke：按 MCP 风格调用 Tool，但内部仍走项目现有 Tool 调用链路。
- 安全治理：只暴露白名单只读 Tool，不返回内部 URL、API Key、token、敏感 header 或完整 rawData。

当前默认暴露：

- `mdm.getMaterial`
- `inventory.getBalance`

后续如果 `mdm.getWarehouse`、`sales.getOrder`、`purchase.getOrder` 等只读 Tool 需要暴露，应先补齐治理标签、权限语义和安全输出，再加入 MCP 暴露白名单。

### 34.3 MCP-style Tool List

接口：

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
    "tenantId": 1,
    "toolCount": 2,
    "tools": [
      {
        "name": "mdm.getMaterial",
        "description": "查询物料主数据",
        "inputSchema": {
          "type": "object",
          "properties": {
            "materialCode": {
              "type": "string"
            }
          }
        },
        "displaySchema": {
          "type": "display",
          "fields": [
            "displayTitle",
            "displaySummary",
            "displayFields",
            "displayItems"
          ]
        },
        "domain": "mdm",
        "category": "material",
        "routeTags": [
          "mdm",
          "material",
          "read"
        ],
        "readOnly": true,
        "requiredPermissions": [
          "ai.tool.read",
          "ai.tool.mdm.read"
        ]
      }
    ]
  }
}
```

返回内容不包含内部 HTTP URL、adapter 内部细节、API Key、token、敏感 header 或业务 rawData。

### 34.4 MCP-style Tool Invoke

接口：

```http
POST http://localhost:18080/api/v1/ai/mcp/tools/mdm.getMaterial/invoke
Authorization: Bearer <accessToken>
Content-Type: application/json
X-Tenant-Id: 1
X-User-Id: 10001
X-Username: admin
X-User-Roles: ROLE_ADMIN
```

请求体：

```json
{
  "runId": "run-mcp-phase71-material-001",
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
    "runId": "run-mcp-phase71-material-001",
    "toolName": "mdm.getMaterial",
    "success": true,
    "errorCode": null,
    "errorMessage": null,
    "display": {
      "displayTitle": "物料信息",
      "displaySummary": "已查询到物料 MAT-001（螺丝）",
      "displayFields": [
        {
          "key": "materialCode",
          "label": "物料编码",
          "value": "MAT-001"
        }
      ],
      "displayItems": []
    },
    "latencyMs": 20
  }
}
```

库存 Tool 调用示例：

```http
POST http://localhost:18080/api/v1/ai/mcp/tools/inventory.getBalance/invoke
Authorization: Bearer <accessToken>
Content-Type: application/json
X-Tenant-Id: 1
X-User-Id: 10001
X-Username: admin
X-User-Roles: ROLE_ADMIN
```

```json
{
  "runId": "run-mcp-phase71-inventory-001",
  "arguments": {
    "materialId": 1,
    "warehouseId": 2001,
    "locationId": 3001
  }
}
```

MCP invoke 响应只返回 display 安全视图和稳定错误语义，不返回完整 `rawData`。

### 34.5 如何复用现有 Tool 治理链路

MCP-style invoke 不直接调用业务 HTTP 服务，而是复用 `ToolInvocationService`：

1. `McpToolController` 接收外部 MCP-style 请求。
2. `McpToolExposureService` 校验 Tool 是否允许 MCP 暴露、是否只读、是否在白名单中。
3. 通过 `ToolInvocationService` 调用真实 Tool。
4. 原有 `ToolPermissionService`、Tool audit、runtime timeout / retry / circuit breaker 继续生效。
5. 执行结果通过 `ToolCallingDisplaySchemaBuilder` 转成 display 安全视图。

因此 MCP 只是外部暴露层，不是新的执行体系。权限失败、runtime 熔断、参数错误等仍保持原有 Tool 调用链路的真实失败语义。

### 34.6 MCP-style Adapter 与标准 MCP Server 的区别

当前阶段是 HTTP MCP-style adapter：

- 使用 REST endpoint 暴露 tool list / invoke。
- 便于通过 gateway 18080 演示和测试。
- 复用项目内已有认证、租户上下文和 Tool 治理链路。

标准 MCP Server 通常还会涉及 MCP transport、client session、标准协议消息和外部 IDE / Agent 客户端适配。后续如果要升级为标准 MCP Server，可以保留当前 `McpToolExposureService`，只替换或新增协议 transport 层。

### 34.7 后续方向

Phase 7.1 完成后，MCP 最小演示已经足够支撑面试讲解。下一步建议进入 Phase 8.1，做面试交付收敛：

- 整理一套端到端演示脚本。
- 补齐 README / 操作手册中的启动、导入知识库、调用接口步骤。
- 形成从 RAG、Tool、Agent Chat、Workflow 到 MCP-style adapter 的完整讲解路径。

## 35. Phase 8.1 面试交付收敛

### 35.1 目标与边界

Phase 8.1 不继续新增复杂业务功能，而是把当前 Tools + RAG + Orchestrator + Workflow + Prompt Context + MCP-style Adapter 能力收敛成可运行、可讲解、可演示的面试交付物。

本阶段不改变 `/api/v1/ai/chat`、`/api/v1/ai/tool-calling/chat`、`/api/v1/ai/agent/chat`、RAG、Workflow 或 MCP-style Adapter 的返回结构，不新增写操作 Tool，不实现完整 MCP Server、Multi-Agent 或复杂长任务编排。

### 35.2 交付文档

新增面试演示总览：

- `docs/architecture/ai-agent-interview-demo-guide.md`

该文档集中说明：

- 本地启动和推荐配置。
- gateway 18080 完整演示顺序。
- 基础 Chat、RAG、Tool Calling、Agent Chat、runtime status、Orchestrator、Workflow、MCP-style Adapter 的调用示例。
- 项目能力矩阵。
- 可直接用于面试表达的讲解稿。
- Phase 8.2 / Phase 9.1 后续建议。

### 35.3 完整演示顺序

推荐按以下顺序演示：

1. `POST /api/v1/ai/chat`：证明基础模型聊天和模型路由可用。
2. `POST /api/v1/ai/rag/import/docs`：导入 SCM/WMS 示例知识库。
3. `POST /api/v1/ai/rag/retrieve`：验证知识库可检索。
4. `POST /api/v1/ai/rag/chat`：验证 RAG 问答。
5. `POST /api/v1/ai/tool-calling/chat`：验证 Tool Calling 闭环。
6. `POST /api/v1/ai/agent/chat`：验证 RAG + Tool + Prompt Context 组合问答。
7. `GET /api/v1/ai/tools/runtime/status`：验证 runtime 保护状态。
8. `GET /api/v1/ai/tool-calling/orchestrations/{runId}`：验证 Orchestrator 步骤轨迹。
9. `GET /api/v1/ai/workflows`：查看 Workflow 定义。
10. `POST /api/v1/ai/workflows/{workflowCode}/run`：运行补货建议 Workflow。
11. `GET /api/v1/ai/workflows/runs/{runId}`：查看 Workflow 状态。
12. `GET /api/v1/ai/mcp/tools`：查看 MCP-style Tool list。
13. `POST /api/v1/ai/mcp/tools/{toolName}/invoke`：验证外部客户端风格 Tool 调用。

每个接口的 method、URL、headers、body 和关键预期字段见 `docs/architecture/ai-agent-interview-demo-guide.md`。

### 35.4 面试讲解主线

推荐讲解主线：

- 先讲项目背景：这是一个 SCM/WMS 企业后端项目，不是孤立 AI demo。
- 再讲 RAG：解决企业规则、口径、流程解释。
- 再讲 Tool Calling：解决实时业务事实查询。
- 再讲 Agent Chat：把 RAG 和 Tool 组合成“知识 + 实时数据”的回答闭环。
- 再讲 Prompt Context：用 Advisor 风格上下文治理替代散落拼 prompt。
- 再讲 Orchestrator：治理 Agent Tool 调用过程、步骤状态和受控二步执行。
- 再讲 Workflow：表达明确业务流程，复用 Tool 和 RAG 生成补货建议。
- 最后讲 MCP-style Adapter：外部 Agent 如何标准化发现和调用内部 Tool，同时不绕过权限、审计和 runtime protection。

### 35.5 当前可展示能力结论

当前项目已经足够用于 Java AI Agent 企业级开发面试展示：

- 能跑：gateway 18080 能覆盖 Chat / RAG / Tool / Agent / Workflow / MCP-style。
- 能讲：每条能力都有对应设计文档和职责边界。
- 能测：`scm-ai-agent` Maven test 不依赖真实模型、真实业务服务、MySQL、Milvus、Embedding API 或外部网络。
- 能扩展：后续可继续接标准 MCP Server、Multi-Agent 或长任务编排。

Phase 8.2 建议继续做演示材料增强，而不是继续堆低收益功能。

## 36. Phase 9.1 标准 MCP Server transport 最小演示

### 36.1 目标与边界

Phase 9.1 在 Phase 7.1 HTTP MCP-style Adapter 的基础上，新增一个标准 MCP Server transport 的最小可演示实现。当前实现采用 JSON-RPC HTTP endpoint，支持 MCP 核心语义中的 `tools/list` 和 `tools/call`，用于面试中说明企业内部只读 Tool 如何通过 MCP 协议风格暴露给外部 Agent / IDE / MCP Client。

本阶段不实现复杂 MCP Client / IDE 集成，不新增写操作 Tool，不改变已有 HTTP MCP-style Adapter，也不重写 Tool 执行体系。

### 36.2 配置项

默认配置：

```yaml
ai:
  agent:
    mcp:
      server:
        enabled: false
        transport: http
        endpoint: /api/v1/ai/mcp/server
        expose-tools: true
```

`local` profile 建议用于面试演示时开启：

```yaml
ai:
  agent:
    mcp:
      server:
        enabled: true
        transport: http
        endpoint: /api/v1/ai/mcp/server
        expose-tools: true
```

HTTP MCP-style Adapter `/api/v1/ai/mcp/tools` 和 `/api/v1/ai/mcp/tools/{toolName}/invoke` 不受该开关影响，保持可用。

### 36.3 MCP Server tools/list

接口：

```http
POST http://localhost:18080/api/v1/ai/mcp/server
Authorization: Bearer <accessToken>
Content-Type: application/json
X-Tenant-Id: 1
X-User-Id: 10001
X-Username: admin
X-User-Roles: ROLE_ADMIN
```

请求体：

```json
{
  "jsonrpc": "2.0",
  "id": "tools-list-1",
  "method": "tools/list",
  "params": {}
}
```

关键预期返回字段：

```json
{
  "jsonrpc": "2.0",
  "id": "tools-list-1",
  "result": {
    "tools": [
      {
        "name": "mdm.getMaterial",
        "description": "查询物料主数据",
        "inputSchema": {
          "type": "object"
        },
        "annotations": {
          "domain": "mdm",
          "category": "material",
          "routeTags": ["mdm", "material", "read"],
          "readOnly": true,
          "requiredPermissions": ["ai.tool.read", "ai.tool.mdm.read"]
        }
      },
      {
        "name": "inventory.getBalance",
        "annotations": {
          "domain": "inventory",
          "readOnly": true
        }
      }
    ]
  }
}
```

返回内容不包含内部 HTTP URL、API Key、token、敏感 header、adapter 内部实现细节或完整 `rawData`。

### 36.4 MCP Server tools/call：查询物料

接口：

```http
POST http://localhost:18080/api/v1/ai/mcp/server
Authorization: Bearer <accessToken>
Content-Type: application/json
X-Tenant-Id: 1
X-User-Id: 10001
X-Username: admin
X-User-Roles: ROLE_ADMIN
```

请求体：

```json
{
  "jsonrpc": "2.0",
  "id": "tool-call-material-1",
  "method": "tools/call",
  "params": {
    "name": "mdm.getMaterial",
    "runId": "run-mcp-server-phase91-material-001",
    "arguments": {
      "materialCode": "MAT-001"
    }
  }
}
```

关键预期返回字段：

```json
{
  "jsonrpc": "2.0",
  "id": "tool-call-material-1",
  "result": {
    "content": [
      {
        "type": "text",
        "text": "已查询到物料 MAT-001（螺丝）"
      }
    ],
    "structuredContent": {
      "runId": "run-mcp-server-phase91-material-001",
      "toolName": "mdm.getMaterial",
      "success": true,
      "errorCode": null,
      "errorMessage": null,
      "displayTitle": "物料信息",
      "displaySummary": "已查询到物料 MAT-001（螺丝）",
      "displayFields": [],
      "displayItems": [],
      "latencyMs": 20
    },
    "isError": false
  }
}
```

### 36.5 MCP Server tools/call：查询库存

请求体：

```json
{
  "jsonrpc": "2.0",
  "id": "tool-call-inventory-1",
  "method": "tools/call",
  "params": {
    "name": "inventory.getBalance",
    "runId": "run-mcp-server-phase91-inventory-001",
    "arguments": {
      "materialId": 1,
      "warehouseId": 2001,
      "locationId": 3001
    }
  }
}
```

关键预期返回字段：

```json
{
  "jsonrpc": "2.0",
  "id": "tool-call-inventory-1",
  "result": {
    "content": [
      {
        "type": "text",
        "text": "已查询到库存余额，物料 1 可用 20.0"
      }
    ],
    "structuredContent": {
      "toolName": "inventory.getBalance",
      "success": true,
      "displayTitle": "库存余额",
      "displaySummary": "已查询到库存余额，物料 1 可用 20.0"
    },
    "isError": false
  }
}
```

### 36.6 错误语义

Tool 不存在、未暴露、非只读、权限失败、参数错误、runtime 熔断等场景会返回 JSON-RPC error，例如：

```json
{
  "jsonrpc": "2.0",
  "id": "tool-call-unknown-1",
  "error": {
    "code": -32010,
    "message": "Tool not exposed to MCP",
    "data": {
      "toolName": "unknown.tool",
      "success": false,
      "errorCode": "404"
    }
  }
}
```

错误响应保留项目内部真实失败语义，但不返回完整 `rawData`、完整 arguments、prompt、模型响应、token 或敏感 header。

### 36.7 复用现有 Tool 治理链路

标准 MCP Server transport 调用链路：

1. `McpServerController` 接收 JSON-RPC 请求并构造 `AgentRequestContext`。
2. `McpServerTransportService` 解析 `tools/list` 或 `tools/call`。
3. `tools/list` 复用 `McpToolExposureService.listTools(...)`。
4. `tools/call` 复用 `McpToolExposureService.invoke(...)`。
5. `McpToolExposureService` 继续调用 `ToolInvocationService`。
6. `ToolPermissionService`、Tool audit、runtime timeout / retry / circuit breaker、display schema 继续生效。

因此 MCP Server 只是协议 transport 层，不是新的 Tool 执行体系。

## 37. Phase 10.1 Multi-Agent 基础模型与 Coordinator 骨架

### 37.1 目标与边界

Phase 10.1 开始进入 Multi-Agent，但只做企业级可控多 Agent 协作的基础模型和最小骨架。本阶段不引入 AutoGen、CrewAI、LangGraph，不实现复杂多轮自治，不让多个 Agent 无约束互相聊天。

当前阶段新增：

- `MultiAgentRun`
- `MultiAgentStep`
- `MultiAgentMessage`
- Coordinator / Planner / Knowledge / Tool / Reviewer 角色定义
- `POST /api/v1/ai/multi-agent/chat`
- `GET /api/v1/ai/multi-agent/runs/{runId}`
- `GET /api/v1/ai/multi-agent/runs?limit=20`

Phase 10.1 不执行真实 RAG、Tool、Workflow 或 MCP 调用，只记录受控单轮协作骨架。

### 37.2 配置

```yaml
ai:
  agent:
    multi-agent:
      enabled: false
      max-rounds: 3
      max-agents: 5
      max-tool-calls: 3
      record-messages: true
      max-records: 100
```

local profile 可开启：

```yaml
ai:
  agent:
    multi-agent:
      enabled: true
      max-rounds: 3
      max-agents: 5
      max-tool-calls: 3
      record-messages: true
      max-records: 100
```

`max-rounds`、`max-agents`、`max-tool-calls` 在 Phase 10.1 主要用于配置绑定、日志记录和后续扩展预留。

### 37.3 Multi-Agent Chat

```http
POST http://localhost:18080/api/v1/ai/multi-agent/chat
Authorization: Bearer <accessToken>
Content-Type: application/json
X-Tenant-Id: 1
X-User-Id: 10001
X-Username: admin
X-User-Roles: ROLE_ADMIN
```

请求体：

```json
{
  "runId": "run-multi-agent-phase101-001",
  "message": "按库存可用数量口径解释，并查物料 MAT-001 的库存",
  "mode": "controlled-demo"
}
```

关键预期返回字段：

```json
{
  "success": true,
  "data": {
    "runId": "run-multi-agent-phase101-001",
    "status": "SUCCESS",
    "answer": "已创建 Multi-Agent 协作运行骨架...",
    "agents": [
      {
        "agentName": "CoordinatorAgent",
        "role": "COORDINATOR",
        "status": "SUCCESS"
      },
      {
        "agentName": "PlannerAgent",
        "role": "PLANNER",
        "status": "SUCCESS"
      }
    ],
    "steps": [
      {
        "stepNo": 1,
        "agentName": "CoordinatorAgent",
        "actionType": "NOOP",
        "status": "SUCCESS",
        "outputSummary": "已接收用户任务，Phase 10.1 仅记录受控协作骨架"
      },
      {
        "stepNo": 2,
        "agentName": "PlannerAgent",
        "actionType": "PLAN",
        "status": "SUCCESS"
      }
    ],
    "latencyMs": 20
  }
}
```

### 37.4 Multi-Agent Run Status

```http
GET http://localhost:18080/api/v1/ai/multi-agent/runs/run-multi-agent-phase101-001
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
    "runId": "run-multi-agent-phase101-001",
    "status": "SUCCESS",
    "agents": [],
    "steps": [],
    "messages": []
  }
}
```

状态接口不返回完整 prompt、完整模型响应、完整 rawData、token、authorization、cookie 或敏感 header。
