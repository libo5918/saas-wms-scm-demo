package com.example.scm.aiagent.toolcalling.answer.strategy;

import org.springframework.stereotype.Component;

/**
 * 物料查询答案提示词策略。
 */
@Component
public class MaterialAnswerPromptStrategy implements ToolCallingAnswerPromptStrategy {

    @Override
    public boolean supports(String toolName) {
        return "mdm.getMaterial".equals(toolName);
    }

    @Override
    public String instructions() {
        return "物料查询结果：优先说明物料编码、物料名称、状态、单位和分类；不要补充不存在的库存、价格或供应商信息。";
    }
}
