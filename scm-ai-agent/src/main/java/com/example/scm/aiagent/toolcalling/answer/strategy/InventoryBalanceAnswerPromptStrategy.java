package com.example.scm.aiagent.toolcalling.answer.strategy;

import org.springframework.stereotype.Component;

/**
 * 库存余额答案提示词策略。
 */
@Component
public class InventoryBalanceAnswerPromptStrategy implements ToolCallingAnswerPromptStrategy {

    @Override
    public boolean supports(String toolName) {
        return "inventory.getBalance".equals(toolName);
    }

    @Override
    public String instructions() {
        return "库存余额查询结果：优先说明可用数量、锁定数量、仓库、库位和单位；如果缺少某个数量字段，不要自行计算或推断。";
    }
}
