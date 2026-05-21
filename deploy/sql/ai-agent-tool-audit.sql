-- AI Agent Phase 4.9: Tool 调用审计 MySQL 表结构
-- 说明：只保存最小可观测字段，不保存 API Key、用户 token、敏感请求头、完整 prompt、完整模型响应或大段业务数据。
USE scm_ai_agent;

CREATE TABLE IF NOT EXISTS tool_invocation_audit (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    tenant_id BIGINT NOT NULL COMMENT '租户 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    run_id VARCHAR(64) NOT NULL COMMENT 'Agent runId',
    tool_name VARCHAR(128) NOT NULL COMMENT 'Tool 名称',
    adapter_mode VARCHAR(32) NOT NULL COMMENT 'Tool 适配模式，例如 mock/http',
    success TINYINT NOT NULL COMMENT '是否成功，1=成功，0=失败',
    error_code VARCHAR(64) NULL COMMENT '失败错误码',
    latency_ms BIGINT NOT NULL DEFAULT 0 COMMENT '调用耗时毫秒',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '记录创建时间',
    PRIMARY KEY (id),
    KEY idx_tool_audit_tenant_created (tenant_id, created_at),
    KEY idx_tool_audit_tenant_tool_created (tenant_id, tool_name, created_at),
    KEY idx_tool_audit_tenant_run (tenant_id, run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI Agent Tool 调用审计表';
