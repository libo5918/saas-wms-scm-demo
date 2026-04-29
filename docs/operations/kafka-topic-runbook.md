# Kafka Topic 运维手册（本地集群）

## 目的

用于销售-库存异步链路的 Kafka topic 检查与故障处理。

## 必需 Topic

- `order.ship.requested.v1`
- `order.cancel.requested.v1`
- `inventory.ship.result.v1`
- `inventory.cancel.result.v1`

## 推荐参数（3 节点集群）

- 分区数：`3`
- 副本因子：`3`
- `min.insync.replicas=2`
- Producer：`acks=all`

## 健康检查

```bash
kafka-topics --bootstrap-server kafka-1:29092 --describe --topic inventory.cancel.result.v1
```

健康状态建议满足：

- `PartitionCount: 3`
- `ReplicationFactor: 3`
- 每个分区都有 3 个副本
- `Isr` 数量至少 2（推荐 3）

## 常见故障：NOT_ENOUGH_REPLICAS

现象：

- producer 持续重试发送
- 日志报错 `NOT_ENOUGH_REPLICAS`

根因：

- `acks=all` 下，ISR 数量不满足确认条件
- 典型错误组合：副本因子为 `1`，但 `min.insync.replicas=2`

## 快速止血（仅改 topic 参数）

当本地环境临时降级时，可先降低该 topic 的 `min.insync.replicas`：

```bash
kafka-configs --bootstrap-server kafka-1:29092 \
  --entity-type topics \
  --entity-name inventory.cancel.result.v1 \
  --alter --add-config min.insync.replicas=1
```

## 重建为生产化参数

先停掉相关生产者/消费者，避免 topic 被自动重建。

```bash
kafka-topics --bootstrap-server kafka-1:29092 --delete --topic inventory.cancel.result.v1
kafka-topics --bootstrap-server kafka-1:29092 --create \
  --topic inventory.cancel.result.v1 \
  --partitions 3 \
  --replication-factor 3 \
  --config min.insync.replicas=2
```

重建后再次检查：

```bash
kafka-topics --bootstrap-server kafka-1:29092 --describe --topic inventory.cancel.result.v1
```

## “删不掉 topic” 的判断

如果执行删除后 `TopicId` 变化，但 topic 仍存在，说明：

- topic 已删成功
- 又被自动创建出来了

常见原因：

- 应用还在持续发送/消费
- broker 开启了自动建 topic

处理顺序：

1. 停相关服务
2. 删除 topic
3. 确认 topic 不在列表中
4. 按目标参数重建
5. 启动服务
