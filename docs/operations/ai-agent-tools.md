# AI Agent Phase 4 Tools 基础能力说明

## 1. 阶段目标

Phase 4 的目标是把 SCM/WMS 现有业务能力逐步封装为 Agent 可以调用的 Tool，为后续 Spring AI Tool Calling、MCP、Workflow、Multi-Agent 和 Orchestrator 做准备。

本阶段只实现 Tools 基础底座，不实现 MCP、Workflow、多 Agent 和长任务编排。

## 2. 当前边界

当前阶段优先做只读 Tool，避免 Agent 误操作真实业务数据。

默认实现使用 mock/local adapter：

- 不依赖真实业务服务
- 不依赖 Nacos
- 不依赖 MySQL、Milvus、Embedding API 或外部网络
- 单元测试可以稳定运行

后续接真实业务服务时，再按工具逐个替换 adapter。

## 3. Tool 抽象设计

当前 `scm-ai-agent` 中的 Tool 基础抽象包括：

- `ToolDefinition`：描述工具名称、领域、说明、是否只读和参数定义。
- `ToolRequest`：工具执行请求，由系统统一补齐 `tenantId`、`userId`、`runId` 和参数。
- `ToolResponse`：工具执行响应，统一返回成功标识、数据、错误码、错误信息和耗时。
- `ToolExecutor`：每个具体工具的执行器接口。
- `ToolRegistry`：启动时收集所有 `ToolExecutor`，按 `toolName` 建立索引。
- `ToolInvocationService`：统一处理工具查找、执行、异常包装和调用日志。

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

## 4. 当前工具清单

当前已提供 5 个只读 mock 工具：

| toolName | 领域 | 说明 |
| --- | --- | --- |
| `inventory.getBalance` | inventory | 查询库存余额 |
| `mdm.getMaterial` | mdm | 查询物料信息 |
| `sales.getOrder` | sales | 查询销售订单 |
| `purchase.getOrder` | purchase | 查询采购订单 |
| `mdm.getWarehouse` | mdm | 查询仓库信息 |

## 5. 调用流程

Tool 调用流程如下：

```text
Client
  -> Gateway 18080
  -> JWT 鉴权
  -> 网关透传租户和用户上下文
  -> scm-ai-agent AiToolController
  -> ToolInvocationService
  -> ToolRegistry 查找 ToolExecutor
  -> mock/local ToolExecutor
  -> ToolResponse
```

客户端请求体中不允许自行覆盖 `tenantId` 和 `userId`。

`tenantId`、`userId`、用户名和角色必须来自网关认证后的透传上下文。

## 6. 为什么先做只读 Tool

真实企业场景中，Agent Tool 一旦接入业务系统，就具备查询或修改业务数据的能力。

本阶段先做只读 Tool，原因是：

- 降低误操作风险
- 先稳定工具协议和调用链路
- 便于后续接入 Spring AI Tool Calling
- 便于后续 MCP 暴露工具
- 写操作工具后续必须补充确认机制、权限校验、审计和幂等设计

## 7. 后续如何接真实 SCM 服务

后续每个 mock/local adapter 可以按以下方式升级：

- REST client：直接调用已有业务服务 HTTP 接口。
- OpenFeign：基于 Spring Cloud 服务发现调用业务服务。
- WebClient：适合响应式或网关转发场景。
- Gateway route：统一走 `scm-gateway`，复用网关鉴权、限流和审计策略。

真实调用时必须继续保留：

- 租户隔离
- 用户上下文
- 权限校验
- 调用日志
- 超时控制
- 异常包装
- 敏感信息脱敏

## 8. 后续如何接 Spring AI Tool Calling / MCP / Workflow

当前 Tool 抽象是后续能力的公共底座：

- Spring AI Tool Calling：可以把 `ToolDefinition` 转换为模型可识别的 tool schema。
- MCP：可以把 `ToolExecutor` 暴露为 MCP tool，供 Dify、Cursor、Claude Code 等外部 Agent 客户端调用。
- Workflow：可以在工作流节点中调用 `ToolInvocationService`。
- Multi-Agent：不同领域 Agent 可以绑定不同 Tool 集合。
- Orchestrator：根据任务规划结果选择合适 Tool 执行。

