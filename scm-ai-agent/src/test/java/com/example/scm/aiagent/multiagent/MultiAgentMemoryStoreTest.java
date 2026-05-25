package com.example.scm.aiagent.multiagent;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.multiagent.model.MultiAgentMemoryEntry;
import com.example.scm.aiagent.multiagent.model.MultiAgentMemoryType;
import com.example.scm.aiagent.multiagent.store.InMemoryMultiAgentMemoryStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MultiAgentMemoryStoreTest {

    @Test
    void shouldTrimByMaxRecordsAndFilterTenantUserConversation() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getMultiAgent().setMemoryMaxRecords(2);
        InMemoryMultiAgentMemoryStore store = new InMemoryMultiAgentMemoryStore(properties);

        store.append(entry("mem-1", "conv-1", 1L, 10001L));
        store.append(entry("mem-2", "conv-1", 1L, 10001L));
        store.append(entry("mem-3", "conv-1", 1L, 10001L));
        store.append(entry("mem-4", "conv-1", 2L, 10001L));

        assertEquals(1, store.listByConversationId(1L, 10001L, "conv-1", 10).size());
        assertEquals("mem-3", store.listByConversationId(1L, 10001L, "conv-1", 10).get(0).getMemoryId());
        assertEquals(1, store.clearByConversationId(1L, 10001L, "conv-1"));
        assertEquals(0, store.listByConversationId(1L, 10001L, "conv-1", 10).size());
    }

    private MultiAgentMemoryEntry entry(String memoryId, String conversationId, Long tenantId, Long userId) {
        return MultiAgentMemoryEntry.builder()
                .memoryId(memoryId)
                .conversationId(conversationId)
                .runId("run-" + memoryId)
                .tenantId(tenantId)
                .userId(userId)
                .type(MultiAgentMemoryType.FINAL_ANSWER_SUMMARY)
                .contentSummary("safe summary")
                .structuredData(Map.of("summary", "safe"))
                .createdAt(Instant.now())
                .build();
    }
}
