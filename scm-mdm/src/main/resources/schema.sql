CREATE TABLE IF NOT EXISTS mdm_material (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    material_code VARCHAR(64) NOT NULL COMMENT '物料编码',
    material_name VARCHAR(128) NOT NULL COMMENT '物料名称',
    material_spec VARCHAR(255) DEFAULT NULL COMMENT '物料规格',
    unit VARCHAR(32) NOT NULL COMMENT '计量单位',
    material_type VARCHAR(32) NOT NULL COMMENT '物料类型',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
    created_by BIGINT DEFAULT NULL COMMENT '创建人',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT DEFAULT NULL COMMENT '更新人',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    CONSTRAINT uq_mdm_material_tenant_code UNIQUE (tenant_id, material_code),
    KEY idx_mdm_material_tenant_status (tenant_id, status)
) COMMENT='物料主数据表：存储租户下的物料基础档案';
