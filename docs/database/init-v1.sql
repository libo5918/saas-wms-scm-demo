-- saas-wms-scm 第一版表结构
-- 使用方式：先创建数据库，再按服务边界执行本文件中的 DDL

CREATE DATABASE IF NOT EXISTS scm_auth DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS scm_mdm DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS scm_purchase DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS scm_inventory DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE scm_auth;

CREATE TABLE IF NOT EXISTS sys_tenant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_code VARCHAR(64) NOT NULL,
    tenant_name VARCHAR(128) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_by BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT DEFAULT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_tenant_code (tenant_code)
);

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(64) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_by BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT DEFAULT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_tenant_username (tenant_id, username)
);

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(64) NOT NULL,
    created_by BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT DEFAULT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_tenant_role_code (tenant_id, role_code)
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_user_role (tenant_id, user_id, role_id)
);

USE scm_mdm;

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
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT DEFAULT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_tenant_material_code (tenant_id, material_code)
);

CREATE TABLE IF NOT EXISTS mdm_supplier (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    supplier_code VARCHAR(64) NOT NULL,
    supplier_name VARCHAR(128) NOT NULL,
    contact_name VARCHAR(64) DEFAULT NULL,
    contact_phone VARCHAR(32) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_by BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT DEFAULT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_tenant_supplier_code (tenant_id, supplier_code)
);

CREATE TABLE IF NOT EXISTS mdm_warehouse (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    warehouse_code VARCHAR(64) NOT NULL,
    warehouse_name VARCHAR(128) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_by BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT DEFAULT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_tenant_warehouse_code (tenant_id, warehouse_code)
);

CREATE TABLE IF NOT EXISTS mdm_location (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    location_code VARCHAR(64) NOT NULL,
    location_name VARCHAR(128) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_by BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT DEFAULT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_tenant_location_code (tenant_id, warehouse_id, location_code)
);

USE scm_purchase;

CREATE TABLE IF NOT EXISTS purchase_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    supplier_id BIGINT NOT NULL,
    order_status VARCHAR(32) NOT NULL,
    total_amount DECIMAL(18, 4) NOT NULL DEFAULT 0,
    remark VARCHAR(255) DEFAULT NULL,
    created_by BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT DEFAULT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_tenant_order_no (tenant_id, order_no)
);

CREATE TABLE IF NOT EXISTS purchase_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    purchase_order_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    plan_qty DECIMAL(18, 4) NOT NULL,
    received_qty DECIMAL(18, 4) NOT NULL DEFAULT 0,
    unit_price DECIMAL(18, 4) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS purchase_receipt (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    receipt_no VARCHAR(64) NOT NULL,
    purchase_order_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    receipt_status VARCHAR(32) NOT NULL,
    created_by BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT DEFAULT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_tenant_receipt_no (tenant_id, receipt_no)
);

CREATE TABLE IF NOT EXISTS purchase_receipt_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    purchase_receipt_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    receipt_qty DECIMAL(18, 4) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

USE scm_inventory;

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
