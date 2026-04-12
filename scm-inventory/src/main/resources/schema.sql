CREATE TABLE IF NOT EXISTS inventory_balance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    on_hand_qty DECIMAL(18, 4) NOT NULL DEFAULT 0,
    locked_qty DECIMAL(18, 4) NOT NULL DEFAULT 0,
    available_qty DECIMAL(18, 4) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT DEFAULT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_tenant_material_location (tenant_id, material_id, warehouse_id, location_id)
);

CREATE TABLE IF NOT EXISTS inventory_txn_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    txn_no VARCHAR(64) NOT NULL,
    biz_type VARCHAR(32) NOT NULL,
    biz_no VARCHAR(64) NOT NULL,
    material_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    txn_direction VARCHAR(16) NOT NULL,
    txn_qty DECIMAL(18, 4) NOT NULL,
    before_qty DECIMAL(18, 4) NOT NULL,
    after_qty DECIMAL(18, 4) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_txn_no (tenant_id, txn_no)
);
