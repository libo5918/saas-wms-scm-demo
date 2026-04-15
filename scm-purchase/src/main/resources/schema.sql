CREATE TABLE IF NOT EXISTS purchase_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    order_no VARCHAR(64) NOT NULL COMMENT '采购订单号',
    supplier_id BIGINT NOT NULL COMMENT '供应商ID',
    order_status VARCHAR(32) NOT NULL COMMENT '采购订单状态',
    total_amount DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '订单总金额',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    created_by BIGINT DEFAULT NULL COMMENT '创建人',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT DEFAULT NULL COMMENT '更新人',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    CONSTRAINT uq_purchase_order_tenant_no UNIQUE (tenant_id, order_no),
    KEY idx_purchase_order_tenant_status (tenant_id, order_status)
) COMMENT='采购订单头表：记录一次采购下单的主单信息';

CREATE TABLE IF NOT EXISTS purchase_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    purchase_order_id BIGINT NOT NULL COMMENT '采购订单ID',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    plan_qty DECIMAL(18, 4) NOT NULL COMMENT '计划采购数量',
    received_qty DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '已收货数量',
    unit_price DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '采购单价',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_purchase_order_item_order (purchase_order_id),
    KEY idx_purchase_order_item_material (material_id)
) COMMENT='采购订单明细表：记录采购订单中的物料、数量和价格';

CREATE TABLE IF NOT EXISTS purchase_receipt (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    receipt_no VARCHAR(64) NOT NULL COMMENT '收货单号',
    purchase_order_id BIGINT NOT NULL COMMENT '采购订单ID',
    supplier_id BIGINT NOT NULL COMMENT '供应商ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    receipt_status VARCHAR(32) NOT NULL COMMENT '收货单状态',
    failure_reason VARCHAR(255) DEFAULT NULL COMMENT '入库失败原因',
    created_by BIGINT DEFAULT NULL COMMENT '创建人',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT DEFAULT NULL COMMENT '更新人',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    CONSTRAINT uq_purchase_receipt_tenant_no UNIQUE (tenant_id, receipt_no),
    KEY idx_purchase_receipt_tenant_status (tenant_id, receipt_status),
    KEY idx_purchase_receipt_order (purchase_order_id)
) COMMENT='采购收货单头表：记录一次采购到货的单据信息';

CREATE TABLE IF NOT EXISTS purchase_receipt_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    purchase_receipt_id BIGINT NOT NULL COMMENT '收货单ID',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    location_id BIGINT NOT NULL COMMENT '库位ID',
    receipt_qty DECIMAL(18, 4) NOT NULL COMMENT '收货数量',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_purchase_receipt_item_receipt (purchase_receipt_id),
    KEY idx_purchase_receipt_item_material (material_id)
) COMMENT='采购收货单明细表：记录收货单中的物料、库位和数量';

-- 兼容升级：当历史库缺少 supplier_id 时，自动补列，避免启动时 Unknown column 报错。
SET @purchase_order_supplier_id_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'purchase_order'
      AND COLUMN_NAME = 'supplier_id'
);
SET @purchase_order_supplier_id_ddl := IF(
    @purchase_order_supplier_id_exists = 0,
    'ALTER TABLE purchase_order ADD COLUMN supplier_id BIGINT NOT NULL DEFAULT 0 COMMENT ''供应商ID'' AFTER order_no',
    'SELECT 1'
);
PREPARE stmt_po_supplier_id FROM @purchase_order_supplier_id_ddl;
EXECUTE stmt_po_supplier_id;
DEALLOCATE PREPARE stmt_po_supplier_id;

SET @purchase_receipt_supplier_id_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'purchase_receipt'
      AND COLUMN_NAME = 'supplier_id'
);
SET @purchase_receipt_supplier_id_ddl := IF(
    @purchase_receipt_supplier_id_exists = 0,
    'ALTER TABLE purchase_receipt ADD COLUMN supplier_id BIGINT NOT NULL DEFAULT 0 COMMENT ''供应商ID'' AFTER purchase_order_id',
    'SELECT 1'
);
PREPARE stmt_pr_supplier_id FROM @purchase_receipt_supplier_id_ddl;
EXECUTE stmt_pr_supplier_id;
DEALLOCATE PREPARE stmt_pr_supplier_id;
