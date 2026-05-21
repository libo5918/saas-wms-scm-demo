package com.example.scm.aiagent.toolcalling.answer.strategy;

/**
 * Tool Calling 答案总结提示词策略。
 *
 * <p>不同 Tool 的展示字段重点不同，策略只补充模型总结要求，不改变 Tool 执行和返回结构。</p>
 */
public interface ToolCallingAnswerPromptStrategy {

    /**
     * 当前策略是否适用于指定 Tool。
     */
    boolean supports(String toolName);

    /**
     * 返回面向模型的中文总结要求。
     */
    String instructions();
}
