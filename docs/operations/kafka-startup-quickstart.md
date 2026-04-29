# Kafka 启动前快速清单

## 适用场景

本地或测试环境首次拉起、重建 Topic、或排查 Topic 参数漂移。

## 推荐顺序

1. 先创建 Topic（统一参数）  
2. 再校验 Topic（存在性 + 分区 + 副本 + min.insync.replicas）  
3. 最后启动业务服务（`scm-inventory`、`scm-sales`）

## 一键创建 Topic

```bash
bash docs/operations/scripts/create-kafka-topics.sh kafka-1:29092 3 1 1
```

参数含义：

1. bootstrap-server
2. partitions
3. replication-factor
4. min.insync.replicas

## 一键校验 Topic

```bash
bash docs/operations/scripts/check-kafka-topics.sh kafka-1:29092 3 1 1
```

如果在宿主机执行但 Kafka CLI 在容器里，使用：

```bash
bash docs/operations/scripts/check-kafka-topics.sh --docker --container kafka-1 kafka-1:29092 3 3 2
```

若输出 `All topic checks passed.`，再启动服务。

## 一键发布前自检（推荐）

当 topic 和服务都已启动后，执行：

```bash
bash docs/operations/scripts/preflight-check.sh
```

容器模式（推荐）：

```bash
bash docs/operations/scripts/preflight-check.sh --docker --container kafka-1 --host host.docker.internal kafka-1:29092 3 3 2
```

该脚本会校验：

1. Topic 契约参数
2. `scm-inventory` / `scm-sales` 健康状态
3. Actuator metrics 可达性
4. 关键消费失败与 DLQ 指标存在性

## 启动后建议验证

1. 查看指标接口是否可访问：
   - `http://localhost:18084/actuator/metrics`
   - `http://localhost:18085/actuator/metrics`
2. 检查关键计数器：
   - `scm.inventory.mq.consume.failed`
   - `scm.inventory.mq.consume.dead-letter`
   - `scm.sales.mq.consume.ship-result.failed`
   - `scm.sales.mq.consume.cancel-result.failed`
