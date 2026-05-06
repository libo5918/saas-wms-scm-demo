# SkyWalking 接入说明

当前仓库的基础设施编排已经包含 SkyWalking：

- OAP: `deploy/docker-compose/infrastructure.yml` 中的 `skywalking-oap`
- UI: `deploy/docker-compose/infrastructure.yml` 中的 `skywalking-ui`
- 默认端口：
  - gRPC: `11800`
  - HTTP: `12800`
  - UI: `18088`

这次接入补的是应用侧 Java agent，不改业务代码，直接对现有 Spring Boot 服务做自动埋点。

## 已补充内容

- `deploy/skywalking/download-agent.ps1`
  - 下载并解压 SkyWalking Java agent
- `deploy/skywalking/start-service-with-skywalking.ps1`
  - 构建并以 `-javaagent` 方式启动单个服务
- `.gitignore`
  - 忽略本地下载的 agent 和运行日志

## 版本选择

- 当前基础设施使用 `apache/skywalking-oap-server:10.2.0`
- Java agent 默认使用 `9.6.0`

这样搭配的原因：

1. 仓库使用 `Java 21`
2. SkyWalking 官方 Java agent 文档说明 agent 支持 `JDK 8 - 25`
3. 当前仓库有 `Spring Gateway`，SkyWalking 官方在 `9.4.0` 之后已明确支持 Spring Gateway tracing，`9.6.0` 更稳
4. `Spring Cloud Gateway` 插件属于 SkyWalking `optional-plugins`，启动 `scm-gateway` 时脚本会自动挂载 `apm-spring-cloud-gateway-4.x`

## 1. 启动基础设施

在 `deploy/docker-compose` 目录执行：

```powershell
docker compose -f infrastructure.yml up -d
```

确认 SkyWalking UI 可访问：

- `http://127.0.0.1:18088`

## 2. 启动带 SkyWalking agent 的服务

### 示例：启动网关

```powershell
.\deploy\skywalking\start-service-with-skywalking.ps1 -Module scm-gateway
```

### 示例：启动认证服务

```powershell
.\deploy\skywalking\start-service-with-skywalking.ps1 -Module scm-auth
```

### 指定 OAP 地址

```powershell
.\deploy\skywalking\start-service-with-skywalking.ps1 -Module scm-sales -OapAddress 127.0.0.1:11800
```

### 复用已有构建产物

```powershell
.\deploy\skywalking\start-service-with-skywalking.ps1 -Module scm-mdm -SkipBuild
```

### 只看实际启动命令

```powershell
.\deploy\skywalking\start-service-with-skywalking.ps1 -Module scm-auth -SkipBuild -DryRun
```

### 一键重启多个服务

默认会重启：

- `scm-auth`
- `scm-gateway`
- `scm-mdm`
- `scm-inventory`
- `scm-sales`

```powershell
.\deploy\skywalking\restart-services-with-skywalking.ps1
```

如果已经有构建产物，可跳过构建：

```powershell
.\deploy\skywalking\restart-services-with-skywalking.ps1 -SkipBuild
```

只重启部分模块：

```powershell
.\deploy\skywalking\restart-services-with-skywalking.ps1 -Modules scm-gateway,scm-sales,scm-inventory
```

## 3. 建议先接入哪些服务

建议先跑这 4 个：

1. `scm-gateway`
2. `scm-auth`
3. `scm-sales`
4. `scm-inventory`

这样能先看到：

- 登录请求链路
- 网关转发链路
- `sales -> inventory` 服务调用链路
- Kafka / JDBC / Spring Web 的自动埋点结果

## 额外说明：Gateway 插件

SkyWalking 官方文档说明，`Spring Cloud Gateway 2.x / 3.x / 4.x` 插件在 `optional-plugins` 目录中，不会默认启用。

本仓库的 `start-service-with-skywalking.ps1` 已经对 `scm-gateway` 做了自动处理：

- 从 `optional-plugins` 中挂载 `apm-spring-cloud-gateway-4.x-plugin`
- 通过 `-Dskywalking.plugin.mount=...` 启用该插件

如果你修改了脚本后已经启动过 `scm-gateway`，需要重启一次网关进程，新的 tracing 才会生效。

## 4. 验证方式

启动服务后，访问 SkyWalking UI：

- `http://127.0.0.1:18088`

然后发起一次真实请求，例如：

```text
POST /api/v1/auth/login
POST /api/v1/sales-orders
POST /api/v1/sales-orders/{id}/ship
```

在 UI 中重点看：

- `Services`
- `Traces`
- `Topology`

如果链路正常，你会看到：

- `scm-gateway`
- `scm-auth`
- `scm-sales`
- `scm-inventory`

以及它们之间的调用关系。

## 5. 日志位置

agent 日志默认输出到：

```text
output/skywalking/<service-name>
```

例如：

```text
output/skywalking/scm-gateway
```

## 6. 当前方案边界

当前是第一阶段接入，只做自动埋点和链路追踪，暂时不做下面这些增强：

- traceId 注入业务日志格式
- 自定义埋点注解
- 告警规则
- OAP 持久化改到 MySQL / Elasticsearch
- 生产环境容器注入 agent

这些都可以在链路跑通后再补。

## 7. 后续建议

如果要把 SkyWalking 真正用于联调和排障，下一步建议继续做：

1. 把 `gateway/auth/sales/inventory` 固定成带 agent 的标准启动方式
2. 给日志格式补 traceId
3. 把 OAP 存储从 `H2` 改成 `MySQL` 或 `Elasticsearch`
4. 给关键业务动作补手工埋点

## 参考

官方文档：

- Java agent setup: https://skywalking.apache.org/docs/skywalking-java/next/en/setup/service-agent/java-agent/readme/
- Java agent setting override: https://skywalking.apache.org/docs/skywalking-java/v9.5.0/en/setup/service-agent/java-agent/setting-override/
- Optional plugins: https://skywalking.apache.org/docs/skywalking-java/latest/en/setup/service-agent/java-agent/optional-plugins/
- Spring Gateway plugin: https://skywalking.apache.org/docs/skywalking-java/v9.5.0/en/setup/service-agent/java-agent/agent-optional-plugins/spring-gateway/
- SkyWalking downloads: https://skywalking.apache.org/downloads/
- Backend docker setup: https://skywalking.apache.org/docs/main/v10.0.0/en/setup/backend/backend-docker/
