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

CREATE TABLE IF NOT EXISTS outbox_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    event_id VARCHAR(64) NOT NULL COMMENT '事件ID',
    aggregate_type VARCHAR(32) NOT NULL COMMENT '聚合类型',
    aggregate_id VARCHAR(64) NOT NULL COMMENT '聚合ID',
    event_type VARCHAR(64) NOT NULL COMMENT '事件类型',
    event_key VARCHAR(64) NOT NULL COMMENT '分区键',
    topic VARCHAR(128) NOT NULL COMMENT 'Kafka Topic',
    payload_json JSON NOT NULL COMMENT '事件负载',
    status VARCHAR(16) NOT NULL DEFAULT 'NEW' COMMENT '事件状态',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    next_retry_time DATETIME DEFAULT NULL COMMENT '下次重试时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uq_outbox_event_event_id UNIQUE (event_id),
    KEY idx_outbox_event_status_retry (status, next_retry_time)
) COMMENT='Outbox事件表：保障业务落库与消息发布最终一致';

CREATE TABLE IF NOT EXISTS mq_consume_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    consumer_group VARCHAR(64) NOT NULL COMMENT '消费组',
    topic VARCHAR(128) NOT NULL COMMENT 'Kafka Topic',
    event_id VARCHAR(64) NOT NULL COMMENT '事件ID',
    biz_key VARCHAR(64) DEFAULT NULL COMMENT '业务键',
    consumed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消费时间',
    CONSTRAINT uq_mq_consume_log_unique UNIQUE (consumer_group, topic, event_id)
) COMMENT='消息消费幂等日志表';
