# Auth + Gateway + Nacos + SkyWalking 快速联调指南

## 1. 启动基础设施

```bash
cd /mnt/e/ideaProject/saas-wms-scm/deploy/docker-compose
docker compose -f infrastructure.yml up -d nacos skywalking-oap skywalking-ui redis mysql
docker compose -f infrastructure.yml ps
```

预期端口：
- Nacos 控制台：`http://localhost:18848/nacos`
- SkyWalking UI: `http://localhost:18088`
- SkyWalking OAP gRPC: `127.0.0.1:11800`

## 2. 启动业务服务

启动顺序建议：
1. `ScmAuthApplication`（端口 `18086`）
2. `ScmInventoryApplication`（端口 `18084`）
3. `ScmSalesApplication`（端口 `18085`）
4. `ScmGatewayApplication`（端口 `18080`）

网关路由已改为基于 Nacos 服务发现的 `lb://`。  
联调前请先确认 Nacos 中以下服务状态为 `UP`：
- `scm-auth`
- `scm-sales`
- `scm-inventory`
- `scm-mdm`

## 3. 验证登录接口（Auth）

```bash
curl -X POST "http://localhost:18086/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1" \
  -d '{"username":"admin","password":"admin123"}'
```

从返回结果中保存 `data.accessToken`。

## 4. 验证网关鉴权

不带 token（预期 `401`）：

```bash
curl -i "http://localhost:18080/api/v1/sales-orders"
```

带 token：

```bash
curl -i "http://localhost:18080/api/v1/sales-orders" \
  -H "Authorization: Bearer <accessToken>"
```

## 5. 启用 SkyWalking Java Agent（IDEA VM Options）

对每个服务 JVM（auth/sales/inventory/gateway）增加以下启动参数：

```text
-javaagent:E:\tools\skywalking-agent\skywalking-agent.jar
-Dskywalking.agent.service_name=scm-auth
-Dskywalking.collector.backend_service=127.0.0.1:11800
```

按服务分别调整 `service_name`：
- `scm-auth`
- `scm-gateway`
- `scm-sales`
- `scm-inventory`

修改后重启服务，并通过网关发起请求流量。

## 6. 在 SkyWalking UI 验证

打开 `http://localhost:18088`，检查：
- 服务列表能看到 4 个应用
- 拓扑图能看到 gateway -> 下游服务调用
- Trace 页面能按接口查询链路

## 7. 常见问题

1. `18080` 端口冲突：
   - gateway 使用 `18080`，SkyWalking UI 已改为 `18088`。
2. Nacos 页面访问：
   - 请在浏览器打开 URL，不要在 PowerShell 里直接输入 URL 当命令执行。
3. actuator `503`：
   - 常见原因是 Redis 不可达，请检查 Redis 容器状态与应用 Redis 配置。
