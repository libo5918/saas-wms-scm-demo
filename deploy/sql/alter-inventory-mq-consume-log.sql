USE scm_inventory;

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
