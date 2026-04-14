# saas-wms-scm

面向 SaaS WMS / SCM 场景的后端训练项目。当前重点是用多模块方式逐步落地主数据、采购、库存、销售这几条核心链路。

## 当前范围

- 只做后端与中间件集成，不做前端页面
- 默认本地 MySQL 开发
- 先用同步 HTTP 编排把主链路跑通，再逐步补状态机、幂等、测试和 DDD 分层

## 服务与模块

### 业务服务

- `scm-mdm`
  - 主数据域
  - 当前已落地物料主数据 CRUD

- `scm-purchase`
  - 采购域
  - 当前已落地采购收货单创建、查询、失败重试、取消
  - 已打通 `purchase receipt -> inventory stock-in`

- `scm-inventory`
  - 库存域
  - 当前已落地 DDD 第一版模型
  - 已支持入库、出库、锁库、解锁、库存余额查询、按业务单查库存流水

- `scm-sales`
  - 销售域最小骨架
  - 当前已落地销售单创建、查询、发货、取消
  - 已打通 `sales order -> inventory lock/out/unlock`

- `scm-auth`
  - 认证与租户基础服务骨架

- `scm-gateway`
  - 网关骨架

### 公共模块

- `scm-common-core`
  - 通用返回体、异常、租户上下文

- `scm-common-web`
  - 全局异常处理、租户请求头拦截、Web 公共配置

- `scm-common-mybatis`
  - 传统 MyBatis 公共封装预留

- `scm-common-redis`
  - Redis 公共封装预留

- `scm-common-kafka`
  - Kafka 公共封装预留

- `scm-common-security`
  - 安全能力预留

- `scm-dependencies`
  - 统一依赖版本管理

## 当前已完成

### scm-mdm

- 物料主数据 CRUD
- 统一返回体、统一异常处理、租户头透传
- MyBatis 注解 SQL + XML SQL 混合示例

### scm-purchase

- 收货单创建、详情、按单号查询、列表
- 收货单状态流转
  - `CREATED`
  - `STOCK_IN_SUCCESS`
  - `STOCK_IN_FAILED`
  - `CANCELLED`
- 自动调用库存入库
- 入库失败后保留失败单据并记录 `failureReason`
- 支持失败重试和取消

### scm-inventory

- DDD 四层结构
  - `interfaces`
  - `application`
  - `domain`
  - `infrastructure`
- 库存动作
  - 入库
  - 出库
  - 锁库
  - 解锁
- 查询能力
  - 库存余额查询
  - 按业务单查库存流水
- 幂等控制
  - 同业务单同库存维度重复动作会拒绝

### scm-sales

- 销售单创建、详情、按单号查询、列表
- 销售单状态流转
  - `CREATED`
  - `LOCK_SUCCESS`
  - `LOCK_FAILED`
  - `SHIPPED`
  - `CANCELLED`
- 自动调用库存锁库
- 发货时自动调用库存出库
- 取消时如果已锁库则自动解锁

## 主链路

### 采购收货到库存入库

1. 创建采购收货单
2. 先落采购单头和明细
3. 调用库存服务执行入库
4. 成功则回写 `STOCK_IN_SUCCESS`
5. 失败则回写 `STOCK_IN_FAILED` 和 `failureReason`
6. 后续可在原失败单据上重试或取消

### 销售下单到库存联动

1. 创建销售单
2. 先落销售单头和明细
3. 调用库存服务执行锁库
4. 成功则回写 `LOCK_SUCCESS`
5. 失败则回写 `LOCK_FAILED` 和 `failureReason`
6. 发货时调用库存出库
7. 取消时如果已锁库则调用库存解锁

## 关键接口

### scm-mdm

- `POST /api/v1/materials`
- `PUT /api/v1/materials/{id}`
- `GET /api/v1/materials/{id}`
- `GET /api/v1/materials`
- `DELETE /api/v1/materials/{id}`

### scm-purchase

- `POST /api/v1/purchase-receipts`
- `POST /api/v1/purchase-receipts/{id}/retry-stock-in`
- `POST /api/v1/purchase-receipts/{id}/cancel`
- `GET /api/v1/purchase-receipts/{id}`
- `GET /api/v1/purchase-receipts/by-receipt-no`
- `GET /api/v1/purchase-receipts`

### scm-inventory

- `POST /api/v1/inventory/stock-ins`
- `POST /api/v1/inventory/stock-outs`
- `POST /api/v1/inventory/locked-stock-outs`
- `POST /api/v1/inventory/locks`
- `POST /api/v1/inventory/unlocks`
- `GET /api/v1/inventory/balances`
- `GET /api/v1/inventory/txn-records`

### scm-sales

- `POST /api/v1/sales-orders`
- `POST /api/v1/sales-orders/{id}/retry-lock`
- `POST /api/v1/sales-orders/{id}/ship`
- `POST /api/v1/sales-orders/{id}/cancel`
- `GET /api/v1/sales-orders/{id}`
- `GET /api/v1/sales-orders/by-order-no`
- `GET /api/v1/sales-orders`

## 本地启动

### MySQL

- 地址：`127.0.0.1:3306`
- 用户：`root`
- 密码：`123456`

默认会使用这些库：

- `scm_mdm`
- `scm_purchase`
- `scm_inventory`
- `scm_sales`

### 推荐启动顺序

如果只联调当前主链路：

1. MySQL
2. `scm-mdm`
3. `scm-inventory`
4. `scm-purchase`
5. `scm-sales`

如果后面接回更多服务：

1. 基础设施
2. `scm-auth`
3. `scm-mdm`
4. `scm-inventory`
5. `scm-purchase`
6. `scm-sales`
7. `scm-gateway`

## 联调示例

### 1. 创建采购收货单

```json
{
  "receiptNo": "RCV-1001",
  "purchaseOrderId": 5001,
  "warehouseId": 2001,
  "items": [
    {
      "materialId": 1,
      "locationId": 3001,
      "receiptQty": 10
    }
  ]
}
```

### 2. 创建销售订单

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

### 3. 销售订单发货

```text
POST /api/v1/sales-orders/{id}/ship
Header: X-Tenant-Id: 1
```

### 4. 销售订单取消

```text
POST /api/v1/sales-orders/{id}/cancel
Header: X-Tenant-Id: 1
```

### 5. 查询库存流水

```text
GET /api/v1/inventory/txn-records?bizType=SALES_ORDER&bizNo=SO-1001
Header: X-Tenant-Id: 1
```

## Git 管理建议

建议将整个项目纳入 Git 管理，并忽略：

- `.idea/`
- 各模块 `target/`
- 本地环境文件，如 `.env`
- 运行日志和临时文件

## 下一步

1. 补销售域的失败重试与更细状态流转
2. 评估把 `purchase` / `sales` 对 `inventory` 的同步 HTTP 编排升级为事件驱动
3. 在库存域补库存调整、移库、盘点
4. 继续补联调测试和演示数据
