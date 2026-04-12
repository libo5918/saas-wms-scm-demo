# mdm-material 模块说明

## 模块定位

`mdm-material` 是主数据服务中的物料模块，负责管理企业级 SaaS 项目中的物料主数据。

主数据特点：

- 变化频率相对交易单据更低
- 以维护、查询、状态控制为主
- 更适合三层架构而不是 DDD 充血模型

## 当前实现内容

- 物料新增
- 物料修改
- 物料详情查询
- 物料列表查询
- 物料逻辑删除
- 租户隔离（通过 `X-Tenant-Id` 请求头）
- 统一返回体
- 统一异常处理

## 当前接口

- `POST /api/v1/materials`
- `PUT /api/v1/materials/{id}`
- `GET /api/v1/materials/{id}`
- `GET /api/v1/materials`
- `DELETE /api/v1/materials/{id}`

## 当前设计说明

### 为什么这里使用三层架构

物料模块本质上是主数据管理模块，规则复杂度不高，绝大多数场景是 CRUD + 状态控制。

因此这一类服务更适合：

- Controller
- Service
- Mapper
- Entity / DTO / VO

这样结构清晰、实现成本低、维护简单。

### 当前已体现的工程点

- DTO / Entity / VO 分离
- 逻辑删除
- 租户隔离
- 统一异常处理
- 参数校验

### 后续可增强点

- 物料分类树
- 物料状态机
- 物料扩展属性模型
- Redis 缓存
- 基于模板方法或策略模式的主数据校验器
- 操作审计日志

## 启动前准备

1. 先启动基础设施
2. 初始化数据库脚本
3. 确保 `scm_mdm` 数据库存在
4. 确保已插入测试租户数据

## 当前测试方式

请求头必须带：

```text
X-Tenant-Id: 1
```

示例新增请求：

```json
{
  "materialCode": "MAT-001",
  "materialName": "螺丝",
  "materialSpec": "M6*30",
  "unit": "PCS",
  "materialType": "RAW",
  "status": 1
}
```
