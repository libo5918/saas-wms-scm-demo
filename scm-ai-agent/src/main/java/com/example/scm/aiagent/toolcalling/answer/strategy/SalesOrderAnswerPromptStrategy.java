package com.example.scm.aiagent.toolcalling.answer.strategy;

import org.springframework.stereotype.Component;

/**
 * 销售订单答案提示词策略。
 */
@Component
public class SalesOrderAnswerPromptStrategy implements ToolCallingAnswerPromptStrategy {

    @Override
    public boolean supports(String toolName) {
        return "sales.getOrder".equals(toolName);
    }

    @Override
    public String instructions() {
        return "销售订单查询结果：优先说明订单号、订单状态、客户和明细行数；不要编造发货、出库或收款结果。";
    }
}
