# 第一版表设计

## 认证域
- `sys_tenant`
  - 租户档案
- `sys_user`
  - 用户档案
- `sys_role`
  - 角色档案
- `sys_user_role`
  - 用户角色关系

## 主数据域
- `mdm_material`
  - 物料主数据
  - 关键字段：`material_code`、`material_name`、`material_spec`、`unit`、`material_type`、`status`
- `mdm_supplier`
  - 供应商主数据
  - 关键字段：`supplier_code`、`supplier_name`、`contact_name`、`contact_phone`、`status`
- `mdm_warehouse`
  - 仓库主数据
  - 关键字段：`warehouse_code`、`warehouse_name`、`warehouse_type`、`contact_name`、`contact_phone`、`address`、`status`
- `mdm_location`
  - 库位主数据
  - 关键字段：`warehouse_id`、`location_code`、`location_name`、`location_type`、`status`

## 采购域
- `purchase_order`
  - 采购订单头表
  - 关键字段：`order_no`、`supplier_id`、`order_status`、`total_amount`、`remark`
- `purchase_order_item`
  - 采购订单明细表
  - 关键字段：`purchase_order_id`、`material_id`、`plan_qty`、`received_qty`、`unit_price`
- `purchase_receipt`
  - 采购收货单
  - 关键字段：`receipt_no`、`purchase_order_id`、`supplier_id`、`warehouse_id`、`receipt_status`、`failure_reason`
- `purchase_receipt_item`
  - 采购收货单明细
  - 关键字段：`purchase_receipt_id`、`material_id`、`location_id`、`receipt_qty`

## 库存域
- `inventory_balance`
  - 库存余额
  - 关键字段：`material_id`、`warehouse_id`、`location_id`、`on_hand_qty`、`locked_qty`、`available_qty`
- `inventory_txn_record`
  - 库存流水
  - 关键字段：`txn_no`、`biz_type`、`biz_no`、`txn_direction`、`txn_qty`、`before_qty`、`after_qty`

## 销售域
- `sales_order`
  - 销售订单
  - 关键字段：`order_no`、`warehouse_id`、`order_status`、`failure_reason`
- `sales_order_item`
  - 销售订单明细
  - 关键字段：`sales_order_id`、`material_id`、`location_id`、`sale_qty`
