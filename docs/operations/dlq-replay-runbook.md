# DLQ 回放操作手册（中文）

## 目的

当消息进入死信 Topic（DLQ）后，支持在修正消息内容后人工回放到原 Topic，恢复业务链路。

## 适用 Topic

- 请求链路 DLQ：`sales.order.requested.dlq.v1`
- 结果链路 DLQ：`sales.inventory.result.dlq.v1`

## 前置条件

1. 已确认导致失败的根因（字段缺失、格式错误、业务数据问题等）。
2. 已修复根因（代码、配置、主数据、topic 参数等）。
3. 有 Kafka 容器执行权限（默认 `kafka-1`）。

## 操作步骤

1. 从 DLQ 读取一条消息样本：

```bash
docker exec -it kafka-1 kafka-console-consumer --bootstrap-server kafka-1:29092 --topic sales.inventory.result.dlq.v1 --from-beginning --max-messages 1
```

2. 确认消息中的 `sourceTopic`（原始目标 Topic）和 `value`（原始消息）。

3. 修正消息为可消费的 JSON（例如补齐 `tenantId`、`orderNo` 等必填字段）。

4. 执行回放脚本：

```bash
bash docs/operations/scripts/replay-dlq-message.sh kafka-1 kafka-1:29092 sales.inventory.result.dlq.v1
```

5. 根据提示输入：
- 回放目标 Topic（`sourceTopic`）
- key（可空）
- 修正后的 JSON

6. 观察验证：
- 消费端日志无重复异常
- 对应 `*.failed` 指标不再持续增长
- 业务状态恢复（订单状态推进）

## 注意事项

1. 回放前建议先小流量单条验证，避免批量错误回放。
2. 回放消息建议保留原 `eventId`，便于链路追踪。
3. 若启用了幂等消费，重复回放同一 `eventId` 可能会被直接跳过。
