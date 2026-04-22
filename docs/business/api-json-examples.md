# 全模块接口 JSON 示例

## 通用约定

- Header：`X-Tenant-Id: 1`
- 成功响应统一结构：

```json
{
  "code": "0",
  "message": "success",
  "data": {}
}
```

---

## 1. MDM 模块（`scm-mdm`）

### 1.1 物料

- `POST /api/v1/materials`

```json
{
  "materialCode": "MAT-001",
  "materialName": "工业齿轮",
  "materialSpec": "M8",
  "unit": "PCS",
  "materialType": "RAW",
  "status": 1
}
```

- `PUT /api/v1/materials/{id}`

```json
{
  "materialName": "工业齿轮-升级版",
  "materialSpec": "M8-PLUS",
  "unit": "PCS",
  "materialType": "RAW",
  "status": 1
}
```

- `GET /api/v1/materials/{id}`、`GET /api/v1/materials`、`DELETE /api/v1/materials/{id}`

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "id": 1,
    "materialCode": "MAT-001",
    "materialName": "工业齿轮",
    "materialSpec": "M8",
    "unit": "PCS",
    "materialType": "RAW",
    "status": 1
  }
}
```

### 1.2 供应商（新增）

- `POST /api/v1/suppliers`

```json
{
  "supplierCode": "SUP-001",
  "supplierName": "华东供应商A",
  "contactName": "张三",
  "contactPhone": "13800000001",
  "status": 1
}
```

- `PUT /api/v1/suppliers/{id}`

```json
{
  "supplierName": "华东供应商A-更新",
  "contactName": "李四",
  "contactPhone": "13800000002",
  "status": 1
}
```

- `GET /api/v1/suppliers/{id}`、`GET /api/v1/suppliers`、`DELETE /api/v1/suppliers/{id}`

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "id": 1,
    "supplierCode": "SUP-001",
    "supplierName": "华东供应商A",
    "contactName": "张三",
    "contactPhone": "13800000001",
    "status": 1
  }
}
```

### 1.3 仓库

- `POST /api/v1/warehouses`

```json
{
  "warehouseCode": "WH-001",
  "warehouseName": "主仓库",
  "warehouseType": "FINISHED",
  "contactName": "王五",
  "contactPhone": "13800000003",
  "address": "上海市浦东新区",
  "status": 1
}
```

- `PUT /api/v1/warehouses/{id}`

```json
{
  "warehouseName": "主仓库-更新",
  "warehouseType": "FINISHED",
  "contactName": "王五",
  "contactPhone": "13800000003",
  "address": "上海市浦东新区张江路",
  "status": 1
}
```

- `GET /api/v1/warehouses/{id}`、`GET /api/v1/warehouses`、`DELETE /api/v1/warehouses/{id}`

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "id": 1,
    "warehouseCode": "WH-001",
    "warehouseName": "主仓库",
    "warehouseType": "FINISHED",
    "contactName": "王五",
    "contactPhone": "13800000003",
    "address": "上海市浦东新区",
    "status": 1
  }
}
```

### 1.4 库位

- `POST /api/v1/locations`

```json
{
  "warehouseId": 1,
  "locationCode": "LOC-001",
  "locationName": "主仓-A01",
  "locationType": "PICK",
  "status": 1
}
```

- `PUT /api/v1/locations/{id}`

```json
{
  "locationName": "主仓-A01-更新",
  "locationType": "PICK",
  "status": 1
}
```

- `GET /api/v1/locations/{id}`、`GET /api/v1/locations?warehouseId=1`、`DELETE /api/v1/locations/{id}`

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "id": 1,
    "warehouseId": 1,
    "locationCode": "LOC-001",
    "locationName": "主仓-A01",
    "locationType": "PICK",
    "status": 1
  }
}
```

---

## 2. 采购模块（`scm-purchase`）

### 2.1 采购订单（新增）

- `POST /api/v1/purchase-orders`

```json
{
  "orderNo": "PO-1001",
  "supplierId": 1,
  "remark": "首批采购",
  "items": [
    {
      "materialId": 1,
      "planQty": 20,
      "unitPrice": 10
    }
  ]
}
```

- `POST /api/v1/purchase-orders/{id}/cancel`

```json
{}
```

