package com.example.scm.aiagent.toolcalling.answer.strategy;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tool Calling 答案提示词策略注册表。
 */
@Component
public class ToolCallingAnswerPromptStrategyRegistry {

    private final List<ToolCallingAnswerPromptStrategy> strategies;
    private final ToolCallingAnswerPromptStrategy fallbackStrategy = new FallbackAnswerPromptStrategy();

    public ToolCallingAnswerPromptStrategyRegistry(List<ToolCallingAnswerPromptStrategy> strategies) {
        this.strategies = strategies;
    }

    /**
     * 按 Tool 名称选择最合适的提示词策略。
     */
    public ToolCallingAnswerPromptStrategy resolve(String toolName) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(toolName))
                .findFirst()
                .orElse(fallbackStrategy);
    }

    private static class FallbackAnswerPromptStrategy implements ToolCallingAnswerPromptStrategy {

        @Override
        public boolean supports(String toolName) {
            return true;
        }

        @Override
        public String instructions() {
            return "通用 Tool 查询结果：优先引用展示摘要和展示字段，避免扩展猜测；如果信息不足，说明只能基于当前工具结果回答。";
        }
    }
}
