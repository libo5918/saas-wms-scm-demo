package com.example.scm.aiagent.agent.prompt;

/**
 * Agent prompt 上下文来源，用于后续平滑映射到 Advisor 链路。
 */
public enum AgentPromptContextSource {

    REQUEST,
    RAG,
    TOOL,
    ORCHESTRATION,
    SYSTEM
}
