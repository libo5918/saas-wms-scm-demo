# mdm 主数据模块说明

## 模块定位

`scm-mdm` 是整个项目的主数据服务，当前负责三类基础档案：

- 物料
- 供应商
- 仓库
- 库位

这类数据的特点是：

- 变化频率低于采购单、销售单、库存流水
- 以维护、查询、启停控制为主
- 更适合三层架构而不是复杂 DDD 建模

## 当前实现内容

- 物料新增、修改、详情、列表、逻辑删除
- 供应商新增、修改、详情、列表、逻辑删除
- 仓库新增、修改、详情、列表、逻辑删除
- 库位新增、修改、详情、列表、逻辑删除
- 租户隔离，通过 `X-Tenant-Id` 请求头区分租户
- 统一返回体
- 统一异常处理
- 参数校验
- 启动时自动执行 `schema.sql` 和 `data.sql`

## 当前接口

### 物料

- `POST /api/v1/materials`
- `PUT /api/v1/materials/{id}`
- `GET /api/v1/materials/{id}`
- `GET /api/v1/materials`
- `DELETE /api/v1/materials/{id}`

### 供应商

- `POST /api/v1/suppliers`
- `PUT /api/v1/suppliers/{id}`
- `GET /api/v1/suppliers/{id}`
- `GET /api/v1/suppliers`
- `DELETE /api/v1/suppliers/{id}`

### 仓库

- `POST /api/v1/warehouses`
- `PUT /api/v1/warehouses/{id}`
- `GET /api/v1/warehouses/{id}`
- `GET /api/v1/warehouses`
- `DELETE /api/v1/warehouses/{id}`

### 库位

- `POST /api/v1/locations`
- `PUT /api/v1/locations/{id}`
- `GET /api/v1/locations/{id}`
- `GET /api/v1/locations`
- `DELETE /api/v1/locations/{id}`

## 当前设计说明

### 为什么这里使用三层架构

主数据模块的规则复杂度不高，当前核心诉求是：

- CRUD
- 状态控制
- 基础唯一性校验
- 租户隔离

因此保留：

- Controller
- Service
- Mapper
- Entity / DTO / VO

这样实现简单，便于后续继续复制到更多主数据类型。

### 当前已体现的工程点

- DTO / Entity / VO 分离
- 逻辑删除
- 租户隔离
- 统一异常处理
- 参数校验
- 启动初始化 SQL

## 与业务服务的关系

当前 `scm-purchase`、`scm-inventory`、`scm-sales` 已经接入主数据校验：

- 物料必须存在且启用
- 仓库必须存在且启用
- 库位必须存在且启用
- 库位必须归属于传入仓库

也就是说，主数据现在不只是“维护档案”，而是已经参与业务入口拦截。

## 初始化数据

默认会初始化一批演示数据：

- 物料
  - `MAT-001` 螺丝
  - `MAT-002` 纸箱
  - `MAT-003` 成品测试件
- 供应商
  - `SUP-001` 默认供应商
  - `SUP-002` 备件供应商
- 仓库
  - `WH-001` 主仓
  - `WH-002` 备件仓
- 库位
  - `LOC-001` 成品区-A01
  - `LOC-002` 成品区-B01
  - `LOC-101` 备件区-A01

## 启动前准备

1. 启动本地 MySQL
2. 确保 `root / 123456` 可用，或者自行改配置
3. 启动 `scm-mdm`
4. Spring 会自动创建 `scm_mdm` 并执行初始化脚本

## 联调约定

请求头必须带：

```text
X-Tenant-Id: 1
```

### 创建仓库示例

```json
{
  "warehouseCode": "WH-003",
  "warehouseName": "电商仓",
  "warehouseType": "FINISHED",
  "contactName": "王五",
  "contactPhone": "13800000003",
  "address": "上海市浦东新区 3 号",
  "status": 1
}
```

### 创建库位示例

```json
{
  "warehouseId": 1,
  "locationCode": "LOC-003",
  "locationName": "成品区-C01",
  "locationType": "PICK",
  "status": 1
}
```
