# 销售出库业务流

## 1. 流程概览

1. 创建销售订单（`sales_order` + `sales_order_item`）
2. 自动触发库存锁定（`lock`）
3. 发货时执行锁定库存出库（`locked-stock-out`）
4. 根据结果回写销售订单状态

## 2. 销售订单状态机

- `CREATED`：已创建，待锁库
- `LOCK_SUCCESS`：锁库成功，待发货
- `LOCK_FAILED`：锁库失败
- `SHIP_FAILED`：发货失败
- `SHIPPED`：发货成功
- `CANCELLED`：已取消

## 3. 关键业务规则

- 仅 `LOCK_FAILED` 可重试锁库
- 仅 `LOCK_SUCCESS` 可发货
- 仅 `SHIP_FAILED` 可重试发货
- 可取消状态：`CREATED`、`LOCK_FAILED`、`LOCK_SUCCESS`、`SHIP_FAILED`
- 取消时如果订单仍持有锁定库存（`LOCK_SUCCESS` / `SHIP_FAILED`），会先自动解锁再取消

## 4. 接口清单（scm-sales）

- `POST /api/v1/sales-orders`：创建销售订单
- `POST /api/v1/sales-orders/{id}/retry-lock`：重试锁库
- `POST /api/v1/sales-orders/{id}/ship`：发货
- `POST /api/v1/sales-orders/{id}/retry-ship`：重试发货
- `POST /api/v1/sales-orders/{id}/cancel`：取消销售订单
- `GET /api/v1/sales-orders/{id}`：查询详情
- `GET /api/v1/sales-orders/by-order-no`：按单号查询
- `GET /api/v1/sales-orders`：查询列表

## 5. 联调建议

1. 先验证 `CREATED -> LOCK_SUCCESS -> SHIPPED` 正向链路
2. 构造锁库失败，验证 `LOCK_FAILED` 与重试能力
3. 构造发货失败，验证 `SHIP_FAILED`、重试发货和取消解锁逻辑
