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
) COMMENT='物料主数据表';

CREATE TABLE IF NOT EXISTS mdm_warehouse (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    warehouse_code VARCHAR(64) NOT NULL COMMENT '仓库编码',
    warehouse_name VARCHAR(128) NOT NULL COMMENT '仓库名称',
    warehouse_type VARCHAR(32) DEFAULT NULL COMMENT '仓库类型',
    contact_name VARCHAR(64) DEFAULT NULL COMMENT '联系人',
    contact_phone VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
    address VARCHAR(255) DEFAULT NULL COMMENT '仓库地址',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
    created_by BIGINT DEFAULT NULL COMMENT '创建人',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT DEFAULT NULL COMMENT '更新人',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    CONSTRAINT uq_mdm_warehouse_tenant_code UNIQUE (tenant_id, warehouse_code),
    KEY idx_mdm_warehouse_tenant_status (tenant_id, status)
) COMMENT='仓库主数据表';

CREATE TABLE IF NOT EXISTS mdm_location (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    warehouse_id BIGINT NOT NULL COMMENT '所属仓库ID',
    location_code VARCHAR(64) NOT NULL COMMENT '库位编码',
    location_name VARCHAR(128) NOT NULL COMMENT '库位名称',
    location_type VARCHAR(32) DEFAULT NULL COMMENT '库位类型',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
    created_by BIGINT DEFAULT NULL COMMENT '创建人',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT DEFAULT NULL COMMENT '更新人',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    CONSTRAINT uq_mdm_location_tenant_wh_code UNIQUE (tenant_id, warehouse_id, location_code),
    KEY idx_mdm_location_tenant_wh_status (tenant_id, warehouse_id, status)
) COMMENT='库位主数据表';