- `GET /api/v1/purchase-orders/{id}`、`GET /api/v1/purchase-orders/by-order-no?orderNo=PO-1001`、`GET /api/v1/purchase-orders`

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "id": 1,
    "orderNo": "PO-1001",
    "supplierId": 1,
    "orderStatus": "PARTIALLY_RECEIVED",
    "totalAmount": 200.0000,
    "remark": "首批采购",
    "items": [
      {
        "id": 11,
        "materialId": 1,
        "planQty": 20.0000,
        "receivedQty": 8.0000,
        "unitPrice": 10.0000
      }
    ]
  }
}
```

### 2.2 采购收货单

- `POST /api/v1/purchase-receipts`

```json
{
  "receiptNo": "RCV-1001",
  "purchaseOrderId": 1,
  "supplierId": 1,
  "warehouseId": 2001,
  "items": [
    {
      "materialId": 1,
      "locationId": 3001,
      "receiptQty": 8
    }
  ]
}
```

- `POST /api/v1/purchase-receipts/{id}/retry-stock-in`

```json
{}
```

- `POST /api/v1/purchase-receipts/{id}/cancel`

```json
{}
```

- `GET /api/v1/purchase-receipts/{id}`、`GET /api/v1/purchase-receipts/by-receipt-no?receiptNo=RCV-1001`、`GET /api/v1/purchase-receipts`

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "id": 1,
    "receiptNo": "RCV-1001",
    "purchaseOrderId": 1,
    "supplierId": 1,
    "warehouseId": 2001,
    "receiptStatus": "STOCK_IN_SUCCESS",
    "failureReason": null,
    "items": [
      {
        "id": 11,
        "materialId": 1,
        "locationId": 3001,
        "receiptQty": 8.0000
      }
    ]
  }
}
```

---

## 3. 销售模块（`scm-sales`）

- `POST /api/v1/sales-orders`

```json
{
  "orderNo": "SO-1001",
  "warehouseId": 2001,
  "items": [
    {
      "materialId": 1,
      "locationId": 3001,
      "saleQty": 2
    }
  ]
}
```

- `POST /api/v1/sales-orders/{id}/retry-lock`
- `POST /api/v1/sales-orders/{id}/ship`
- `POST /api/v1/sales-orders/{id}/retry-ship`
- `POST /api/v1/sales-orders/{id}/cancel`

```json
{}
```

- `GET /api/v1/sales-orders/{id}`、`GET /api/v1/sales-orders/by-order-no?orderNo=SO-1001`、`GET /api/v1/sales-orders`

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "id": 1,
    "orderNo": "SO-1001",
    "warehouseId": 2001,
    "orderStatus": "LOCK_SUCCESS",
    "failureReason": null,
    "items": [
      {
        "id": 11,
        "materialId": 1,
        "locationId": 3001,
        "saleQty": 2.0000
      }
    ]
  }
}
```

---

## 4. 库存模块（`scm-inventory`）

### 4.1 写操作接口

- `POST /api/v1/inventory/stock-ins`

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

- `POST /api/v1/inventory/adjustments`

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

- `POST /api/v1/inventory/locks`

```json
{
  "bizType": "SALES_ORDER",
  "bizNo": "SO-1001",
  "operatorId": 1,
  "items": [
    {
      "materialId": 1,
      "warehouseId": 2001,
      "locationId": 3001,
      "quantity": 2
    }
  ]
}
```

- `POST /api/v1/inventory/stock-outs`
- `POST /api/v1/inventory/locked-stock-outs`
- `POST /api/v1/inventory/unlocks`

```json
{
  "bizType": "SALES_ORDER",
  "bizNo": "SO-1001",
  "operatorId": 1,
  "items": [
    {
      "materialId": 1,
      "warehouseId": 2001,
      "locationId": 3001,
      "quantity": 2
    }
  ]
}
```

- `POST /api/v1/inventory/transfers`

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

- `POST /api/v1/inventory/stocktakes`

```json
{
  "bizType": "STOCKTAKE",
  "bizNo": "STK-001",
  "operatorId": 1,
  "items": [
    {
      "materialId": 1,
      "warehouseId": 2001,
      "locationId": 3001,
      "countedQty": 8
    }
  ]
}
```

### 4.2 查询接口

- `GET /api/v1/inventory/balances?materialId=1&warehouseId=2001&locationId=3001`

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "materialId": 1,
    "warehouseId": 2001,
    "locationId": 3001,
    "onHandQty": 100.0000,
    "lockedQty": 10.0000,
    "availableQty": 90.0000,
    "version": 3
  }
}
```

- `GET /api/v1/inventory/txn-records?bizType=SALES_ORDER&bizNo=SO-1001`

```json
{
  "code": "0",
  "message": "success",
  "data": [
    {
      "txnNo": "TXN-202604210001",
      "bizType": "SALES_ORDER",
      "bizNo": "SO-1001",
      "materialId": 1,
      "warehouseId": 2001,
      "locationId": 3001,
      "txnDirection": "OUT",
      "txnQty": 2.0000,
      "beforeQty": 100.0000,
      "afterQty": 98.0000
    }
  ]
}
```

---

## 5. 最近新增接口说明（重点）

- `POST /api/v1/purchase-orders/{id}/cancel`（采购订单取消）
- `POST /api/v1/sales-orders/{id}/retry-ship`（销售订单重试发货）
- `POST /api/v1/sales-orders/{id}/cancel`（销售订单取消，含解锁逻辑）
- `POST /api/v1/suppliers` / `PUT /api/v1/suppliers/{id}`（供应商主数据）

以上接口的 JSON 示例均已在本文覆盖。
