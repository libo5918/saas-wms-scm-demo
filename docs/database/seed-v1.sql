USE scm_auth;

INSERT INTO sys_tenant(tenant_code, tenant_name, status, created_by, updated_by, deleted)
VALUES ('TENANT_DEFAULT', '默认租户', 1, 1, 1, 0)
ON DUPLICATE KEY UPDATE tenant_name = VALUES(tenant_name), status = VALUES(status), updated_by = VALUES(updated_by);

INSERT INTO sys_user(tenant_id, username, password, nickname, status, created_by, updated_by, deleted)
SELECT 1, 'admin', '{noop}123456', '系统管理员', 1, 1, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_user WHERE tenant_id = 1 AND username = 'admin'
);

USE scm_mdm;

INSERT INTO mdm_material (
    id, tenant_id, material_code, material_name, material_spec, unit, material_type, status, created_by, updated_by, deleted
) VALUES
    (1, 1, 'MAT-001', '螺丝', 'M8*30', 'PCS', 'RAW', 1, 1, 1, 0),
    (2, 1, 'MAT-002', '纸箱', '60X40X40', 'PCS', 'PACK', 1, 1, 1, 0),
    (3, 1, 'MAT-003', '成品测试件', 'STANDARD', 'PCS', 'FG', 1, 1, 1, 0)
ON DUPLICATE KEY UPDATE
    material_name = VALUES(material_name),
    material_spec = VALUES(material_spec),
    unit = VALUES(unit),
    material_type = VALUES(material_type),
    status = VALUES(status),
    updated_by = VALUES(updated_by),
    deleted = VALUES(deleted);

INSERT INTO mdm_supplier (
    id, tenant_id, supplier_code, supplier_name, contact_name, contact_phone, status, created_by, updated_by, deleted
) VALUES
    (1, 1, 'SUP-001', '默认供应商', '王五', '13900000001', 1, 1, 1, 0),
    (2, 1, 'SUP-002', '备件供应商', '赵六', '13900000002', 1, 1, 1, 0)
ON DUPLICATE KEY UPDATE
    supplier_name = VALUES(supplier_name),
    contact_name = VALUES(contact_name),
    contact_phone = VALUES(contact_phone),
    status = VALUES(status),
    updated_by = VALUES(updated_by),
    deleted = VALUES(deleted);

INSERT INTO mdm_warehouse (
    id, tenant_id, warehouse_code, warehouse_name, warehouse_type, contact_name, contact_phone, address, status, created_by, updated_by, deleted
) VALUES
    (1, 1, 'WH-001', '主仓', 'FINISHED', '张三', '13800000001', '上海市浦东新区1号', 1, 1, 1, 0),
    (2, 1, 'WH-002', '备件仓', 'SPARE', '李四', '13800000002', '上海市浦东新区2号', 1, 1, 1, 0)
ON DUPLICATE KEY UPDATE
    warehouse_name = VALUES(warehouse_name),
    warehouse_type = VALUES(warehouse_type),
    contact_name = VALUES(contact_name),
    contact_phone = VALUES(contact_phone),
    address = VALUES(address),
    status = VALUES(status),
    updated_by = VALUES(updated_by),
    deleted = VALUES(deleted);

INSERT INTO mdm_location (
    id, tenant_id, warehouse_id, location_code, location_name, location_type, status, created_by, updated_by, deleted
) VALUES
    (1, 1, 1, 'LOC-001', '成品区A01', 'PICK', 1, 1, 1, 0),
    (2, 1, 1, 'LOC-002', '成品区B01', 'STORAGE', 1, 1, 1, 0),
    (3, 1, 2, 'LOC-101', '备件区A01', 'STORAGE', 1, 1, 1, 0)
ON DUPLICATE KEY UPDATE
    location_name = VALUES(location_name),
    location_type = VALUES(location_type),
    status = VALUES(status),
    updated_by = VALUES(updated_by),
    deleted = VALUES(deleted);

USE scm_purchase;

INSERT INTO purchase_order (
    id, tenant_id, order_no, supplier_id, order_status, total_amount, remark, created_by, updated_by, deleted
) VALUES
    (1, 1, 'PO-001', 1, 'CREATED', 300.0000, '首单采购', 1, 1, 0),
    (2, 1, 'PO-002', 2, 'CREATED', 150.0000, '备件采购', 1, 1, 0)
ON DUPLICATE KEY UPDATE
    supplier_id = VALUES(supplier_id),
    order_status = VALUES(order_status),
    total_amount = VALUES(total_amount),
    remark = VALUES(remark),
    updated_by = VALUES(updated_by),
    deleted = VALUES(deleted);

INSERT INTO purchase_order_item (
    id, tenant_id, purchase_order_id, material_id, plan_qty, received_qty, unit_price
) VALUES
    (11, 1, 1, 1, 20.0000, 0.0000, 10.0000),
    (12, 1, 1, 2, 10.0000, 0.0000, 10.0000),
    (13, 1, 2, 3, 5.0000, 0.0000, 30.0000)
ON DUPLICATE KEY UPDATE
    purchase_order_id = VALUES(purchase_order_id),
    material_id = VALUES(material_id),
    plan_qty = VALUES(plan_qty),
    received_qty = VALUES(received_qty),
    unit_price = VALUES(unit_price);

INSERT INTO purchase_receipt (
    id, tenant_id, receipt_no, purchase_order_id, supplier_id, warehouse_id, receipt_status, failure_reason, created_by, updated_by, deleted
) VALUES
    (1, 1, 'RCV-001', 1, 1, 2001, 'STOCK_IN_SUCCESS', NULL, 1, 1, 0),
    (2, 1, 'RCV-002', 2, 2, 2001, 'STOCK_IN_SUCCESS', NULL, 1, 1, 0)
ON DUPLICATE KEY UPDATE
    purchase_order_id = VALUES(purchase_order_id),
    supplier_id = VALUES(supplier_id),
    warehouse_id = VALUES(warehouse_id),
    receipt_status = VALUES(receipt_status),
    failure_reason = VALUES(failure_reason),
    updated_by = VALUES(updated_by),
    deleted = VALUES(deleted);

INSERT INTO purchase_receipt_item (
    id, tenant_id, purchase_receipt_id, material_id, location_id, receipt_qty
) VALUES
    (11, 1, 1, 1, 3001, 20.0000),
    (12, 1, 1, 2, 3001, 10.0000),
    (13, 1, 2, 3, 3002, 5.0000)
ON DUPLICATE KEY UPDATE
    purchase_receipt_id = VALUES(purchase_receipt_id),
    material_id = VALUES(material_id),
    location_id = VALUES(location_id),
    receipt_qty = VALUES(receipt_qty);
