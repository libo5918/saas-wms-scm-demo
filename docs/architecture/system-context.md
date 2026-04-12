# 系统上下文

## 项目定位

这是一个企业级 SaaS 供应链/仓储项目，用于 Java 后端复训和面试展示。

## 核心目标

- 恢复真实企业项目开发手感
- 输出可讲清楚的微服务架构案例
- 通过采购入库链路串起库存领域

## 服务划分

- `scm-gateway`
- `scm-auth`
- `scm-mdm`
- `scm-purchase`
- `scm-inventory`

## 中间件

- Nacos
- MySQL
- Redis
- Kafka
- SkyWalking
- Seata（第二阶段）
- Elasticsearch（第二阶段）
