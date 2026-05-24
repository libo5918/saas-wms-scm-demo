package com.example.scm.aiagent.multiagent.service;

import com.example.scm.aiagent.multiagent.dto.MultiAgentChatRequest;
import com.example.scm.aiagent.multiagent.model.MultiAgentIntentType;
import com.example.scm.aiagent.multiagent.model.MultiAgentPlan;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/** PlannerAgent 的规则化任务规划服务，Phase 10.2 不调用模型。 */
@Service
public class MultiAgentPlannerService {

    public MultiAgentPlan plan(MultiAgentChatRequest request) {
        String message = request.getMessage() == null ? "" : request.getMessage();
        boolean hasRagIntent = containsAny(message, "解释", "规则", "口径", "知识库", "为什么", "流程", "含义");
        boolean hasToolIntent = hasExplicitToolHint(request)
                || (containsAny(message, "查", "查询", "看看", "获取", "实时")
                && containsAny(message, "物料", "库存", "仓库", "库位", "销售订单", "采购订单", "订单"));
        boolean hasWorkflowIntent = containsAny(message, "补货建议", "建议草案", "流程草案");

        MultiAgentIntentType intentType = resolveIntentType(hasRagIntent, hasToolIntent, hasWorkflowIntent, request);
        boolean needRag = intentType == MultiAgentIntentType.RAG_ONLY || intentType == MultiAgentIntentType.RAG_TOOL;
        boolean needTool = intentType == MultiAgentIntentType.TOOL_ONLY || intentType == MultiAgentIntentType.RAG_TOOL
                || intentType == MultiAgentIntentType.MCP_TOOL;

        return MultiAgentPlan.builder()
                .intentType(intentType)
                .needRag(needRag)
                .needTool(needTool)
                .needWorkflow(hasWorkflowIntent)
                .needReview(true)
                .requestedTool(request.getRequestedTool())
                .requestedDomain(request.getRequestedDomain())
                .routeTags(request.getRouteTags() == null ? List.of() : request.getRouteTags())
                .reason(reason(intentType))
                .build();
    }

    private MultiAgentIntentType resolveIntentType(boolean rag, boolean tool, boolean workflow,
                                                   MultiAgentChatRequest request) {
        if (workflow && !tool && !rag) {
            return MultiAgentIntentType.WORKFLOW;
        }
        if (StringUtils.hasText(request.getRequestedTool()) && request.getRequestedTool().startsWith("mcp.")) {
            return MultiAgentIntentType.MCP_TOOL;
        }
        if (rag && tool) {
            return MultiAgentIntentType.RAG_TOOL;
        }
        if (tool) {
            return MultiAgentIntentType.TOOL_ONLY;
        }
        if (rag || StringUtils.hasText(request.getKnowledgeBaseId())) {
            return MultiAgentIntentType.RAG_ONLY;
        }
        return MultiAgentIntentType.GENERAL;
    }

    private boolean hasExplicitToolHint(MultiAgentChatRequest request) {
        return StringUtils.hasText(request.getRequestedTool())
                || StringUtils.hasText(request.getRequestedDomain())
                || (request.getRouteTags() != null && !request.getRouteTags().isEmpty());
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String reason(MultiAgentIntentType intentType) {
        return switch (intentType) {
            case RAG_ONLY -> "识别为知识库规则、口径或流程解释任务";
            case TOOL_ONLY -> "识别为实时业务数据查询任务";
            case RAG_TOOL -> "识别为知识库解释 + 实时业务数据查询组合任务";
            case WORKFLOW -> "识别为固定业务流程任务，本阶段仅记录计划";
            case MCP_TOOL -> "识别为 MCP Tool 调用任务，本阶段仅记录受控计划";
            case GENERAL -> "识别为通用 Multi-Agent 协作任务";
        };
    }
}
