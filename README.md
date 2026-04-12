# saas-wms-scm-demo

企业级 SaaS 供应链 / 仓储复训与面试项目。

## 当前阶段

第一阶段只做后端与中间件，不做前端页面。

## 服务与模块说明

### 已有服务

- `scm-gateway`
  统一网关入口，后续承接路由转发、鉴权前置、灰度和统一跨域策略。
- `scm-auth`
  认证与租户基础域，负责租户、用户、角色、登录态与权限模型。
- `scm-mdm`
  主数据域，负责物料、仓库、库位、供应商等基础档案。
- `scm-purchase`
  采购域，负责采购订单、收货单，以及采购入库业务编排。
- `scm-inventory`
  库存域，负责库存余额、库存流水、库存变更和库存事件。

### 公共模块

- `scm-dependencies`
  统一依赖版本管理。
- `scm-common-core`
  通用返回体、异常、租户上下文等基础能力。
- `scm-common-web`
  Web 公共能力，如全局异常处理、租户头拦截器。
- `scm-common-mybatis`
  传统 MyBatis 公共封装预留位。
- `scm-common-redis`
  Redis 公共封装预留位。
- `scm-common-kafka`
  Kafka 公共封装预留位。
- `scm-common-security`
  安全鉴权公共封装预留位。

### 后续新增服务规划

后续如果新增服务，也统一记录在这里。目前已规划但尚未拆分的候选服务如下：

- `scm-order-center`
  销售 / 出库相关订单中心，承接销售订单、出库单等流程。
- `scm-fulfillment`
  仓内作业服务，承接上架、拣货、移库、盘点等仓储执行动作。
- `scm-settlement`
  采购 / 销售结算服务，承接应付应收、对账、开票状态。
- `scm-report`
  报表与经营分析服务，承接库存报表、采购分析、周转分析。
- `scm-message`
  消息与通知服务，承接站内信、短信、邮件、Webhook。

## 当前已完成

### `scm-mdm`

- 已完成 `material` 第一版 CRUD。
- 支持统一返回体、统一异常处理、租户头透传。
- 持久化层同时演示两种 MyBatis 写法：
  `insert/update/delete` 写在 `Mapper` 注解上；
  `select` 写在 `resources/mapper/*.xml` 中。
- 默认可直接本地启动，使用内存 H2。
- 如需切换 MySQL，可启用 `mysql` profile。

### `scm-inventory`

- 已完成库存入库第一版 DDD 模型。
- 已落地应用层、领域层、基础设施层、接口层。
- 支持库存入库、库存余额查询、库存流水落库、基础幂等校验。

## 架构原则

- Spring Cloud Alibaba 路线
- 注册中心 / 配置中心使用 Nacos
- 三层服务使用 MyBatis
- DDD 服务使用 MyBatis-Plus
- 第一条核心业务链：
  采购订单 -> 收货 -> 入库 -> 库存增加 -> 库存流水

## Git 管理建议

建议把整个项目纳入 Git 管理，但不要提交以下内容：

- `.idea/`
- 各模块 `target/`
- 本地环境文件如 `.env`
- 运行日志和临时文件

仓库根目录已经补了 `.gitignore`。如果你确认现在就初始化，可以在项目根目录执行：

```bash
git init
git add .
git commit -m "init: bootstrap saas-wms-scm-demo"
```

## 启动说明

### 直接启动 `ScmMdmApplication`

默认配置现在使用 H2 内存库，可以直接在 IDE 中运行 `ScmMdmApplication`。

### 切换到 MySQL

如果你要连本地 Docker MySQL：

```bash
mvn -pl scm-mdm -am spring-boot:run -DskipTests -Dspring-boot.run.profiles=mysql
```

IDE 中则添加 `spring.profiles.active=mysql`。

## 下一步

1. 在 `scm-purchase` 中落地采购订单与收货单第一版模型
2. 打通 `purchase receipt -> inventory stock-in` 编排链路
3. 在 `inventory` 中继续补出库、锁库、库存调整
4. 补充接口测试与初始化演示数据
