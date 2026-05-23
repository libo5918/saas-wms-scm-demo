package com.example.scm.aiagent.agent.service;

/** RAG + Tool 组合入口的最小意图类型。 */
public enum AgentIntentType {
    /** 只需要知识库解释或规则说明。 */
    RAG_ONLY,

    /** 只需要实时业务 Tool 查询。 */
    TOOL_ONLY,

    /** 同时需要知识库口径解释和实时业务数据。 */
    RAG_TOOL
}