## 9. IDEA 本地验证前提

本阶段默认 mock/local adapter，不需要额外配置真实 SCM 服务。

建议仍通过 gateway 访问：

```text
http://localhost:18080
```

需要启动：

- `scm-auth`
- `scm-gateway`
- `scm-ai-agent`

## 10. Gateway 接口示例

### 10.1 登录获取 Token

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

从返回结果中复制：

```text
data.accessToken
```

### 10.2 查询 Tool 列表

```http
GET http://localhost:18080/api/v1/ai/tools
Authorization: Bearer <accessToken>
```

关键预期返回字段：

```json
{
  "success": true,
  "code": "200",
  "data": {
    "tenantId": 1,
    "toolCount": 5,
    "tools": [
      {
        "name": "inventory.getBalance",
        "domain": "inventory",
        "readOnly": true
      }
    ]
  }
}
```

### 10.3 调用库存余额 Tool

```http
POST http://localhost:18080/api/v1/ai/tools/invoke
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "toolName": "inventory.getBalance",
  "runId": "run-tools-demo-001",
  "parameters": {
    "materialId": 1001,
    "warehouseId": 1
  }
}
```

关键预期返回字段：

```json
{
  "success": true,
  "code": "200",
  "data": {
    "success": true,
    "toolName": "inventory.getBalance",
    "runId": "run-tools-demo-001",
    "data": {
      "tenantId": 1,
      "materialId": 1001,
      "warehouseId": 1,
      "availableQty": 128,
      "lockedQty": 12,
      "adapterMode": "mock"
    }
  }
}
```

### 10.4 调用物料信息 Tool

```http
POST http://localhost:18080/api/v1/ai/tools/invoke
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "toolName": "mdm.getMaterial",
  "runId": "run-tools-demo-002",
  "parameters": {
    "materialId": 1001,
    "materialCode": "MAT-1001"
  }
}
```

### 10.5 工具不存在时的返回

```http
POST http://localhost:18080/api/v1/ai/tools/invoke
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "toolName": "unknown.tool",
  "runId": "run-tools-demo-404",
  "parameters": {}
}
```

关键预期返回字段：

```json
{
  "success": true,
  "code": "200",
  "data": {
    "success": false,
    "toolName": "unknown.tool",
    "errorCode": "404",
    "errorMessage": "Tool not found: unknown.tool"
  }
}
```

这里外层 `success=true` 表示 HTTP 请求被 AI Agent 服务正常处理；内层 `data.success=false` 表示工具业务调用失败。

## 11. 当前阶段测试方式

执行：

```bash
mvn -pl scm-ai-agent -am test
```

测试覆盖：

- `ToolRegistry` 注册和查询工具
- mock 工具正常执行
- 不存在的工具返回明确错误
- Tool API 可以携带租户和用户上下文
- 缺少用户上下文时返回明确错误

## 12. 当前阶段不做的事情

本阶段不实现：

- 真实 SCM 服务调用
- Spring AI 自动 Tool Calling
- MCP Server
- Workflow
- Multi-Agent
- 长任务编排
- 写操作 Tool

## 13. Phase 4.1：真实 SCM/WMS 服务调用骨架

Phase 4.1 在 Phase 4 的 Tool 协议基础上，新增 Tool Adapter 切换能力。

当前目标不是一次性把所有 Tool 都接到真实业务服务，而是先把调用骨架打稳，优先完成：

- `inventory.getBalance`
- `mdm.getMaterial`

### 13.1 Adapter 模式

配置项：

```yaml
ai:
  agent:
    tools:
      adapter-mode: mock
```

可选值：

| adapter-mode | 说明 |
| --- | --- |
| `mock` | 默认模式，使用本地 mock/local adapter，不依赖真实业务服务 |
| `http` | 通过 HTTP 调用真实 SCM/WMS 服务 |

后续可继续扩展：

- `feign`
- `webclient`
- `gateway`

### 13.2 Client 抽象

