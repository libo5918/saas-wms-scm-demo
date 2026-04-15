# 仓库与库位接口说明

## 适用范围

本文档说明 `scm-mdm` 中仓库和库位主数据接口，供采购、库存、销售三个模块联调使用。

## 仓库接口

### 1. 创建仓库

`POST /api/v1/warehouses`

请求头：

```text
X-Tenant-Id: 1
Content-Type: application/json
```

请求体：

```json
{
  "warehouseCode": "WH-003",
  "warehouseName": "电商仓",
  "warehouseType": "FINISHED",
  "contactName": "王五",
  "contactPhone": "13800000003",
  "address": "上海市浦东新区 3 号",
  "status": 1
}
```

### 2. 查询仓库详情

`GET /api/v1/warehouses/{id}`

### 3. 查询仓库列表

`GET /api/v1/warehouses`

### 4. 修改仓库

`PUT /api/v1/warehouses/{id}`

### 5. 删除仓库

`DELETE /api/v1/warehouses/{id}`

## 库位接口

### 1. 创建库位

`POST /api/v1/locations`

请求体：

```json
{
  "warehouseId": 1,
  "locationCode": "LOC-003",
  "locationName": "成品区-C01",
  "locationType": "PICK",
  "status": 1
}
```

### 2. 查询库位详情

`GET /api/v1/locations/{id}`

### 3. 查询库位列表

`GET /api/v1/locations`

按仓库过滤：

`GET /api/v1/locations?warehouseId=1`

### 4. 修改库位

`PUT /api/v1/locations/{id}`

### 5. 删除库位

`DELETE /api/v1/locations/{id}`

## 联调规则

- `status=1` 表示启用，`status=0` 表示禁用
- 库位创建时必须传 `warehouseId`
- 采购、库存、销售在业务入口都会校验：
  - 仓库存在且启用
  - 库位存在且启用
  - 库位属于请求中的仓库

## 当前演示数据

- 仓库
  - `1 / WH-001 / 主仓`
  - `2 / WH-002 / 备件仓`
- 库位
  - `1 / LOC-001 / 主仓 / 成品区-A01`
  - `2 / LOC-002 / 主仓 / 成品区-B01`
  - `3 / LOC-101 / 备件仓 / 备件区-A01`
