# 当前认证方案说明

## 1. 文档目的

本文档描述 `saas-wms-scm` 当前已经落地的认证基座实现，覆盖以下模块：

- `scm-auth`
- `scm-gateway`
- `scm-common-security`
- 依赖网关透传租户头的业务服务

目标是让团队清楚当前认证链路已经做到什么程度、如何联调、有哪些边界，以及下一步该如何演进。

## 2. 方案概览

当前认证方案已经从纯骨架推进为可联调的第一版认证基座，整体模式如下：

1. `scm-auth` 负责登录与 JWT 签发
2. `scm-gateway` 负责 JWT 校验与身份透传
3. 下游业务服务继续使用 `X-Tenant-Id` 作为租户上下文入口

这套实现的定位不是最终商用形态，而是为后续的 RBAC、租户治理、网关统一收口提供一套稳定底座。

## 3. 模块职责

### 3.1 `scm-auth`

负责认证接口，当前已提供：

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `GET /api/v1/auth/me`
- `POST /api/v1/auth/logout`

当前登录数据源还是配置驱动的 demo 用户：

- 用户名：`admin`
- 密码：`admin123`
- 租户：`1`
- 用户：`10001`
- 角色：`ROLE_ADMIN,ROLE_TENANT_ADMIN`

JWT 签发与解析逻辑统一走 `AuthServiceImpl -> JwtTokenProvider`。

### 3.2 `scm-gateway`

负责统一认证入口。

除白名单接口外，网关要求请求头包含：

```text
Authorization: Bearer <token>
```

网关校验通过后，会把身份与租户信息透传到下游服务。

### 3.3 `scm-common-security`

负责公共安全能力封装，当前已沉淀：

- `JwtTokenProvider`
- `JwtTokenClaims`
- `GatewayHeaders`

这样做的目的是避免 `auth` 和 `gateway` 各自维护一套 JWT 逻辑，降低后续改造成本。

### 3.4 业务服务

当前 `mdm / purchase / inventory / sales` 仍然主要依赖 `X-Tenant-Id` 建立租户上下文。

服务内通过 `TenantHeaderInterceptor` 读取请求头，通过 `TenantContext` 写入线程上下文。

## 4. 接口设计

### 4.1 登录接口

```text
POST /api/v1/auth/login
```

请求体：

```json
{
  "username": "admin",
  "password": "admin123"
}
```

响应字段：

- `accessToken`
- `tokenType`
- `expiresAt`
- `tenantId`
- `userId`
- `username`
- `roles`

### 4.2 刷新接口

```text
POST /api/v1/auth/refresh
```

请求头：

```text
Authorization: Bearer <token>
```

行为说明：

- 解析旧 token 中的 `tenantId / userId / username / roles`
- 重新签发新的 access token

### 4.3 当前用户接口

```text
GET /api/v1/auth/me
```

请求头：

```text
Authorization: Bearer <token>
```

行为说明：

- 返回当前 token 对应的用户身份信息
- 返回字段包括 `tenantId / userId / username / roles`

### 4.4 登出接口

```text
POST /api/v1/auth/logout
```

当前行为：

- 只校验 token 合法性
- 不做 token 黑名单
- 不做持久化撤销

## 5. JWT 令牌设计

当前关键配置如下：

```text
issuer = scm-auth
expire-seconds = 7200
secret = change-this-dev-secret-key-at-least-32-bytes
```

当前 token 中包含的核心 claim：

- `tenantId`
- `userId`
- `username`
- `roles`
- `issuedAt`
- `expiration`

当前实现特点：

- `JwtTokenProvider` 强校验 `issuer`
- 缺少 `tenantId` 或 `userId` 时按 `401` 处理
- `secret` 长度要求不少于 32 个字符

## 6. 网关透传设计

当前白名单接口包括：

- `/api/v1/auth/login`
- `/actuator/**`

其它业务请求默认要求 Bearer Token。

令牌校验通过后，网关会向下游附加以下头：

```text
X-Tenant-Id
X-User-Id
X-User-Name
X-User-Roles
X-Gateway-Internal
X-Gateway-Secret
```

设计意图：

1. 统一由网关向下游传播身份上下文
2. 为后续“只允许网关内转”的收口方案预留接口
3. 为后续接入 RBAC 判定保留角色信息

## 7. 错误码与异常语义

当前公共错误码已经补齐：

- `400`：请求参数错误
- `401`：认证失败，例如 token 缺失、格式非法、签名错误
- `403`：访问被拒绝，预留给网关收口和权限不足场景
- `404`：资源不存在
- `500`：系统内部异常

## 8. 当前配置

### 8.1 `scm-auth`

当前关键配置位于 `scm-auth/src/main/resources/application.yml`：

```yaml
auth:
  jwt:
    issuer: scm-auth
    secret: change-this-dev-secret-key-at-least-32-bytes
    expire-seconds: 7200
  demo-user:
    username: admin
    password: admin123
    tenant-id: 1
    user-id: 10001
    roles: ROLE_ADMIN,ROLE_TENANT_ADMIN
```

### 8.2 `scm-gateway`

当前关键配置位于 `scm-gateway/src/main/resources/application.yml`：

```yaml
auth:
  jwt:
    issuer: scm-auth
    secret: change-this-dev-secret-key-at-least-32-bytes
    expire-seconds: 7200
security:
  gateway:
    internal-secret: scm-gateway-internal-dev-secret
```

## 9. 已完成验证

本轮已经通过以下测试验证主链路：

- `AuthServiceImplTest`
- `AuthControllerTest`
- `JwtAuthGlobalFilterTest`

执行命令：

```bash
mvn -pl scm-auth,scm-gateway -am test
```

验证范围：

1. 登录成功与错误凭证失败
2. token 刷新
3. 当前用户查询
4. 登出接口基础行为
5. 网关对白名单请求放行
6. 网关对缺失 Authorization 的请求返回 `401`
7. 网关对合法 token 正常透传身份头

## 10. 当前限制

这套方案还只是第一版基座，当前主要限制如下：

1. 用户来源还是配置，不是数据库
2. 没有用户、角色、权限、租户管理员的持久化模型
3. `logout` 还不是严格意义上的会话撤销
4. 下游服务还没有全部强制只接受网关内部转发
5. `roles` 已经透传，但尚未接入真正的 RBAC 判定

## 11. 后续建议

建议下一阶段按以下顺序推进：

1. 将 demo 用户升级为数据库版 `用户 / 角色 / 权限 / 租户管理员`
2. 增加 refresh token 或 token 黑名单能力，完善登出语义
3. 逐步启用下游服务“仅信任网关转发”的正式收口模式
4. 在网关或业务服务侧接入 RBAC 权限判定
5. 将租户生命周期、组织成员、套餐配额纳入统一身份体系

## 12. 关键源码位置

当前实现主要分布在以下文件：

- `scm-auth/src/main/java/com/example/scm/auth/controller/AuthController.java`
- `scm-auth/src/main/java/com/example/scm/auth/service/impl/AuthServiceImpl.java`
- `scm-common/scm-common-security/src/main/java/com/example/scm/common/security/JwtTokenProvider.java`
- `scm-common/scm-common-security/src/main/java/com/example/scm/common/security/GatewayHeaders.java`
- `scm-gateway/src/main/java/com/example/scm/gateway/filter/JwtAuthGlobalFilter.java`
- `scm-auth/src/main/resources/application.yml`
- `scm-gateway/src/main/resources/application.yml`
