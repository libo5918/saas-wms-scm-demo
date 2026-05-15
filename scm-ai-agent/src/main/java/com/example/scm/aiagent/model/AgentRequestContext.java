package com.example.scm.aiagent.model;

import java.util.List;

public record AgentRequestContext(
        Long tenantId,
        Long userId,
        String username,
        List<String> roles
) {
}
