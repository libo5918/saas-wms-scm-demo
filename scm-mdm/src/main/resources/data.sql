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
