USE scm_purchase;

-- 修复历史库缺少 supplier_id 导致的启动报错：
-- Unknown column 'supplier_id' in 'field list'
ALTER TABLE purchase_order
    ADD COLUMN IF NOT EXISTS supplier_id BIGINT NOT NULL DEFAULT 0 COMMENT '供应商ID' AFTER order_no;

ALTER TABLE purchase_receipt
    ADD COLUMN IF NOT EXISTS supplier_id BIGINT NOT NULL DEFAULT 0 COMMENT '供应商ID' AFTER purchase_order_id;
