package com.example.scm.aiagent.toolcalling.answer.strategy;

import org.springframework.stereotype.Component;

/**
 * 采购订单答案提示词策略。
 */
@Component
public class PurchaseOrderAnswerPromptStrategy implements ToolCallingAnswerPromptStrategy {

    @Override
    public boolean supports(String toolName) {
        return "purchase.getOrder".equals(toolName);
    }

    @Override
    public String instructions() {
        return "采购订单查询结果：优先说明订单号、订单状态、供应商和明细行数；不要编造到货、入库或付款结果。";
    }
}
