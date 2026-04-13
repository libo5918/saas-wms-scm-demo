CREATE TABLE IF NOT EXISTS purchase_receipt (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    receipt_no VARCHAR(64) NOT NULL COMMENT '收货单号',
    purchase_order_id BIGINT NOT NULL COMMENT '采购订单ID',
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
