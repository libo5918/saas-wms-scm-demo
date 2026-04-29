# 销售-库存事件驱动链路（当前实现）

## 适用范围

本文描述当前已经落地的 `scm-sales` 与 `scm-inventory` 之间的异步闭环流程，包含：

- 发货流程
- 取消流程

## 发货流程

1. 调用接口：`POST /api/v1/sales-orders/{id}/ship`
2. `scm-sales` 在同一本地事务内完成：
   - 写入 `outbox_event`（`ORDER_SHIP_REQUESTED`，topic=`order.ship.requested.v1`）
   - 订单状态更新为 `SHIP_PENDING`
3. Outbox 发布器将事件发送到 Kafka。
4. `scm-inventory` 消费 `order.ship.requested.v1` 并执行“锁定库存出库”。
5. `scm-inventory` 发布结果事件到 `inventory.ship.result.v1`：
   - `SUCCESS`
   - `FAILED`（带失败原因）
6. `scm-sales` 消费结果事件并更新终态：
   - `SUCCESS` -> `SHIPPED`
   - `FAILED` -> `SHIP_FAILED`

## 取消流程

1. 调用接口：`POST /api/v1/sales-orders/{id}/cancel`
2. `scm-sales` 判断订单是否仍持有锁定库存。
3. 如果持有锁定库存，则在同一本地事务内完成：
   - 写入 `outbox_event`（`ORDER_CANCEL_REQUESTED`，topic=`order.cancel.requested.v1`）
   - 订单状态更新为 `CANCEL_PENDING`
4. Outbox 发布器将事件发送到 Kafka。
5. `scm-inventory` 消费 `order.cancel.requested.v1` 并执行库存解锁。
6. `scm-inventory` 发布结果事件到 `inventory.cancel.result.v1`：
   - `SUCCESS`
   - `FAILED`（带失败原因）
7. `scm-sales` 消费结果事件并更新终态：
   - `SUCCESS` -> `CANCELLED`
   - `FAILED` -> `CANCEL_FAILED`

如果订单不持有锁定库存，`scm-sales` 直接将状态更新为 `CANCELLED`。

## 幂等机制

### 消费幂等（mq_consume_log）

`mq_consume_log` 使用唯一键：

- `(consumer_group, topic, event_id)`

消费者落库语句：

```sql
INSERT IGNORE INTO mq_consume_log(tenant_id, consumer_group, topic, event_id, biz_key)
VALUES (?, ?, ?, ?, ?)
```

同一事件重复投递时会命中唯一键冲突，`INSERT IGNORE` 会忽略重复写入。

### 领域幂等（库存侧）

库存领域会拒绝重复业务动作并抛出：

- `Duplicate stock-out request`
- `Duplicate stock-unlock request`

消费者将上述重复场景视为“幂等成功”，并继续写入消费日志，避免无限重试。

## 订单状态机（sales）

- `CREATED`
- `LOCK_SUCCESS`
- `LOCK_FAILED`
- `SHIP_PENDING`
- `SHIPPED`
- `SHIP_FAILED`
- `CANCEL_PENDING`
- `CANCELLED`
- `CANCEL_FAILED`

## 事务边界规则

对于 `ship/cancel` 发起动作，以下两步必须同事务提交：

- `appendOutboxEvent(...)`
- `updateOrderStatus(...)`

当前实现是在 `ship/retryShip/cancel` 入口方法上使用 `@Transactional`。

## 超时补偿任务（已实现）

`scm-sales` 新增定时任务 `PendingOrderCompensationJob`，用于处理长时间停留在处理中状态的订单：

- 扫描状态：`SHIP_PENDING`、`CANCEL_PENDING`
- 扫描条件：`updated_at` 超过配置阈值（默认 10 分钟）
- 补偿动作：若该订单满足以下条件，则补写一条 outbox 事件
  - 不存在同类型未发送 outbox 事件（`NEW/FAILED`）
  - 冷却窗口内不存在同类型 outbox 事件（包含 `SENT`）
  - 同订单同事件类型累计补偿次数未超过上限
  - `SHIP_PENDING` -> `ORDER_SHIP_REQUESTED` / `order.ship.requested.v1`
  - `CANCEL_PENDING` -> `ORDER_CANCEL_REQUESTED` / `order.cancel.requested.v1`

任务参数（`scm-sales/application.yml`）：

- `compensation.pending-order.fixed-delay-ms`（默认 `60000`）
- `compensation.pending-order.timeout-minutes`（默认 `10`）
- `compensation.pending-order.limit`（默认 `100`）
- `compensation.pending-order.cooldown-minutes`（默认 `15`）
- `compensation.pending-order.max-events-per-order`（默认 `5`）

### 设计目的

上述三道门槛用于避免同一订单在 `PENDING` 状态下被定时任务无限补写事件，导致：

- `outbox_event` 持续膨胀
- 下游重复处理与重复结果事件增加
- `mq_consume_log` 出现同一 `biz_key` 的高频新事件记录（不同 `event_id`）
