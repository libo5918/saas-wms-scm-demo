CREATE TABLE IF NOT EXISTS inventory_balance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    location_id BIGINT NOT NULL COMMENT '库位ID',
    on_hand_qty DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '现存数量',
    locked_qty DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '锁定数量',
    available_qty DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '可用数量',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '版本号',
    created_by BIGINT DEFAULT NULL COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT DEFAULT NULL COMMENT '更新人',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    CONSTRAINT uq_inventory_balance_tenant_material_wh_loc UNIQUE (tenant_id, material_id, warehouse_id, location_id),
    KEY idx_inventory_balance_tenant_material (tenant_id, material_id)
) COMMENT='库存余额表：记录物料在仓库库位维度上的当前库存数量';

CREATE TABLE IF NOT EXISTS inventory_txn_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    txn_no VARCHAR(64) NOT NULL COMMENT '流水号',
    biz_type VARCHAR(32) NOT NULL COMMENT '业务类型',
    biz_no VARCHAR(64) NOT NULL COMMENT '业务单号',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    location_id BIGINT NOT NULL COMMENT '库位ID',
    txn_direction VARCHAR(16) NOT NULL COMMENT '出入库方向',
    txn_qty DECIMAL(18, 4) NOT NULL COMMENT '变动数量',
    before_qty DECIMAL(18, 4) NOT NULL COMMENT '变动前数量',
    after_qty DECIMAL(18, 4) NOT NULL COMMENT '变动后数量',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT uq_inventory_txn_record_tenant_txn_no UNIQUE (tenant_id, txn_no),
    KEY idx_inventory_txn_record_biz (tenant_id, biz_type, biz_no),
    KEY idx_inventory_txn_record_material (tenant_id, material_id)
) COMMENT='库存流水表：记录每次库存变动的业务来源和前后数量快照';
