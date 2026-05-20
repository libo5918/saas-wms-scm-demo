package com.example.scm.aiagent.tool.store;

import com.example.scm.aiagent.tool.model.ToolInvocationAuditRecord;

import java.util.List;

/**
 * Tool 调用审计存储抽象。
 *
 * <p>当前默认使用 in-memory，实现后续可平滑替换为 MySQL。</p>
 */
public interface ToolInvocationAuditStore {

    /**
     * 保存一条 Tool 调用审计记录。
     */
    void save(ToolInvocationAuditRecord record);

    /**
     * 按租户和可选筛选条件查询最近的 Tool 调用记录。
     */
    List<ToolInvocationAuditRecord> list(Long tenantId, String toolName, String runId, int limit);
}
