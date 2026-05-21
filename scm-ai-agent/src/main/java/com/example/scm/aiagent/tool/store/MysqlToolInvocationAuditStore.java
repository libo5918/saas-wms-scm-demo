package com.example.scm.aiagent.tool.store;

import com.example.scm.aiagent.tool.model.ToolInvocationAuditRecord;
import com.example.scm.aiagent.tool.persistence.mapper.ToolInvocationAuditMapper;
import com.example.scm.aiagent.tool.persistence.po.ToolInvocationAuditPO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tool 调用审计的 MySQL 实现。
 *
 * <p>只保存最小可观测字段，不持久化 API Key、请求头、prompt、模型响应或大段业务数据。</p>
 */
@Component
@ConditionalOnProperty(prefix = "ai.agent.tools.audit", name = "mode", havingValue = "mysql")
public class MysqlToolInvocationAuditStore implements ToolInvocationAuditStore {

    private final ToolInvocationAuditMapper mapper;

    public MysqlToolInvocationAuditStore(ToolInvocationAuditMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(ToolInvocationAuditRecord record) {
        mapper.insert(toPo(record));
    }

    @Override
    public List<ToolInvocationAuditRecord> list(Long tenantId, String toolName, String runId, int limit) {
        return mapper.selectRecent(tenantId, toolName, runId, Math.max(1, limit))
                .stream()
                .map(this::toRecord)
                .toList();
    }

    private ToolInvocationAuditPO toPo(ToolInvocationAuditRecord record) {
        ToolInvocationAuditPO po = new ToolInvocationAuditPO();
        po.setTenantId(record.getTenantId());
        po.setUserId(record.getUserId());
        po.setRunId(record.getRunId());
        po.setToolName(record.getToolName());
        po.setAdapterMode(record.getAdapterMode());
        po.setSuccess(record.isSuccess());
        po.setErrorCode(record.getErrorCode());
        po.setLatencyMs(record.getLatencyMs());
        po.setCreatedAt(record.getCreatedAt());
        return po;
    }

    private ToolInvocationAuditRecord toRecord(ToolInvocationAuditPO po) {
        return ToolInvocationAuditRecord.builder()
                .tenantId(po.getTenantId())
                .userId(po.getUserId())
                .runId(po.getRunId())
                .toolName(po.getToolName())
                .adapterMode(po.getAdapterMode())
                .success(Boolean.TRUE.equals(po.getSuccess()))
                .errorCode(po.getErrorCode())
                .latencyMs(po.getLatencyMs() == null ? 0 : po.getLatencyMs())
                .createdAt(po.getCreatedAt())
                .build();
    }
}
