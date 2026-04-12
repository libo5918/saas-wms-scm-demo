CREATE TABLE IF NOT EXISTS mdm_material (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    material_code VARCHAR(64) NOT NULL,
    material_name VARCHAR(128) NOT NULL,
    material_spec VARCHAR(255) DEFAULT NULL,
    unit VARCHAR(32) NOT NULL,
    material_type VARCHAR(32) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_by BIGINT DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT DEFAULT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, material_code)
);
