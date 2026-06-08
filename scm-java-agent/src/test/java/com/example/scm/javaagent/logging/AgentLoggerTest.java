package com.example.scm.javaagent.logging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentLoggerTest {

    @Test
    void sanitizeShouldRedactSensitiveFields() {
        String source = "authorization=Bearer abc, cookie=session, token=abc, accessToken=a, "
                + "refreshToken=b, apiKey=c, api-key=d, password=p, secret=s, "
                + "rawData={big}, prompt=full prompt, model response=full response";

        String sanitized = AgentLogger.sanitize(source);

        assertTrue(sanitized.contains("authorization=[REDACTED]"));
        assertTrue(sanitized.contains("cookie=[REDACTED]"));
        assertTrue(sanitized.contains("token=[REDACTED]"));
        assertTrue(sanitized.contains("accessToken=[REDACTED]"));
        assertTrue(sanitized.contains("refreshToken=[REDACTED]"));
        assertTrue(sanitized.contains("apiKey=[REDACTED]"));
        assertTrue(sanitized.contains("api-key=[REDACTED]"));
        assertTrue(sanitized.contains("password=[REDACTED]"));
        assertTrue(sanitized.contains("secret=[REDACTED]"));
        assertTrue(sanitized.contains("rawData=[REDACTED]"));
        assertTrue(sanitized.contains("prompt=[REDACTED]"));
        assertTrue(sanitized.contains("model response=[REDACTED]"));
        assertFalse(sanitized.contains("Bearer abc"));
        assertFalse(sanitized.contains("full prompt"));
        assertFalse(sanitized.contains("full response"));
    }
}
