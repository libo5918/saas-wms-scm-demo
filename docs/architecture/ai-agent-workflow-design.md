# AI Agent Workflow 最小闭环设计

## 1. 定位

Workflow 表达企业业务流程，例如“查询物料 -> 查询库存 -> 生成补货建议草案”。它关注业务步骤、输入映射、步骤状态和最终业务结论。

Orchestrator 表达 Agent 工具执行过程，例如 Tool plan、stepRef、runtime 保护、权限校验和审计轨迹。它关注模型规划后的工具调用治理。

Phase 6.1 的 Workflow 可以复用 ToolInvocationService，因此权限、审计和 runtime 保护仍然生效，但 Workflow 不等同于 Orchestrator。

## 2. 当前固定流程

`scm_stock_replenishment_advice` 是一个只读演示流程：

1. `query_material`：调用 `mdm.getMaterial` 查询物料。
2. `query_inventory_balance`：调用 `inventory.getBalance` 查询库存余额。
3. `generate_advice`：调用模型生成中文补货建议草案。

该流程不会创建采购单、调拨单、补货单，也不会执行任何写操作。

## 3. 参数来源

- `materialCode`：优先来自请求 `parameters.materialCode`，其次从用户 `message` 中提取，例如 `MAT-001`。
- `materialId`：来自第一步 `mdm.getMaterial` 返回结果中的 `id`，作为第二步 `inventory.getBalance.materialId`。
- `warehouseId` / `locationId`：优先来自请求 `parameters`，其次从用户 `message` 中提取。

参数不足时，对应步骤进入 `SKIPPED` 或 `FAILED`，最终回答说明缺少参数。

## 4. 安全边界

Workflow status 只返回脱敏概要：

- stepCode
- stepName
- stepNo
- stepType
- status
- toolName
- inputResolved
- skipReason
- errorCode / errorMessage
- displayTitle / displaySummary
- safeFields
- latencyMs

接口不返回完整 `rawData`、完整 prompt、完整模型响应、用户凭证、敏感 header 或内部 HTTP header。

## 5. 后续演进

Phase 6.1 不实现通用工作流引擎。后续可以逐步增加：

- Workflow definition 配置化。
- 条件步骤。
- 人工确认节点。
- 长任务和异步状态。
- 与 MCP / 外部编排平台的集成。
