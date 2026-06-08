package com.example.scm.javaagent.bytebuddy;

import com.example.scm.javaagent.config.AgentConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodTraceDecisionTest {

    @Test
    void shouldLogAllMethodsWhenThresholdIsZero() {
        AgentConfig config = AgentConfig.parse("slowThresholdMs=0");

        assertTrue(MethodTraceDecision.shouldLog(config, "invoke", 1, null));
    }

    @Test
    void shouldOnlyLogSlowMethodWhenThresholdIsPositive() {
        AgentConfig config = AgentConfig.parse("slowThresholdMs=200");

        assertFalse(MethodTraceDecision.shouldLog(config, "invoke", 199, null));
        assertTrue(MethodTraceDecision.shouldLog(config, "invoke", 200, null));
    }

    @Test
    void shouldAlwaysLogExceptionWhenMethodIsAllowed() {
        AgentConfig config = AgentConfig.parse("slowThresholdMs=200");

        assertTrue(MethodTraceDecision.shouldLog(config, "invoke", 1, new RuntimeException("failed")));
    }

    @Test
    void shouldRespectMethodFilter() {
        AgentConfig config = AgentConfig.parse("includeMethods=invoke,excludeMethods=skip,slowThresholdMs=0");

        assertTrue(MethodTraceDecision.shouldLog(config, "invoke", 1, null));
        assertFalse(MethodTraceDecision.shouldLog(config, "execute", 1, null));
        assertFalse(MethodTraceDecision.shouldLog(config, "skip", 1, new RuntimeException("failed")));
    }
}
