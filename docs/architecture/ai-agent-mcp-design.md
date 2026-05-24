# AI Agent MCP-style Tool Adapter 设计

## 1. 定位

Phase 7.1 的目标是提供一个面试展示级 MCP-style Tool Adapter，说明企业内部已治理 Tool 如何被外部 Agent、IDE 或客户端发现和调用。

当前实现采用 HTTP 形式的 MCP 风格接口，不实现完整 MCP Server 协议栈，不接入真实外部 MCP Client。这样可以快速通过 gateway 18080 演示，同时保留后续升级为标准 MCP Server 的空间。

## 2. 架构关系

```mermaid
flowchart LR
    A["外部 Agent / IDE / Client"] --> B["MCP-style HTTP Adapter"]
    B --> C["McpToolExposureService"]
    C --> D["ToolRegistry"]
    C --> E["ToolInvocationService"]
    E --> F["ToolPermissionService"]
    E --> G["ToolRuntimeProtectionService"]
    E --> H["Tool Audit"]
    E --> I["真实只读业务 Tool"]
    C --> J["Display Schema Builder"]
```

MCP-style adapter 只负责安全暴露和协议适配：

- list：从 `ToolRegistry` 读取 ToolDefinition，过滤允许 MCP 暴露的只读 Tool。
- invoke：校验 Tool 是否允许暴露后，调用 `ToolInvocationService`。
- output：将 Tool 执行结果转换为 display 安全视图。

## 3. 暴露边界

默认只暴露安全只读 Tool：

- `mdm.getMaterial`
- `inventory.getBalance`

暴露条件：

- Tool 存在于 `ToolRegistry`。
- Tool 为 `readOnly=true`。
- Tool 在 MCP 暴露白名单中。
- Tool 输出可以转换为 display schema。

禁止暴露：

- 写操作 Tool。
- 内部 HTTP URL。
- API Key、token、authorization、cookie。
- 内部 HTTP header。
- 完整 rawData。
- 完整 prompt 或完整模型响应。

## 4. 接口设计

### 4.1 Tool List

```http
GET /api/v1/ai/mcp/tools
```

返回 MCP 风格工具定义视图：

- name
- description
- inputSchema
- displaySchema
- domain
- category
- routeTags
- readOnly
- requiredPermissions

### 4.2 Tool Invoke

```http
POST /api/v1/ai/mcp/tools/{toolName}/invoke
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

返回 MCP 风格工具调用结果视图：

- runId
- toolName
- success
- errorCode
- errorMessage
- display.displayTitle
- display.displaySummary
- display.displayFields
- display.displayItems
- latencyMs

响应不返回完整 `rawData`。

## 5. 与标准 MCP Server 的关系

当前实现是 HTTP MCP-style adapter：

- 便于在现有 Spring Boot / Gateway / 租户上下文中演示。
- 复用现有认证、权限、审计、runtime protection。
- 不引入复杂 MCP transport 和外部 client session。

标准 MCP Server 后续可以在协议层替换 HTTP adapter：

- 保留 `McpToolExposureService`。
- 保留 `ToolRegistry` 和 `ToolInvocationService`。
- 新增 MCP transport，把标准 MCP list / call tool 请求转换为当前内部调用模型。

因此 Phase 7.1 的价值不是完整协议覆盖，而是证明企业 Tool 已具备标准化暴露所需的治理基础。

## 6. 面试讲解要点

可以这样讲：

“MCP 的核心价值是让外部 Agent 通过标准方式发现和调用工具。但企业项目不能让外部客户端绕过内部治理，所以我没有重新实现一套工具调用，而是在现有 ToolInvocationService 外面做 MCP-style adapter。外部看到的是 tool list 和 invoke，内部仍然走权限校验、租户上下文、审计、超时重试、熔断和 display schema。后续接标准 MCP Server 时，只需要替换 transport 层，Tool 治理链路不用重写。”
