INSERT INTO scm_mdm.mdm_material (
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

INSERT INTO scm_mdm.mdm_warehouse (
    id, tenant_id, warehouse_code, warehouse_name, warehouse_type, contact_name, contact_phone, address, status, created_by, updated_by, deleted
) VALUES
    (1, 1, 'WH-001', '主仓', 'FINISHED', '张三', '13800000001', '上海市浦东新区 1 号', 1, 1, 1, 0),
    (2, 1, 'WH-002', '备件仓', 'SPARE', '李四', '13800000002', '上海市浦东新区 2 号', 1, 1, 1, 0)
ON DUPLICATE KEY UPDATE
    warehouse_name = VALUES(warehouse_name),
    warehouse_type = VALUES(warehouse_type),
    contact_name = VALUES(contact_name),
    contact_phone = VALUES(contact_phone),
    address = VALUES(address),
    status = VALUES(status),
    updated_by = VALUES(updated_by),
    deleted = VALUES(deleted);

INSERT INTO scm_mdm.mdm_location (
    id, tenant_id, warehouse_id, location_code, location_name, location_type, status, created_by, updated_by, deleted
) VALUES
    (1, 1, 1, 'LOC-001', '成品区-A01', 'PICK', 1, 1, 1, 0),
    (2, 1, 1, 'LOC-002', '成品区-B01', 'STORAGE', 1, 1, 1, 0),
    (3, 1, 2, 'LOC-101', '备件区-A01', 'STORAGE', 1, 1, 1, 0)
ON DUPLICATE KEY UPDATE
    location_name = VALUES(location_name),
    location_type = VALUES(location_type),
    status = VALUES(status),
    updated_by = VALUES(updated_by),
    deleted = VALUES(deleted);
