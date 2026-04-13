USE scm_auth;

INSERT INTO sys_tenant(tenant_code, tenant_name, status, created_by, updated_by, deleted)
VALUES ('TENANT_DEFAULT', '默认租户', 1, 1, 1, 0)
ON DUPLICATE KEY UPDATE tenant_name = VALUES(tenant_name), status = VALUES(status), updated_by = VALUES(updated_by);

INSERT INTO sys_user(tenant_id, username, password, nickname, status, created_by, updated_by, deleted)
SELECT 1, 'admin', '{noop}123456', '系统管理员', 1, 1, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_user WHERE tenant_id = 1 AND username = 'admin'
);
