CREATE TABLE IF NOT EXISTS sales_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    order_no VARCHAR(64) NOT NULL COMMENT '销售单号',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    order_status VARCHAR(32) NOT NULL COMMENT '销售单状态',
    failure_reason VARCHAR(255) DEFAULT NULL COMMENT '库存联动失败原因',
    created_by BIGINT DEFAULT NULL COMMENT '创建人',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT DEFAULT NULL COMMENT '更新人',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    CONSTRAINT uq_sales_order_tenant_no UNIQUE (tenant_id, order_no),
    KEY idx_sales_order_tenant_status (tenant_id, order_status)
) COMMENT='销售订单头表：记录一次销售下单与库存联动状态';

CREATE TABLE IF NOT EXISTS sales_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    sales_order_id BIGINT NOT NULL COMMENT '销售单ID',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    location_id BIGINT NOT NULL COMMENT '库位ID',
    sale_qty DECIMAL(18, 4) NOT NULL COMMENT '销售数量',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_sales_order_item_order (sales_order_id),
    KEY idx_sales_order_item_material (material_id)
) COMMENT='销售订单明细表：记录销售单中的物料、库位和数量';