Phase 4.1 新增业务服务 Client 抽象：

- `InventoryToolClient`
- `MdmToolClient`
- `MockInventoryToolClient`
- `MockMdmToolClient`
- `HttpInventoryToolClient`
- `HttpMdmToolClient`

ToolExecutor 不再直接写死 mock 数据，而是委托给对应 ToolClient。

这样后续切换真实服务时，不需要改 Tool API 和 ToolRegistry。

### 13.3 HTTP Adapter 真实接口路径

当前根据现有业务 Controller 适配：

| Tool | 真实服务 | HTTP 路径 |
| --- | --- | --- |
| `inventory.getBalance` | `scm-inventory` | `GET /api/v1/inventory/balances?materialId=&warehouseId=&locationId=` |
| `mdm.getMaterial` | `scm-mdm` | `GET /api/v1/materials/{materialId}` |

当前项目默认端口：

| 服务 | 默认端口 |
| --- | --- |
| `scm-mdm` | `18082` |
| `scm-inventory` | `18084` |

### 13.4 HTTP Adapter 配置

`application.yml` 默认配置：

```yaml
ai:
  agent:
    tools:
      adapter-mode: ${AI_AGENT_TOOLS_ADAPTER_MODE:mock}
      http:
        inventory-base-url: ${INVENTORY_SERVICE_BASE_URL:http://localhost:18084}
        mdm-base-url: ${MDM_SERVICE_BASE_URL:http://localhost:18082}
        connect-timeout-ms: ${AI_AGENT_TOOLS_HTTP_CONNECT_TIMEOUT_MS:3000}
        read-timeout-ms: ${AI_AGENT_TOOLS_HTTP_READ_TIMEOUT_MS:5000}
```

IDEA 本地联调真实业务服务时，可以在 `application-local.yml` 中临时改为：

```yaml
ai:
  agent:
    tools:
      adapter-mode: http
      http:
        inventory-base-url: http://localhost:18084
        mdm-base-url: http://localhost:18082
```

也可以使用环境变量：

```text
AI_AGENT_TOOLS_ADAPTER_MODE=http
INVENTORY_SERVICE_BASE_URL=http://localhost:18084
MDM_SERVICE_BASE_URL=http://localhost:18082
```

### 13.5 身份上下文透传

HTTP adapter 调用下游服务时会透传：

- `X-Tenant-Id`
- `X-User-Id`
- `X-User-Name`
- `X-User-Roles`
- `X-Agent-Run-Id`

客户端请求体中仍然不允许自行覆盖租户和用户身份。

### 13.6 Gateway 验证方式

默认 mock 模式下，仍然通过 gateway 18080 调用：

```http
POST http://localhost:18080/api/v1/ai/tools/invoke
Authorization: Bearer <accessToken>
Content-Type: application/json
```

库存余额 Tool：

```json
{
  "toolName": "inventory.getBalance",
  "runId": "run-tools-http-ready-001",
  "parameters": {
    "materialId": 1001,
    "warehouseId": 1,
    "locationId": 1
  }
}
```

mock 模式关键返回：

```json
{
  "success": true,
  "data": {
    "success": true,
    "toolName": "inventory.getBalance",
    "data": {
      "adapterMode": "mock"
    }
  }
}
```

切换到 `adapter-mode=http` 且启动 `scm-inventory` 后，关键返回中的 `adapterMode` 应为：

```json
{
  "adapterMode": "http"
}
```

物料信息 Tool：

```json
{
  "toolName": "mdm.getMaterial",
  "runId": "run-tools-http-ready-002",
  "parameters": {
    "materialId": 1001
  }
}
```

### 13.7 后续扩展方向

Phase 4.2 可以继续做：

- 为 `sales.getOrder` 增加 `SalesToolClient`
- 为 `purchase.getOrder` 增加 `PurchaseToolClient`
- 为 `mdm.getWarehouse` 增加仓库 HTTP adapter
- 增加 Tool 调用审计表
- 将 ToolDefinition 转换为 Spring AI Tool Calling schema
- 为 MCP 暴露复用同一套 ToolExecutor
