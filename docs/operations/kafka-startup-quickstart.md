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

若输出 `All topic checks passed.`，再启动服务。

## 启动后建议验证

1. 查看指标接口是否可访问：
   - `http://localhost:18084/actuator/metrics`
   - `http://localhost:18085/actuator/metrics`
2. 检查关键计数器：
   - `scm.inventory.mq.consume.failed`
   - `scm.inventory.mq.consume.dead-letter`
   - `scm.sales.mq.consume.ship-result.failed`
   - `scm.sales.mq.consume.cancel-result.failed`
