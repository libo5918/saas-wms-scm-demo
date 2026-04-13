# saas-wms-scm

面向 SaaS WMS / SCM 场景的后端演示项目，当前阶段聚焦：

- 多模块工程骨架
- 主数据、采购、库存三个核心域
- 采购收货到库存入库的第一条业务联动链路

## 当前范围

当前只做后端与中间件集成，不做前端页面。

## 服务与模块

### 已有服务

- `scm-gateway`
  统一网关入口，后续承接路由转发、鉴权前置、灰度和跨域策略。
- `scm-auth`
  认证与租户基础服务，后续承接租户、用户、角色、登录态与权限模型。
- `scm-mdm`
  主数据域，当前已落地物料主数据管理。
- `scm-purchase`
  采购域，当前已落地采购收货单创建、查询、失败重试与取消。
- `scm-inventory`
  库存域，当前已落地库存入库、库存余额、库存流水查询。

### 公共模块

- `scm-common-core`
  通用返回体、异常、租户上下文等基础能力。
- `scm-common-web`
  全局异常处理、租户请求头拦截等 Web 公共能力。
- `scm-common-redis`
  Redis 公共封装预留。
- `scm-common-kafka`
  Kafka 公共封装预留。
- `scm-common-security`
  安全鉴权公共封装预留。
- `scm-common-mybatis`
  传统 MyBatis 公共封装预留。
- `scm-dependencies`
  统一依赖版本管理。

### 后续计划中的服务

- `scm-order-center`
  销售 / 出库相关订单中心。
- `scm-fulfillment`
  仓内作业服务，承接上架、拣货、移库、盘点等动作。
- `scm-settlement`
  采购 / 销售结算服务。
- `scm-report`
  报表与经营分析服务。
- `scm-message`
  消息与通知服务。

## 当前已完成

### scm-mdm

- 已完成 `material` 第一版 CRUD。
- 支持统一返回体、统一异常处理、租户头透传。
- MyBatis 同时演示两种写法：
  - 简单 `insert/update/delete` 写在 `Mapper` 注解里
  - `select` 写在 `resources/mapper/*.xml` 里
- 默认连接本地 MySQL：`127.0.0.1:3306/scm_mdm`

### scm-inventory

- 已完成库存入库第一版 DDD 模型。
- 已落地 `interfaces / application / domain / infrastructure` 四层结构。
- 支持：
  - 库存入库
  - 库存余额查询
  - 按业务单号查询库存流水
  - 库存流水落库
  - 基础幂等校验
- 默认连接本地 MySQL：`127.0.0.1:3306/scm_inventory`

### scm-purchase

- 已完成采购收货单创建、详情、按单号查询、列表查询。
- 已打通 `purchase receipt -> inventory stock-in` 同步联动。
- 已支持：
  - 收货单创建后自动触发库存入库
  - 收货单失败重试入库
  - 收货单取消
  - 收货单状态流转
- 当前状态模型：
  - `CREATED`
  - `STOCK_IN_SUCCESS`
  - `STOCK_IN_FAILED`
  - `CANCELLED`
- 默认连接本地 MySQL：`127.0.0.1:3306/scm_purchase`

## 当前主链路

### 采购收货到库存入库

1. 创建采购收货单
2. 收货单头和明细先落采购库
3. 同步调用库存服务执行入库
4. 成功则回写 `STOCK_IN_SUCCESS`
5. 失败则回写 `STOCK_IN_FAILED` 和 `failureReason`
6. 后续可以在原失败单据上执行重试，或直接取消

### 为什么拆成两段事务

采购收货单落库和库存服务调用不是同一个本地事务。

当前设计故意把它拆成：

1. 本地事务：创建收货单
2. 远程调用：库存入库
3. 本地事务：回写收货状态

这样做的目的，是在库存失败时仍然保留失败单据，便于排障和后续补偿，而不是整单回滚消失。

## 关键接口

### scm-mdm

- `POST /api/v1/materials`
  创建物料
- `PUT /api/v1/materials/{id}`
  修改物料
- `GET /api/v1/materials/{id}`
  查询物料详情
- `GET /api/v1/materials`
  查询物料列表
- `DELETE /api/v1/materials/{id}`
  逻辑删除物料

### scm-purchase

- `POST /api/v1/purchase-receipts`
  创建采购收货单
- `POST /api/v1/purchase-receipts/{id}/retry-stock-in`
  对失败收货单重试入库
- `POST /api/v1/purchase-receipts/{id}/cancel`
  取消未完成或失败的收货单
- `GET /api/v1/purchase-receipts/{id}`
  按主键查询收货单详情
- `GET /api/v1/purchase-receipts/by-receipt-no?receiptNo=...`
  按收货单号查询详情
- `GET /api/v1/purchase-receipts`
  查询收货单列表

### scm-inventory

- `POST /api/v1/inventory/stock-ins`
  执行库存入库
- `GET /api/v1/inventory/balances?materialId=...&warehouseId=...&locationId=...`
  查询库存余额
- `GET /api/v1/inventory/txn-records?bizType=...&bizNo=...`
  按业务单查询库存流水

## 本地启动说明

### MySQL

当前默认使用本地 MySQL：

- 地址：`127.0.0.1:3306`
- 用户名：`root`
- 密码：`123456`

服务默认会连接以下数据库：

- `scm_mdm`
- `scm_purchase`
- `scm_inventory`

当前 JDBC URL 已带 `createDatabaseIfNotExist=true`，如果本地账号有建库权限，可直接启动。

### 推荐启动顺序

如果只联调当前已完成范围，推荐顺序：

1. MySQL
2. `scm-mdm`
3. `scm-inventory`
4. `scm-purchase`

如果以后接入更多服务，再考虑：

1. 基础设施
2. `scm-auth`
3. `scm-mdm`
4. `scm-inventory`
5. `scm-purchase`
6. `scm-gateway`

## 联调示例

### 1. 创建收货单

```json
{
  "receiptNo": "RCV-001",
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

### 2. 查询收货单详情

```text
GET /api/v1/purchase-receipts/by-receipt-no?receiptNo=RCV-001
Header: X-Tenant-Id: 1
```

### 3. 查询库存流水

```text
GET /api/v1/inventory/txn-records?bizType=PURCHASE_RECEIPT&bizNo=RCV-001
Header: X-Tenant-Id: 1
```

### 4. 重试失败收货单

```text
POST /api/v1/purchase-receipts/{id}/retry-stock-in
Header: X-Tenant-Id: 1
```

### 5. 取消失败或未完成收货单

```text
POST /api/v1/purchase-receipts/{id}/cancel
Header: X-Tenant-Id: 1
```

## Git 管理建议

建议把整个项目纳入 Git 管理，并忽略以下内容：

- `.idea/`
- 各模块 `target/`
- 本地环境文件，如 `.env`
- 运行日志和临时文件

根目录已经提供 `.gitignore`。

## 下一步

1. 在 `inventory` 中继续补出库、锁库、库存调整
2. 在 `purchase` 中补采购订单和收货确认边界
3. 评估把 `purchase -> inventory` 从同步 HTTP 编排升级为事件驱动
4. 继续补充联调测试和演示数据
