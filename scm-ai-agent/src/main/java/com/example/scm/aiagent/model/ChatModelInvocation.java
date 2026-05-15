package com.example.scm.aiagent.model;

public record ChatModelInvocation(
        String runId,
        String message,
        String taskType,
        AgentRequestContext context,
        ModelRoute route
) {
}
