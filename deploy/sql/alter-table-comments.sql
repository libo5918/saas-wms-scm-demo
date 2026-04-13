USE scm_mdm;

ALTER TABLE mdm_material COMMENT = '物料主数据表：存储租户下的物料基础档案';
ALTER TABLE mdm_material
    MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN tenant_id BIGINT NOT NULL COMMENT '租户ID',
    MODIFY COLUMN material_code VARCHAR(64) NOT NULL COMMENT '物料编码',
    MODIFY COLUMN material_name VARCHAR(128) NOT NULL COMMENT '物料名称',
    MODIFY COLUMN material_spec VARCHAR(255) DEFAULT NULL COMMENT '物料规格',
    MODIFY COLUMN unit VARCHAR(32) NOT NULL COMMENT '计量单位',
    MODIFY COLUMN material_type VARCHAR(32) NOT NULL COMMENT '物料类型',
    MODIFY COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
    MODIFY COLUMN created_by BIGINT DEFAULT NULL COMMENT '创建人',
    MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_by BIGINT DEFAULT NULL COMMENT '更新人',
    MODIFY COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    MODIFY COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记';
ALTER TABLE mdm_material DROP INDEX tenant_id, ADD CONSTRAINT uq_mdm_material_tenant_code UNIQUE (tenant_id, material_code);
ALTER TABLE mdm_material ADD INDEX idx_mdm_material_tenant_status (tenant_id, status);

USE scm_purchase;

ALTER TABLE purchase_receipt
    ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(255) DEFAULT NULL COMMENT '入库失败原因' AFTER receipt_status;
ALTER TABLE purchase_receipt COMMENT = '采购收货单头表：记录一次采购到货的单据信息';
ALTER TABLE purchase_receipt
    MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN tenant_id BIGINT NOT NULL COMMENT '租户ID',
    MODIFY COLUMN receipt_no VARCHAR(64) NOT NULL COMMENT '收货单号',
    MODIFY COLUMN purchase_order_id BIGINT NOT NULL COMMENT '采购订单ID',
    MODIFY COLUMN warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    MODIFY COLUMN receipt_status VARCHAR(32) NOT NULL COMMENT '收货单状态',
    MODIFY COLUMN failure_reason VARCHAR(255) DEFAULT NULL COMMENT '入库失败原因',
    MODIFY COLUMN created_by BIGINT DEFAULT NULL COMMENT '创建人',
    MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_by BIGINT DEFAULT NULL COMMENT '更新人',
    MODIFY COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    MODIFY COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记';
ALTER TABLE purchase_receipt DROP INDEX tenant_id, ADD CONSTRAINT uq_purchase_receipt_tenant_no UNIQUE (tenant_id, receipt_no);
ALTER TABLE purchase_receipt ADD INDEX idx_purchase_receipt_tenant_status (tenant_id, receipt_status);
ALTER TABLE purchase_receipt ADD INDEX idx_purchase_receipt_order (purchase_order_id);

ALTER TABLE purchase_receipt_item COMMENT = '采购收货单明细表：记录收货单中的物料、库位和数量';
ALTER TABLE purchase_receipt_item
    MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN tenant_id BIGINT NOT NULL COMMENT '租户ID',
    MODIFY COLUMN purchase_receipt_id BIGINT NOT NULL COMMENT '收货单ID',
    MODIFY COLUMN material_id BIGINT NOT NULL COMMENT '物料ID',
    MODIFY COLUMN location_id BIGINT NOT NULL COMMENT '库位ID',
    MODIFY COLUMN receipt_qty DECIMAL(18, 4) NOT NULL COMMENT '收货数量',
    MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE purchase_receipt_item ADD INDEX idx_purchase_receipt_item_receipt (purchase_receipt_id);
ALTER TABLE purchase_receipt_item ADD INDEX idx_purchase_receipt_item_material (material_id);

USE scm_inventory;

ALTER TABLE inventory_balance COMMENT = '库存余额表：记录物料在仓库库位维度上的当前库存数量';
ALTER TABLE inventory_balance
    MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN tenant_id BIGINT NOT NULL COMMENT '租户ID',
    MODIFY COLUMN material_id BIGINT NOT NULL COMMENT '物料ID',
    MODIFY COLUMN warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    MODIFY COLUMN location_id BIGINT NOT NULL COMMENT '库位ID',
    MODIFY COLUMN on_hand_qty DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '现存数量',
    MODIFY COLUMN locked_qty DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '锁定数量',
    MODIFY COLUMN available_qty DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '可用数量',
    MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '版本号',
    MODIFY COLUMN created_by BIGINT DEFAULT NULL COMMENT '创建人',
    MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_by BIGINT DEFAULT NULL COMMENT '更新人',
    MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    MODIFY COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记';
ALTER TABLE inventory_balance RENAME INDEX uk_tenant_material_location TO uq_inventory_balance_tenant_material_wh_loc;
ALTER TABLE inventory_balance ADD INDEX idx_inventory_balance_tenant_material (tenant_id, material_id);

ALTER TABLE inventory_txn_record COMMENT = '库存流水表：记录每次库存变动的业务来源和前后数量快照';
ALTER TABLE inventory_txn_record
    MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN tenant_id BIGINT NOT NULL COMMENT '租户ID',
    MODIFY COLUMN txn_no VARCHAR(64) NOT NULL COMMENT '流水号',
    MODIFY COLUMN biz_type VARCHAR(32) NOT NULL COMMENT '业务类型',
    MODIFY COLUMN biz_no VARCHAR(64) NOT NULL COMMENT '业务单号',
    MODIFY COLUMN material_id BIGINT NOT NULL COMMENT '物料ID',
    MODIFY COLUMN warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    MODIFY COLUMN location_id BIGINT NOT NULL COMMENT '库位ID',
    MODIFY COLUMN txn_direction VARCHAR(16) NOT NULL COMMENT '出入库方向',
    MODIFY COLUMN txn_qty DECIMAL(18, 4) NOT NULL COMMENT '变动数量',
    MODIFY COLUMN before_qty DECIMAL(18, 4) NOT NULL COMMENT '变动前数量',
    MODIFY COLUMN after_qty DECIMAL(18, 4) NOT NULL COMMENT '变动后数量',
    MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE inventory_txn_record RENAME INDEX uk_tenant_txn_no TO uq_inventory_txn_record_tenant_txn_no;
ALTER TABLE inventory_txn_record ADD INDEX idx_inventory_txn_record_biz (tenant_id, biz_type, biz_no);
ALTER TABLE inventory_txn_record ADD INDEX idx_inventory_txn_record_material (tenant_id, material_id);
