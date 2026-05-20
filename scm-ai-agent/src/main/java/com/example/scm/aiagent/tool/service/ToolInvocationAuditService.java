package com.example.scm.aiagent.tool.service;

import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.tool.dto.ToolInvocationAuditListResponse;
import com.example.scm.aiagent.tool.model.ToolInvocationAuditRecord;
import com.example.scm.aiagent.tool.store.ToolInvocationAuditStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Tool 调用审计服务。
 */
@Slf4j
@Service
public class ToolInvocationAuditService {

    private final ToolInvocationAuditStore toolInvocationAuditStore;

    public ToolInvocationAuditService(ToolInvocationAuditStore toolInvocationAuditStore) {
        this.toolInvocationAuditStore = toolInvocationAuditStore;
    }

    /**
     * 保存一条 Tool 调用审计记录。
     */
    public void record(AgentRequestContext context, String runId, String toolName, String adapterMode,
                       boolean success, String errorCode, long latencyMs) {
        ToolInvocationAuditRecord record = ToolInvocationAuditRecord.builder()
                .tenantId(context.tenantId())
                .userId(context.userId())
                .runId(runId)
                .toolName(toolName)
                .adapterMode(adapterMode)
                .success(success)
                .errorCode(errorCode)
                .latencyMs(latencyMs)
                .createdAt(Instant.now())
                .build();
        toolInvocationAuditStore.save(record);
        log.info("AI tool audit recorded, tenantId={}, userId={}, runId={}, toolName={}, adapterMode={}, success={}, latencyMs={}",
                context.tenantId(), context.userId(), runId, toolName, adapterMode, success, latencyMs);
    }

    /**
     * 查询最近的 Tool 调用审计记录。
     */
    public ToolInvocationAuditListResponse list(AgentRequestContext context, String toolName, String runId, Integer limit) {
        int effectiveLimit = limit == null ? 20 : Math.max(1, Math.min(limit, 100));
        var records = toolInvocationAuditStore.list(context.tenantId(), toolName, runId, effectiveLimit);
        return ToolInvocationAuditListResponse.builder()
                .tenantId(context.tenantId())
                .count(records.size())
                .records(records)
                .build();
    }
}
