package com.example.scm.aiagent.agent.prompt;

/**
 * Prompt 构造结果，额外携带上下文治理统计指标，便于日志观测。
 */
public record AgentPromptBuildResult(
        String prompt,
        AgentPromptContext context,
        int sectionCount,
        int includedSectionCount,
        int truncatedSectionCount
) {
}
