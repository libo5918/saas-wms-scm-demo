package com.example.scm.aiagent.agent.prompt;

/**
 * Agent prompt 上下文分区类型。
 */
public enum AgentPromptContextType {

    USER_MESSAGE,
    RAG_CONTEXT,
    TOOL_EXECUTION,
    ORCHESTRATION_STEPS,
    SYSTEM_INSTRUCTIONS,
    SAFETY_CONSTRAINTS
}
