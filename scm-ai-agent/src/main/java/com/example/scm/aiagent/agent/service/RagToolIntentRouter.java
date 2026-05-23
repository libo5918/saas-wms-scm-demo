package com.example.scm.aiagent.agent.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * RAG + Tool 组合问答的轻量意图路由器。
 *
 * <p>Phase 5.1 为了面试展示和稳定测试，先使用关键词规则，不引入模型分类。</p>
 */
@Component
public class RagToolIntentRouter {

    private static final List<String> TOOL_KEYWORDS = List.of(
            "物料", "仓库", "库存", "销售订单", "采购订单", "订单", "余额", "可用数量", "锁定数量",
            "material", "warehouse", "inventory", "balance", "sales order", "purchase order");

    private static final List<String> RAG_KEYWORDS = List.of(
            "规则", "口径", "含义", "解释", "说明", "流程", "为什么", "怎么计算", "如何计算",
            "rule", "policy", "explain", "meaning", "process");

    private static final List<String> TOOL_QUERY_KEYWORDS = List.of(
            "查", "查询", "看看", "看一下", "获取", "多少", "状态", "信息", "get", "query", "find", "show");

    private static final List<String> INVENTORY_FOLLOW_UP_KEYWORDS = List.of(
            "库存", "库存余额", "余额", "可用数量", "锁定数量", "库位库存",
            "inventory", "balance", "available", "locked");

    /**
     * 识别组合问答应走 RAG、Tool，还是两者都走。
     */
    public AgentIntentType route(String message, String knowledgeBaseId) {
        boolean hasToolIntent = containsAny(message, TOOL_KEYWORDS) && containsAny(message, TOOL_QUERY_KEYWORDS);
        boolean hasRagIntent = containsAny(message, RAG_KEYWORDS) || StringUtils.hasText(knowledgeBaseId);
        if (hasToolIntent && hasRagIntent) {
            return AgentIntentType.RAG_TOOL;
        }
        if (hasToolIntent) {
            return AgentIntentType.TOOL_ONLY;
        }
        return AgentIntentType.RAG_ONLY;
    }

    /**
     * 判断受控二步是否允许继续查库存。
     */
    public boolean hasInventoryFollowUpIntent(String message) {
        return containsAny(message, INVENTORY_FOLLOW_UP_KEYWORDS);
    }

    private boolean containsAny(String message, List<String> keywords) {
        if (!StringUtils.hasText(message)) {
            return false;
        }
        String lower = message.toLowerCase();
        return keywords.stream().anyMatch(keyword -> lower.contains(keyword.toLowerCase()));
    }
}
