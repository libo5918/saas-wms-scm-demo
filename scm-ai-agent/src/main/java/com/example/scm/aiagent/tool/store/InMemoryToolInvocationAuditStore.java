package com.example.scm.aiagent.tool.store;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.tool.model.ToolInvocationAuditRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Tool 调用审计的内存实现。
 */
@Component
@ConditionalOnProperty(prefix = "ai.agent.tools.audit", name = "mode", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryToolInvocationAuditStore implements ToolInvocationAuditStore {

    private final ConcurrentLinkedDeque<ToolInvocationAuditRecord> records = new ConcurrentLinkedDeque<>();
    private final int maxRecords;

    public InMemoryToolInvocationAuditStore(AiAgentProperties properties) {
        this.maxRecords = properties.getTools().getAudit().getMaxRecords();
    }

    @Override
    public void save(ToolInvocationAuditRecord record) {
        records.addFirst(record);
        while (records.size() > maxRecords) {
            records.pollLast();
        }
    }

    @Override
    public List<ToolInvocationAuditRecord> list(Long tenantId, String toolName, String runId, int limit) {
        int effectiveLimit = Math.max(1, limit);
        return records.stream()
                .filter(record -> tenantId.equals(record.getTenantId()))
                .filter(record -> !StringUtils.hasText(toolName) || toolName.equals(record.getToolName()))
                .filter(record -> !StringUtils.hasText(runId) || runId.equals(record.getRunId()))
                .limit(effectiveLimit)
                .toList();
    }
}
