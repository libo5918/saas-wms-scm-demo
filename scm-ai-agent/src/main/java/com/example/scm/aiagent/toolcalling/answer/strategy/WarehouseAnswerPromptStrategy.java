package com.example.scm.aiagent.toolcalling.answer.strategy;

import org.springframework.stereotype.Component;

/**
 * 仓库查询答案提示词策略。
 */
@Component
public class WarehouseAnswerPromptStrategy implements ToolCallingAnswerPromptStrategy {

    @Override
    public boolean supports(String toolName) {
        return "mdm.getWarehouse".equals(toolName);
    }

    @Override
    public String instructions() {
        return "仓库查询结果：优先说明仓库编码、仓库名称、仓库类型和状态；不要编造库区、库位或库存数量。";
    }
}
