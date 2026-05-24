package com.example.scm.aiagent.workflow.executor;

import com.example.scm.aiagent.dto.ChatRequest;
import com.example.scm.aiagent.dto.ChatResponse;
import com.example.scm.aiagent.rag.dto.RagRetrieveRequest;
import com.example.scm.aiagent.rag.dto.RagRetrieveResponse;
import com.example.scm.aiagent.rag.dto.RagRetrievedChunk;
import com.example.scm.aiagent.rag.service.RagService;
import com.example.scm.aiagent.service.AgentChatService;
import com.example.scm.aiagent.workflow.dto.AgentWorkflowRunRequest;
import com.example.scm.aiagent.workflow.engine.AgentWorkflowExecutionContext;
import com.example.scm.aiagent.workflow.model.AgentWorkflowRun;
import com.example.scm.aiagent.workflow.model.AgentWorkflowStep;
import com.example.scm.aiagent.workflow.model.AgentWorkflowStepDefinition;
import com.example.scm.aiagent.workflow.model.AgentWorkflowStepStatus;
import com.example.scm.aiagent.workflow.model.AgentWorkflowStepType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Summary 类型 Workflow 步骤执行器，负责 RAG 检索和最终建议生成。 */
@Slf4j
@Component
public class SummaryWorkflowStepExecutor extends AbstractWorkflowStepExecutor {

    private static final String QUERY_MATERIAL = "query_material";
    private static final String QUERY_INVENTORY_BALANCE = "query_inventory_balance";

    private final AgentChatService agentChatService;
    private final RagService ragService;

    public SummaryWorkflowStepExecutor(AgentChatService agentChatService, RagService ragService) {
        this.agentChatService = agentChatService;
        this.ragService = ragService;
    }

    @Override
    public boolean supports(AgentWorkflowStepDefinition definition) {
        return definition.getStepType() == AgentWorkflowStepType.SUMMARY;
    }

    @Override
    public void execute(AgentWorkflowExecutionContext context, AgentWorkflowStepDefinition definition) {
        AgentWorkflowStep step = newStep(definition);
        long startedAt = beginStep(context, step);
        if (!dependenciesSucceeded(context, definition)) {
            skipStep(context, step, "前置只读查询未全部成功", startedAt);
            if (!StringUtils.hasText(context.getRun().getFinalAnswer())) {
                context.setFinalAnswer("无法生成补货建议草案：前置只读查询未全部成功。");
            }
            return;
        }

        Map<String, Object> materialSafeFields = context.getStepOutput(QUERY_MATERIAL);
        Map<String, Object> inventorySafeFields = context.getStepOutput(QUERY_INVENTORY_BALANCE);
        Map<String, Object> ragSummary = retrieveRagSummary(context, step, materialSafeFields, inventorySafeFields);
        context.setRagSummary(ragSummary);

        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setTaskType("workflow_stock_replenishment_advice");
        chatRequest.setRequiredCapabilities(List.of("CHAT"));
        chatRequest.setMessage(buildSummaryPrompt(context.getRequest().getMessage(), materialSafeFields, inventorySafeFields, ragSummary));
        ChatResponse response = agentChatService.chat(chatRequest, context.getAgentRequestContext());

        step.setStatus(AgentWorkflowStepStatus.SUCCESS);
        step.setInputResolved(true);
        step.setDisplayTitle("补货建议草案");
        step.setDisplaySummary("已基于只读查询结果生成补货建议草案");
        Map<String, Object> summarySafeFields = new LinkedHashMap<>();
        summarySafeFields.put("material", materialSafeFields);
        summarySafeFields.put("inventory", inventorySafeFields);
        if (!ragSummary.isEmpty()) {
            summarySafeFields.put("rag", ragSummary);
        }
        step.setSafeFields(summarySafeFields);
        context.setFinalAnswer(response.getAnswer());
        finishStep(context, step, startedAt);
    }

    private boolean dependenciesSucceeded(AgentWorkflowExecutionContext context, AgentWorkflowStepDefinition definition) {
        if (definition.getDependsOnStepCodes() == null || definition.getDependsOnStepCodes().isEmpty()) {
            return true;
        }
        return definition.getDependsOnStepCodes().stream().allMatch(context::isStepSuccess);
    }

