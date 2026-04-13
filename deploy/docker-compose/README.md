# 本地基础设施启动说明

当前文件：`infrastructure.yml`

## 启动

```bash
cd /mnt/e/ideaProject/saas-wms-scm/deploy/docker-compose
docker compose -f infrastructure.yml up -d
```

## 查看状态

```bash
docker compose -f infrastructure.yml ps
```

## 查看日志

```bash
docker compose -f infrastructure.yml logs mysql --tail 100
docker compose -f infrastructure.yml logs nacos --tail 100
docker compose -f infrastructure.yml logs kafka --tail 100
docker compose -f infrastructure.yml logs skywalking-oap --tail 100
```

## 停止

```bash
docker compose -f infrastructure.yml down
```

## 端口约定

- MySQL: `13306`
- Redis: `16379`
- Nacos: `18848`
- Kafka: `19092`
- SkyWalking OAP gRPC: `11800`
- SkyWalking OAP HTTP: `12800`
- SkyWalking UI: `18080`
