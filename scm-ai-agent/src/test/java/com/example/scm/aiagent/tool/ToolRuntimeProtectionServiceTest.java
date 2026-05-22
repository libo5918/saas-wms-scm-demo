package com.example.scm.aiagent.tool;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.tool.client.ToolCircuitOpenException;
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
        assertEquals(1, service.getStatus("inventory.getBalance").getTotalCalls());
        assertEquals(1, service.getStatus("inventory.getBalance").getSuccessCount());
        assertEquals(1, service.getStatus("inventory.getBalance").getRetryCount());
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
        assertEquals(1, service.getStatus("inventory.getBalance").getFailureCount());
        assertEquals("IllegalStateException", service.getStatus("inventory.getBalance").getLastErrorType());
    }

    @Test
    void shouldNotOpenCircuitWhenCircuitBreakerDisabled() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getTools().getRuntime().setCircuitBreakerEnabled(false);
        properties.getTools().getRuntime().setRetryEnabled(false);
        properties.getTools().getRuntime().setFailureThreshold(1);
        ToolRuntimeProtectionService service = new ToolRuntimeProtectionService(properties);
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(ToolClientException.class, () -> service.execute("inventory.getBalance", () -> {
            attempts.incrementAndGet();
            throw new ToolClientException("down");
        }));
        assertThrows(ToolClientException.class, () -> service.execute("inventory.getBalance", () -> {
            attempts.incrementAndGet();
            throw new ToolClientException("down");
        }));

        assertEquals(2, attempts.get());
        assertEquals("CLOSED", service.getStatus("inventory.getBalance").getCircuitState());
    }

    @Test
    void shouldOpenCircuitAndSkipExecutionWhenThresholdReached() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getTools().getRuntime().setCircuitBreakerEnabled(true);
        properties.getTools().getRuntime().setRetryEnabled(false);
        properties.getTools().getRuntime().setFailureThreshold(1);
        properties.getTools().getRuntime().setOpenDurationMs(60000);
        ToolRuntimeProtectionService service = new ToolRuntimeProtectionService(properties);
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(ToolClientException.class, () -> service.execute("inventory.getBalance", () -> {
            attempts.incrementAndGet();
            throw new ToolClientException("down");
        }));
        assertThrows(ToolCircuitOpenException.class, () -> service.execute("inventory.getBalance", () -> {
            attempts.incrementAndGet();
            return "never";
        }));

        assertEquals(1, attempts.get());
        assertEquals("OPEN", service.getStatus("inventory.getBalance").getCircuitState());
    }

    @Test
    void shouldHalfOpenAfterOpenDurationAndCloseOnSuccess() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getTools().getRuntime().setCircuitBreakerEnabled(true);
        properties.getTools().getRuntime().setRetryEnabled(false);
        properties.getTools().getRuntime().setFailureThreshold(1);
        properties.getTools().getRuntime().setOpenDurationMs(0);
        ToolRuntimeProtectionService service = new ToolRuntimeProtectionService(properties);
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(ToolClientException.class, () -> service.execute("inventory.getBalance", () -> {
            attempts.incrementAndGet();
            throw new ToolClientException("down");
        }));
        Object result = service.execute("inventory.getBalance", () -> {
            attempts.incrementAndGet();
            return "ok";
        });

        assertEquals("ok", result);
        assertEquals(2, attempts.get());
        assertEquals("CLOSED", service.getStatus("inventory.getBalance").getCircuitState());
    }
}