    private Map<String, Object> retrieveRagSummary(AgentWorkflowExecutionContext context,
                                                   AgentWorkflowStep step,
                                                   Map<String, Object> materialSafeFields,
                                                   Map<String, Object> inventorySafeFields) {
        AgentWorkflowRunRequest request = context.getRequest();
        if (!StringUtils.hasText(request.getKnowledgeBaseId())) {
            return Map.of();
        }
        RagRetrieveRequest retrieveRequest = new RagRetrieveRequest();
        retrieveRequest.setKnowledgeBaseId(request.getKnowledgeBaseId());
        retrieveRequest.setQuery(buildRagQuery(context.getRun(), request, materialSafeFields, inventorySafeFields));
        retrieveRequest.setTopK(request.getTopK());
        retrieveRequest.setScoreThreshold(request.getScoreThreshold());
        retrieveRequest.setFilters(request.getFilters() == null ? Map.of() : request.getFilters());
        RagRetrieveResponse response = ragService.retrieve(retrieveRequest, context.getAgentRequestContext());
        log.info("AI workflow rag retrieve finished, tenantId={}, userId={}, runId={}, workflowCode={}, workflowName={}, stepCode={}, ragKnowledgeBaseId={}, ragRetrievedCount={}, latencyMs={}",
                context.getAgentRequestContext().tenantId(), context.getAgentRequestContext().userId(),
                context.getRun().getRunId(), context.getRun().getWorkflowCode(), context.getRun().getWorkflowName(),
                step.getStepCode(), response.getKnowledgeBaseId(), response.getRetrievedCount(), response.getLatencyMs());
        return toRagSummary(response);
    }

    private String buildRagQuery(AgentWorkflowRun run,
                                 AgentWorkflowRunRequest request,
                                 Map<String, Object> materialSafeFields,
                                 Map<String, Object> inventorySafeFields) {
        return """
                %s
                workflow=%s
                material=%s
                inventory=%s
                rules=库存可用数量口径,锁定数量含义,物料状态含义,补货建议规则,审批人工确认边界
                """.formatted(request.getMessage(), run.getWorkflowName(), materialSafeFields, inventorySafeFields);
    }

    private Map<String, Object> toRagSummary(RagRetrieveResponse response) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("knowledgeBaseId", response.getKnowledgeBaseId());
        summary.put("retrievedCount", response.getRetrievedCount());
        summary.put("chunks", response.getChunks() == null ? List.of() : response.getChunks().stream()
                .map(this::toRagChunkSummary)
                .toList());
        return summary;
    }

    private Map<String, Object> toRagChunkSummary(RagRetrievedChunk chunk) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("documentId", chunk.getDocumentId());
        summary.put("chunkId", chunk.getChunkId());
        summary.put("title", chunk.getTitle());
        summary.put("source", chunk.getSource());
        summary.put("contentSnippet", snippet(chunk.getContent(), 300));
        summary.put("score", chunk.getScore());
        return summary;
    }

    private String buildSummaryPrompt(String userMessage,
                                      Map<String, Object> materialSafeFields,
                                      Map<String, Object> inventorySafeFields,
                                      Map<String, Object> ragSummary) {
        return """
                你是 SCM/WMS 企业级 AI Agent，请基于只读查询结果生成中文补货建议草案。
                要求：
                1. 只能给出建议草案，不要创建采购单、调拨单、补货单或任何写操作。
                2. 简要说明物料、仓库/库位库存、可用数量和风险判断。
                3. 如果库存充足，说明无需补货；如果库存偏低，给出人工确认建议。

                用户问题：
                %s

                物料安全摘要：
                %s

                库存安全摘要：
                %s
                知识库规则摘要：
                %s
                """.formatted(userMessage, materialSafeFields, inventorySafeFields,
                ragSummary == null || ragSummary.isEmpty() ? "未检索到知识库规则摘要，请不要编造知识库内容。" : ragSummary);
    }

    private String snippet(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
