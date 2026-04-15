# 采购入库业务流

## 1. 流程概览

1. 创建采购订单（`purchase_order` + `purchase_order_item`）
2. 创建采购收货单（`purchase_receipt` + `purchase_receipt_item`）
3. 采购收货单触发库存入库（调用 `scm-inventory`）
4. 入库成功后，回写采购收货单状态和采购订单进度

## 2. 采购订单状态机

- `CREATED`：已创建，尚未开始收货
- `PARTIALLY_RECEIVED`：已部分收货
- `RECEIVED`：已全部收货
- `CANCELLED`：已取消（仅允许 `CREATED` 取消）

状态规则：
- 收货成功后，系统按订单明细 `received_qty / plan_qty` 自动推导状态
- 有任一明细已收且仍有未收明细 -> `PARTIALLY_RECEIVED`
- 全部明细收满 -> `RECEIVED`
- 已部分收货或已收货完成，不允许取消

## 3. 采购收货单状态机

- `CREATED`：已建单，待入库
- `STOCK_IN_SUCCESS`：入库成功
- `STOCK_IN_FAILED`：入库失败（记录 `failureReason`）
- `CANCELLED`：已取消

状态规则：
- 仅 `STOCK_IN_FAILED` 允许重试入库
- `CREATED` / `STOCK_IN_FAILED` 允许取消
- `STOCK_IN_SUCCESS` 不允许取消

## 4. 关键业务约束

- 收货单 `supplierId` 必须与采购订单 `supplierId` 一致
- 收货单明细物料必须存在于采购订单明细中
- 收货数量不能超过采购订单剩余可收数量
- 同一收货单若同一物料出现多行，会先按物料汇总再回写进度
- 进度回写采用 SQL 条件更新（`received_qty + increment <= plan_qty`），避免并发超收

## 5. 接口清单（scm-purchase）

- `POST /api/v1/purchase-orders`：创建采购订单
- `GET /api/v1/purchase-orders/{id}`：查询采购订单详情
- `GET /api/v1/purchase-orders/by-order-no`：按单号查询采购订单
- `GET /api/v1/purchase-orders`：查询采购订单列表
- `POST /api/v1/purchase-orders/{id}/cancel`：取消采购订单

- `POST /api/v1/purchase-receipts`：创建采购收货单
- `POST /api/v1/purchase-receipts/{id}/retry-stock-in`：重试收货入库
- `POST /api/v1/purchase-receipts/{id}/cancel`：取消收货单
- `GET /api/v1/purchase-receipts/{id}`：查询收货单详情
- `GET /api/v1/purchase-receipts/by-receipt-no`：按单号查询收货单
- `GET /api/v1/purchase-receipts`：查询收货单列表

## 6. 联调建议

1. 先建采购订单，再创建收货单
2. 用一次成功入库验证订单状态从 `CREATED -> PARTIALLY_RECEIVED/RECEIVED`
3. 构造超收请求，验证会被拒绝
4. 构造库存失败场景，验证收货单变为 `STOCK_IN_FAILED` 且可重试
