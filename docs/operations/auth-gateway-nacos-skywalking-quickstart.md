# Auth + Gateway + Nacos + SkyWalking Quickstart

## 1. Start infrastructure

```bash
cd /mnt/e/ideaProject/saas-wms-scm/deploy/docker-compose
docker compose -f infrastructure.yml up -d nacos skywalking-oap skywalking-ui redis mysql
docker compose -f infrastructure.yml ps
```

Expected ports:
- Nacos console: `http://localhost:18848/nacos`
- SkyWalking UI: `http://localhost:18088`
- SkyWalking OAP gRPC: `127.0.0.1:11800`

## 2. Start business services

Start:
1. `ScmAuthApplication` (port `18086`)
2. `ScmInventoryApplication` (port `18084`)
3. `ScmSalesApplication` (port `18085`)
4. `ScmGatewayApplication` (port `18080`)

## 3. Verify auth login

```bash
curl -X POST "http://localhost:18086/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1" \
  -d '{"username":"admin","password":"admin123"}'
```

Save `data.accessToken` from response.

## 4. Verify gateway token guard

Without token (should be `401`):

```bash
curl -i "http://localhost:18080/api/v1/sales-orders"
```

With token:

```bash
curl -i "http://localhost:18080/api/v1/sales-orders" \
  -H "Authorization: Bearer <accessToken>"
```

## 5. Enable SkyWalking Java agent (IDEA VM options)

For each service JVM (auth/sales/inventory/gateway), add:

```text
-javaagent:E:\tools\skywalking-agent\skywalking-agent.jar
-Dskywalking.agent.service_name=scm-auth
-Dskywalking.collector.backend_service=127.0.0.1:11800
```

Adjust `service_name` per app:
- `scm-auth`
- `scm-gateway`
- `scm-sales`
- `scm-inventory`

Then restart services and generate traffic through gateway.

## 6. Verify in SkyWalking UI

Open `http://localhost:18088`, check:
- Services list shows the 4 apps
- Topology shows gateway -> downstream call graph
- Traces can be queried by endpoint

## 7. Common issues

1. Port conflict on `18080`:
   - gateway uses `18080`, SkyWalking UI uses `18088` now.
2. Nacos page access:
   - use browser URL, not directly typing URL in PowerShell command line.
3. Health `503` from actuator:
   - often caused by Redis unreachable; verify Redis container and app redis config.
