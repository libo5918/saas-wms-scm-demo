# Topic 契约与启动检查清单

## 适用范围

本文档定义销售-库存事件驱动链路使用的 Kafka Topic，以及服务启动前后的检查顺序要求。

## Topic 契约

### 业务 Topic

| Topic | Producer | Consumer | Purpose |
| --- | --- | --- | --- |
| `order.ship.requested.v1` | `scm-sales` outbox 发布器 | `scm-inventory` | 发货请求事件 |
| `order.cancel.requested.v1` | `scm-sales` outbox 发布器 | `scm-inventory` | 取消/解锁请求事件 |
| `inventory.ship.result.v1` | `scm-inventory` | `scm-sales` | 发货结果回传事件 |
| `inventory.cancel.result.v1` | `scm-inventory` | `scm-sales` | 取消结果回传事件 |
| `sales.order.requested.dlq.v1` | `scm-inventory` | 运维/人工回放 | 请求事件死信 Topic（无效消息或处理失败） |
| `sales.inventory.result.dlq.v1` | `scm-sales` | 运维/人工回放 | 库存结果事件死信 Topic（无效消息或处理失败） |

### 消费组

| Service | Consumer group |
| --- | --- |
| `scm-inventory` | `scm-inventory-order-consumer` |
| `scm-sales` | `scm-sales-ship-result-consumer` |
| `scm-sales` | `scm-sales-cancel-result-consumer` |

## Topic 参数规范

按环境选择以下其中一套标准：

### 本地单节点 Kafka

- `partitions=3`
- `replication.factor=1`
- `min.insync.replicas=1`

### 多节点集群（推荐基线）

- `partitions=3`
- `replication.factor=3`
- `min.insync.replicas=2`

## 启动前检查清单（必须通过）

1. 先创建全部必需 Topic。  
2. 校验每个 Topic 都存在，且分区数符合预期。  
3. 校验 `replication.factor` 与 `min.insync.replicas` 与当前集群规模兼容。  
4. 校验应用配置中的 topic 名称与本文档契约一致。  
5. Topic 检查通过后，再启动消费者（`scm-inventory`/`scm-sales`）。

## 建议创建命令

将 `--bootstrap-server` 替换为你的 Kafka 连接地址。

```bash
kafka-topics.sh --bootstrap-server kafka-1:29092 --create --if-not-exists --topic order.ship.requested.v1 --partitions 3 --replication-factor 1 --config min.insync.replicas=1
kafka-topics.sh --bootstrap-server kafka-1:29092 --create --if-not-exists --topic order.cancel.requested.v1 --partitions 3 --replication-factor 1 --config min.insync.replicas=1
kafka-topics.sh --bootstrap-server kafka-1:29092 --create --if-not-exists --topic inventory.ship.result.v1 --partitions 3 --replication-factor 1 --config min.insync.replicas=1
kafka-topics.sh --bootstrap-server kafka-1:29092 --create --if-not-exists --topic inventory.cancel.result.v1 --partitions 3 --replication-factor 1 --config min.insync.replicas=1
kafka-topics.sh --bootstrap-server kafka-1:29092 --create --if-not-exists --topic sales.order.requested.dlq.v1 --partitions 3 --replication-factor 1 --config min.insync.replicas=1
kafka-topics.sh --bootstrap-server kafka-1:29092 --create --if-not-exists --topic sales.inventory.result.dlq.v1 --partitions 3 --replication-factor 1 --config min.insync.replicas=1
```

## 校验脚本

执行方式：

```bash
bash docs/operations/scripts/check-kafka-topics.sh kafka-1:29092 3 1 1
```

参数说明：

1. bootstrap-server  
2. 预期分区数  
3. 预期副本因子  
4. 预期 min.insync.replicas
