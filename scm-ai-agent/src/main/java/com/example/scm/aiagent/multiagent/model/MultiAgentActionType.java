package com.example.scm.aiagent.multiagent.model;

/** Multi-Agent step 表达的动作类型，Phase 10.1 只使用其中少量安全骨架动作。 */
public enum MultiAgentActionType {
    PLAN,
    RAG_RETRIEVE,
    TOOL_CALL,
    WORKFLOW_RUN,
    MCP_TOOL_CALL,
    REVIEW,
    FINAL_ANSWER,
    NOOP
}
