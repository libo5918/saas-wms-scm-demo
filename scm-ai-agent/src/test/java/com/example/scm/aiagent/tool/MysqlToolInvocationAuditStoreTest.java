package com.example.scm.aiagent.tool;

import com.example.scm.aiagent.tool.model.ToolInvocationAuditRecord;
import com.example.scm.aiagent.tool.persistence.mapper.ToolInvocationAuditMapper;
import com.example.scm.aiagent.tool.persistence.po.ToolInvocationAuditPO;
import com.example.scm.aiagent.tool.store.MysqlToolInvocationAuditStore;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MysqlToolInvocationAuditStoreTest {

    @Test
    void shouldPersistMinimumAuditFields() {
        ToolInvocationAuditMapper mapper = mock(ToolInvocationAuditMapper.class);
        MysqlToolInvocationAuditStore store = new MysqlToolInvocationAuditStore(mapper);
        Instant createdAt = Instant.parse("2026-05-22T00:00:00Z");

        store.save(ToolInvocationAuditRecord.builder()
                .tenantId(1L)
                .userId(10001L)
                .runId("run-audit-1")
                .toolName("mdm.getMaterial")
                .adapterMode("http")
                .success(false)
                .errorCode("404")
                .latencyMs(23)
                .createdAt(createdAt)
                .build());

        ArgumentCaptor<ToolInvocationAuditPO> captor = ArgumentCaptor.forClass(ToolInvocationAuditPO.class);
        verify(mapper).insert(captor.capture());
        ToolInvocationAuditPO po = captor.getValue();
        assertEquals(1L, po.getTenantId());
        assertEquals(10001L, po.getUserId());
        assertEquals("run-audit-1", po.getRunId());
        assertEquals("mdm.getMaterial", po.getToolName());
        assertEquals("http", po.getAdapterMode());
        assertEquals(false, po.getSuccess());
        assertEquals("404", po.getErrorCode());
        assertEquals(23L, po.getLatencyMs());
        assertEquals(createdAt, po.getCreatedAt());
    }

    @Test
    void shouldReadRecentAuditRecords() {
        ToolInvocationAuditMapper mapper = mock(ToolInvocationAuditMapper.class);
        MysqlToolInvocationAuditStore store = new MysqlToolInvocationAuditStore(mapper);
        ToolInvocationAuditPO po = new ToolInvocationAuditPO();
        po.setTenantId(1L);
        po.setUserId(10001L);
        po.setRunId("run-audit-2");
        po.setToolName("inventory.getBalance");
        po.setAdapterMode("mock");
        po.setSuccess(true);
        po.setLatencyMs(12L);
        po.setCreatedAt(Instant.parse("2026-05-22T00:00:00Z"));
        when(mapper.selectRecent(eq(1L), eq("inventory.getBalance"), eq("run-audit-2"), eq(10)))
                .thenReturn(List.of(po));

        List<ToolInvocationAuditRecord> records = store.list(1L, "inventory.getBalance", "run-audit-2", 10);

        assertEquals(1, records.size());
        assertEquals("inventory.getBalance", records.get(0).getToolName());
        assertEquals("run-audit-2", records.get(0).getRunId());
        assertEquals(12L, records.get(0).getLatencyMs());
    }
}
