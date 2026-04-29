# Sales 结果消息 DLQ 回归清单

## 目标

验证 `scm-sales` 在消费库存结果 Topic 时，遇到坏消息会进入 DLQ，而不是无限重试。

涉及 Topic：

- `inventory.ship.result.v1`
- `inventory.cancel.result.v1`
- `sales.inventory.result.dlq.v1`

## 前置条件

1. `scm-sales` 服务已启动（默认 `18085`）。
2. Kafka Topic 已按契约创建完成。
3. 可用 `docker exec kafka-1` 执行 Kafka 命令。

## 验证步骤

1. 记录初始指标值：

```bash
curl -s http://localhost:18085/actuator/metrics/scm.sales.mq.consume.ship-result.failed
curl -s http://localhost:18085/actuator/metrics/scm.sales.mq.consume.cancel-result.failed
curl -s http://localhost:18085/actuator/metrics/scm.sales.mq.consume.ship-result.dead-letter
curl -s http://localhost:18085/actuator/metrics/scm.sales.mq.consume.cancel-result.dead-letter
```

2. 注入坏消息（无 `tenantId`）：

```bash
bash docs/operations/scripts/inject-sales-result-bad-messages.sh kafka-1 kafka-1:29092
```

3. 再次读取指标，确认失败计数和 dead-letter 计数增加。

4. 消费 DLQ Topic，确认可读到坏消息：

```bash
docker exec -it kafka-1 kafka-console-consumer --bootstrap-server kafka-1:29092 --topic sales.inventory.result.dlq.v1 --from-beginning --max-messages 10
```

## 预期结果

1. `scm.sales.mq.consume.ship-result.failed` 增加。
2. `scm.sales.mq.consume.cancel-result.failed` 增加。
3. `scm.sales.mq.consume.ship-result.dead-letter` 增加。
4. `scm.sales.mq.consume.cancel-result.dead-letter` 增加。
5. `sales.inventory.result.dlq.v1` 可读到对应坏消息。
