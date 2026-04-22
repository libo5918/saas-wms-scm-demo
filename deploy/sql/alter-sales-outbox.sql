USE scm_sales;

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

ALTER TABLE outbox_event
    ADD COLUMN topic VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'Kafka Topic' AFTER event_key;

UPDATE outbox_event
SET topic = CASE event_type
                WHEN 'ORDER_SHIP_REQUESTED' THEN 'order.ship.requested.v1'
                WHEN 'ORDER_CANCEL_REQUESTED' THEN 'order.cancel.requested.v1'
                ELSE topic
            END
WHERE topic = '';

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
