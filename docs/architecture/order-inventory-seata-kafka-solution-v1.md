# 订单-库存 Seata + Kafka 落地方案（V1）

## 1. 目标与边界

- 目标：在 `scm-sales`（订单）与 `scm-inventory`（库存）之间实现高一致性、可补偿、可观测。
- 边界：
  - 同步强一致：下单锁库（必须立即返回结果）使用 Seata。
  - 异步最终一致：发货、取消、超时关单等使用 Kafka 事件驱动。

## 2. 事务策略

### 2.1 同步链路（Seata AT）

- 场景：`创建销售订单 -> 锁库`
- 建议：
  - `SalesOrderService#create` 开启 `@GlobalTransactional`
  - 订单写库与库存冻结在同一全局事务内
  - 任一失败全局回滚

### 2.2 异步链路（Kafka + Outbox）

- 场景：
  - 发货请求
  - 取消订单释放库存
  - 超时关单
- 原则：
  - 业务状态更新与 Outbox 落库同事务提交
  - 独立发布器将 Outbox 推送到 Kafka
  - 消费端幂等处理

## 3. 状态机建议

### 3.1 订单状态（scm-sales）

- `CREATED`
- `LOCK_SUCCESS`
- `LOCK_FAILED`
- `SHIP_PENDING`
- `SHIPPED`
- `SHIP_FAILED`
- `CANCEL_PENDING`
- `CANCELLED`
- `CANCEL_FAILED`

### 3.2 库存预留状态（scm-inventory）

- `RESERVED`（已冻结）
- `RELEASED`（已释放）
- `CONSUMED`（已扣减）

## 4. Kafka Topic 规划

- `order.ship.requested.v1`
- `order.cancel.requested.v1`
- `inventory.ship.result.v1`
- `inventory.release.result.v1`
- `order.timeout.cancel.v1`（可选）
- `scm.dlq.v1`（死信）

建议：
- `key = orderNo`，保证同订单有序。
- value 包含：`eventId`、`eventType`、`orderNo`、`tenantId`、`occurredAt`、`payload`。

## 5. 数据表设计（新增）

## 5.1 Outbox（建议每个服务各一张）

```sql
CREATE TABLE IF NOT EXISTS outbox_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    event_id VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(32) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    event_key VARCHAR(64) NOT NULL,
    payload_json JSON NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'NEW',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_time DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_outbox_event_id (event_id),
    KEY idx_outbox_status_retry (status, next_retry_time)
);
```

## 5.2 消费幂等表

```sql
CREATE TABLE IF NOT EXISTS mq_consume_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    consumer_group VARCHAR(64) NOT NULL,
    topic VARCHAR(128) NOT NULL,
    event_id VARCHAR(64) NOT NULL,
    biz_key VARCHAR(64) DEFAULT NULL,
    consumed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_consume_unique (consumer_group, topic, event_id)
);
```

## 6. 核心流程

## 6.1 创建订单锁库（同步）

1. `scm-sales` 开启 Seata 全局事务。
2. 写 `sales_order=CREATED`、`sales_order_item`。
3. 调 `scm-inventory` 执行锁库。
4. 成功：订单改 `LOCK_SUCCESS`。
5. 失败：全局回滚，返回错误。

## 6.2 发货（异步）

1. 订单服务将状态改 `SHIP_PENDING`，同事务写 Outbox：`order.ship.requested.v1`。
2. 发布器推 Kafka。
3. 库存消费并执行“冻结转扣减”。
4. 库存发送结果事件：
   - 成功：`inventory.ship.result.v1(status=SUCCESS)`
   - 失败：`inventory.ship.result.v1(status=FAILED)`
5. 订单消费结果，更新 `SHIPPED/SHIP_FAILED`。

## 6.3 取消（异步）

1. 订单状态改 `CANCEL_PENDING`，写 Outbox：`order.cancel.requested.v1`。
2. 库存消费后释放冻结库存。
3. 库存发 `inventory.release.result.v1`。
4. 订单消费结果，更新 `CANCELLED/CANCEL_FAILED`。

## 7. 补偿机制

## 7.1 自动补偿（优先）

- 发布失败：Outbox 轮询重试（指数退避，例：1m/5m/15m/1h）。
- 消费失败：业务异常重试，超过阈值投递 `scm.dlq.v1`。
- 业务失败：
  - 发货失败 -> `SHIP_FAILED`，支持重试发货。
  - 取消失败 -> `CANCEL_FAILED`，定时任务重复触发取消事件。

## 7.2 人工补偿（兜底）

- 提供“补偿任务”查询维度：
  - `orderNo`
  - 当前状态
  - 最近失败原因
  - 重试次数
- 人工可触发：重试发货、重试取消、强制解锁（需审计）。

## 8. 幂等与防重

- 生产端：
  - `event_id` 全局唯一
  - Outbox `uk_outbox_event_id` 防重复发布
- 消费端：
  - 先写 `mq_consume_log`（唯一键）
  - 冲突即判定已消费，直接返回成功
- 业务端：
  - 库存事务号建议 `bizType + bizNo + action` 唯一约束

## 9. 监控告警

- 指标：
  - Outbox 未发送积压数
  - DLQ 消息数
  - 订单 `*_FAILED` 状态数量
  - 补偿任务重试次数分布
- 告警建议：
  - Outbox 积压 > 阈值（持续 5 分钟）
  - DLQ 新增 > 阈值
  - `CANCEL_FAILED` / `SHIP_FAILED` 突增

## 10. 分阶段实施

### Phase 1（1-2 周）

- 接入 Seata 到 `create+lock` 同步链路
- 完成 Outbox 与消费幂等表建模

### Phase 2（1-2 周）

- 发货、取消改造为 Kafka 异步
- 增加重试与 DLQ

### Phase 3（持续优化）

- 补偿后台页面
- 压测与故障演练（broker down、消费堆积、部分超时）

## 11. 与当前项目的直接改造点

- `scm-sales`
  - `SalesOrderServiceImpl`：拆分 `ship/cancel` 为状态推进 + Outbox 事件
  - 新增 `OutboxEventMapper`、`MqConsumeLogMapper`
- `scm-inventory`
  - 新增 Kafka 消费者：发货请求、取消请求
  - 消费后调用现有 `locked-stock-out` / `unlock` 应用服务
- `deploy/sql`
  - 增加 `outbox_event`、`mq_consume_log` DDL

