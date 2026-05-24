package com.example.scm.aiagent.multiagent;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.multiagent.model.MultiAgentRun;
import com.example.scm.aiagent.multiagent.model.MultiAgentRunStatus;
import com.example.scm.aiagent.multiagent.store.InMemoryMultiAgentRunStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryMultiAgentRunStoreTest {

    @Test
    void shouldSaveGetListAndTrimByMaxRecords() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getMultiAgent().setMaxRecords(2);
        InMemoryMultiAgentRunStore store = new InMemoryMultiAgentRunStore(properties);

        store.save(run("run-1"));
        store.save(run("run-2"));
        store.save(run("run-3"));

        assertTrue(store.get("run-1").isEmpty());
        assertTrue(store.get("run-2").isPresent());
        assertTrue(store.get("run-3").isPresent());
        assertEquals(2, store.list(10).size());
        assertFalse(store.list(10).toString().toLowerCase().contains("token"));
    }

    private MultiAgentRun run(String runId) {
        return MultiAgentRun.builder()
                .runId(runId)
                .tenantId(1L)
                .userId(10001L)
                .userMessage("safe message")
                .status(MultiAgentRunStatus.SUCCESS)
                .createdAt(Instant.now())
                .build();
    }
}
