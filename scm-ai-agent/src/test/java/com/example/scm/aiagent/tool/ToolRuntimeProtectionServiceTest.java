package com.example.scm.aiagent.tool;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.tool.client.ToolClientException;
import com.example.scm.aiagent.tool.service.ToolRuntimeProtectionService;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ToolRuntimeProtectionServiceTest {

    @Test
    void shouldRetryRetryableToolClientException() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getTools().getRuntime().setMaxRetries(1);
        ToolRuntimeProtectionService service = new ToolRuntimeProtectionService(properties);
        AtomicInteger attempts = new AtomicInteger();

        Object result = service.execute("inventory.getBalance", () -> {
            if (attempts.incrementAndGet() == 1) {
                throw new ToolClientException("temporary failure");
            }
            return "ok";
        });

        assertEquals("ok", result);
        assertEquals(2, attempts.get());
    }

    @Test
    void shouldNotRetryNonRetryableRuntimeException() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getTools().getRuntime().setMaxRetries(3);
        ToolRuntimeProtectionService service = new ToolRuntimeProtectionService(properties);
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(IllegalStateException.class, () -> service.execute("inventory.getBalance", () -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("bad state");
        }));

        assertEquals(1, attempts.get());
    }
}
