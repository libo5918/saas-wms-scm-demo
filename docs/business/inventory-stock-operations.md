# 库存接口补充文档

本文档补充 `scm-inventory` 当前已提供的库存操作接口，重点覆盖本轮新增的库存调整、库存移库能力，并统一说明请求头、请求体和返回结构。

## 基本约定

- 服务模块：`scm-inventory`
- 接口前缀：`/api/v1/inventory`
- 请求头：`X-Tenant-Id: 1`
- 返回包装：`Result<T>`

成功返回示例：

```json
{
  "code": "0",
  "message": "success",
  "data": {}
}
```

## 已支持接口

### 1. 库存入库

- 方法：`POST /api/v1/inventory/stock-ins`
- 用途：按业务单执行库存入库，增加现存和可用库存，并生成库存流水

请求示例：

```json
{
  "bizType": "PURCHASE_RECEIPT",
  "bizNo": "RCV-1001",
  "operatorId": 1,
  "items": [
    {
      "materialId": 1,
      "warehouseId": 2001,
      "locationId": 3001,
      "quantity": 10
    }
  ]
}
```

### 2. 库存调整

- 方法：`POST /api/v1/inventory/adjustments`
- 用途：处理盘盈、盘亏或人工修正
- 说明：`adjustType` 目前支持 `INCREASE`、`DECREASE`

请求字段：

- `bizType`：业务类型，示例 `MANUAL_ADJUST`
- `bizNo`：业务单号，示例 `ADJ-001`
- `adjustType`：调整类型
- `operatorId`：操作人 ID
- `items`：调整明细

明细字段：

- `materialId`：物料 ID
- `warehouseId`：仓库 ID
- `locationId`：库位 ID
- `quantity`：调整数量，必须大于 `0`

请求示例：

```json
{
  "bizType": "MANUAL_ADJUST",
  "bizNo": "ADJ-001",
  "adjustType": "INCREASE",
  "operatorId": 1,
  "items": [
    {
      "materialId": 1,
      "warehouseId": 2001,
      "locationId": 3001,
      "quantity": 5
    }
  ]
}
```

响应 `data` 示例：

```json
{
  "bizType": "MANUAL_ADJUST",
  "bizNo": "ADJ-001",
  "adjustType": "INCREASE",
  "lines": [
    {
      "txnNo": "TXN-202604150001",
      "materialId": 1,
      "warehouseId": 2001,
      "locationId": 3001,
      "quantity": 5,
      "beforeQty": 10,
      "afterQty": 15
    }
  ]
}
```

业务规则：

- `INCREASE` 会增加现存和可用库存
- `DECREASE` 会扣减现存和可用库存，不允许扣成负数
- 每条明细都会生成一条库存流水

### 3. 库存锁定

- 方法：`POST /api/v1/inventory/locks`
- 用途：按业务单锁定可用库存

### 4. 普通出库

- 方法：`POST /api/v1/inventory/stock-outs`
- 用途：直接扣减现存和可用库存

### 5. 锁定库存出库

- 方法：`POST /api/v1/inventory/locked-stock-outs`
- 用途：消耗已锁定库存完成出库

### 6. 库存移库

- 方法：`POST /api/v1/inventory/transfers`
- 用途：将库存从源仓位移动到目标仓位

请求字段：

- `bizType`：业务类型，示例 `INVENTORY_TRANSFER`
- `bizNo`：业务单号，示例 `TRF-001`
- `operatorId`：操作人 ID
- `items`：移库明细

明细字段：

- `materialId`：物料 ID
- `fromWarehouseId`：源仓库 ID
- `fromLocationId`：源库位 ID
- `toWarehouseId`：目标仓库 ID
- `toLocationId`：目标库位 ID
- `quantity`：移库数量，必须大于 `0`

请求示例：

```json
{
  "bizType": "INVENTORY_TRANSFER",
  "bizNo": "TRF-001",
  "operatorId": 1,
  "items": [
    {
      "materialId": 1,
      "fromWarehouseId": 2001,
      "fromLocationId": 3001,
      "toWarehouseId": 2001,
      "toLocationId": 3002,
      "quantity": 3
    }
  ]
}
```

响应 `data` 示例：

```json
{
  "bizType": "INVENTORY_TRANSFER",
  "bizNo": "TRF-001",
  "lines": [
    {
      "moveOutTxnNo": "TXN-202604150010",
      "moveInTxnNo": "TXN-202604150011",
      "materialId": 1,
      "fromWarehouseId": 2001,
      "fromLocationId": 3001,
      "toWarehouseId": 2001,
      "toLocationId": 3002,
      "quantity": 3,
      "fromBeforeQty": 10,
      "fromAfterQty": 7,
      "toBeforeQty": 2,
      "toAfterQty": 5
    }
  ]
}
```

业务规则：

- 移库会生成两条流水：移出、移入
- 源库位库存不足时移库失败
- 目标库位不存在余额记录时会自动建立库存维度记录

### 7. 库存解锁

- 方法：`POST /api/v1/inventory/unlocks`
- 用途：释放已锁定库存

### 8. 查询库存余额

- 方法：`GET /api/v1/inventory/balances`
- 用途：查询指定物料在仓库/库位维度下的库存余额

请求参数：

- `materialId`
- `warehouseId`
- `locationId`

示例：

```text
GET /api/v1/inventory/balances?materialId=1&warehouseId=2001&locationId=3001
Header: X-Tenant-Id: 1
```

### 9. 查询库存流水

- 方法：`GET /api/v1/inventory/txn-records`
- 用途：按业务类型和业务单号查询库存流水

请求参数：

- `bizType`
- `bizNo`

示例：

```text
GET /api/v1/inventory/txn-records?bizType=MANUAL_ADJUST&bizNo=ADJ-001
Header: X-Tenant-Id: 1
```

## 联调建议

推荐联调顺序：

1. 先执行入库，制造初始可用库存
2. 执行调整或移库，确认库存余额变化
3. 再查询 `/balances` 和 `/txn-records` 校验结果

推荐准备的演示数据：

- 物料：`materialId=1`
- 仓库：`warehouseId=2001`
- 库位：`locationId=3001`、`3002`
