package com.example.scm.aiagent.tool;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.tool.model.ToolInvocationAuditRecord;
import com.example.scm.aiagent.tool.service.ToolInvocationAuditService;
import com.example.scm.aiagent.tool.store.ToolInvocationAuditStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ToolInvocationAuditServiceTest {

    @Test
    void shouldRecordMinimumAuditFields() {
        CapturingAuditStore store = new CapturingAuditStore();
        AiAgentProperties properties = new AiAgentProperties();
        properties.getTools().getAudit().setMode("mysql");
        ToolInvocationAuditService service = new ToolInvocationAuditService(store, properties);
        AgentRequestContext context = new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));

        service.record(context, "run-audit-1", "mdm.getMaterial", "http", false, "404", 37);

        ToolInvocationAuditRecord record = store.record;
        assertEquals(1L, record.getTenantId());
        assertEquals(10001L, record.getUserId());
        assertEquals("run-audit-1", record.getRunId());
        assertEquals("mdm.getMaterial", record.getToolName());
        assertEquals("http", record.getAdapterMode());
        assertEquals(false, record.isSuccess());
        assertEquals("404", record.getErrorCode());
        assertEquals(37L, record.getLatencyMs());
        assertNotNull(record.getCreatedAt());
    }

    private static class CapturingAuditStore implements ToolInvocationAuditStore {

        private ToolInvocationAuditRecord record;

        @Override
        public void save(ToolInvocationAuditRecord record) {
            this.record = record;
        }

        @Override
        public List<ToolInvocationAuditRecord> list(Long tenantId, String toolName, String runId, int limit) {
            return List.of();
        }
    }
}
