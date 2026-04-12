# 数据库脚本说明

当前初始化脚本：`init-v1.sql`

## 第一版数据库拆分

- `scm_auth`
- `scm_mdm`
- `scm_purchase`
- `scm_inventory`

## 执行方式

可以先启动本地 MySQL 容器，然后执行：

```bash
mysql -h 127.0.0.1 -P 13306 -uroot -p123456 < init-v1.sql
```

如果要分别执行，也可以登录 MySQL 后按数据库分段执行。
