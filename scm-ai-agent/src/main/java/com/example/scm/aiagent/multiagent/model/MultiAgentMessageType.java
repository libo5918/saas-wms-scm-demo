package com.example.scm.aiagent.multiagent.model;

/** Agent 间安全摘要消息类型，不承载完整 prompt 或模型响应。 */
public enum MultiAgentMessageType {
    TASK,
    PLAN_SUMMARY,
    RESULT_SUMMARY,
    FINAL_SUMMARY
}
