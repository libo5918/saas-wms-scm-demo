package com.example.scm.aiagent.multiagent.model;

/** Multi-Agent PlannerAgent 识别出的受控任务类型。 */
public enum MultiAgentIntentType {
    RAG_ONLY,
    TOOL_ONLY,
    RAG_TOOL,
    WORKFLOW,
    MCP_TOOL,
    GENERAL
}
