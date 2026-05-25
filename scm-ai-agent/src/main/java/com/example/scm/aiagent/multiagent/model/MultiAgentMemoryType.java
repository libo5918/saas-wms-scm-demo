package com.example.scm.aiagent.multiagent.model;

/** Multi-Agent 会话记忆摘要类型，只保存可安全复用的短摘要。 */
public enum MultiAgentMemoryType {
    USER_MESSAGE_SUMMARY,
    PLAN_SUMMARY,
    RAG_SUMMARY,
    TOOL_SUMMARY,
    REVIEW_SUMMARY,
    FINAL_ANSWER_SUMMARY
}
