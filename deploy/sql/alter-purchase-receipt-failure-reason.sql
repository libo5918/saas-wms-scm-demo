USE scm_purchase;

-- 给现有收货单表补充失败原因列，避免代码升级后查询旧表结构报错。
ALTER TABLE purchase_receipt
    ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(255) DEFAULT NULL COMMENT '入库失败原因' AFTER receipt_status;

-- 同步刷新关键列注释，保证数据库工具里能直接看懂字段用途。
ALTER TABLE purchase_receipt
    MODIFY COLUMN receipt_status VARCHAR(32) NOT NULL COMMENT '收货单状态',
    MODIFY COLUMN failure_reason VARCHAR(255) DEFAULT NULL COMMENT '入库失败原因',
    MODIFY COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
