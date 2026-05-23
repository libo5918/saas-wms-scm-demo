package com.example.scm.aiagent.workflow.service;

import com.example.scm.aiagent.dto.ChatRequest;
import com.example.scm.aiagent.dto.ChatResponse;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.rag.dto.RagRetrieveRequest;
import com.example.scm.aiagent.rag.dto.RagRetrieveResponse;
import com.example.scm.aiagent.rag.dto.RagRetrievedChunk;
import com.example.scm.aiagent.rag.service.RagService;
import com.example.scm.aiagent.service.AgentChatService;
import com.example.scm.aiagent.tool.dto.ToolInvokeRequest;
import com.example.scm.aiagent.tool.dto.ToolResponse;
import com.example.scm.aiagent.tool.service.ToolInvocationService;
import com.example.scm.aiagent.toolcalling.display.ToolCallingDisplaySchemaBuilder;
import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayData;
import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayField;
import com.example.scm.aiagent.workflow.dto.AgentWorkflowDefinitionView;
import com.example.scm.aiagent.workflow.dto.AgentWorkflowRunRequest;
import com.example.scm.aiagent.workflow.dto.AgentWorkflowRunResponse;
import com.example.scm.aiagent.workflow.model.AgentWorkflowDefinition;
import com.example.scm.aiagent.workflow.model.AgentWorkflowRun;
import com.example.scm.aiagent.workflow.model.AgentWorkflowRunStatus;
import com.example.scm.aiagent.workflow.model.AgentWorkflowStep;
import com.example.scm.aiagent.workflow.model.AgentWorkflowStepDefinition;
import com.example.scm.aiagent.workflow.model.AgentWorkflowStepStatus;
import com.example.scm.aiagent.workflow.model.AgentWorkflowStepType;
import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agent Workflow 服务。
 *
 * <p>Phase 6.1 只实现固定只读流程，用于展示业务流程编排与 Tool Orchestrator 的边界。</p>
 */
@Slf4j
@Service
public class AgentWorkflowService {

    private final AgentWorkflowDefinitionRegistry definitionRegistry;
    private final AgentWorkflowRunStore runStore;
    private final AgentWorkflowParameterResolver parameterResolver;
    private final AgentWorkflowViewMapper viewMapper;
    private final ToolInvocationService toolInvocationService;
    private final ToolCallingDisplaySchemaBuilder displaySchemaBuilder;
    private final AgentChatService agentChatService;
    private final RagService ragService;

    public AgentWorkflowService(AgentWorkflowDefinitionRegistry definitionRegistry,
                                AgentWorkflowRunStore runStore,
                                AgentWorkflowParameterResolver parameterResolver,
                                AgentWorkflowViewMapper viewMapper,
                                ToolInvocationService toolInvocationService,
                                ToolCallingDisplaySchemaBuilder displaySchemaBuilder,
                                AgentChatService agentChatService,
                                RagService ragService) {
        this.definitionRegistry = definitionRegistry;
        this.runStore = runStore;
        this.parameterResolver = parameterResolver;
        this.viewMapper = viewMapper;
        this.toolInvocationService = toolInvocationService;
        this.displaySchemaBuilder = displaySchemaBuilder;
        this.agentChatService = agentChatService;
        this.ragService = ragService;
    }

    public List<AgentWorkflowDefinitionView> listDefinitions() {
        return definitionRegistry.listDefinitions().stream()
                .map(viewMapper::toDefinitionView)
                .toList();
    }

    public AgentWorkflowRunResponse getRun(String runId) {
        return runStore.get(runId)
                .map(viewMapper::toRunResponse)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Workflow run not found: " + runId));
    }

    public List<AgentWorkflowRunResponse> listRuns(int limit) {
        return runStore.list(limit).stream().map(viewMapper::toRunResponse).toList();
    }

    public AgentWorkflowRunResponse run(String workflowCode,
                                        AgentWorkflowRunRequest request,
                                        AgentRequestContext context) {
        AgentWorkflowDefinition definition = definitionRegistry.findByCode(workflowCode)
                .filter(AgentWorkflowDefinition::isEnabled)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Workflow not found: " + workflowCode));
        if (!AgentWorkflowDefinitionRegistry.STOCK_REPLENISHMENT_WORKFLOW.equals(workflowCode)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Unsupported workflow: " + workflowCode);
        }

        long startedAt = System.nanoTime();
        String runId = StringUtils.hasText(request.getRunId()) ? request.getRunId() : UUID.randomUUID().toString();
        AgentWorkflowRun run = AgentWorkflowRun.builder()
                .runId(runId)
                .workflowCode(definition.getWorkflowCode())
                .workflowName(definition.getWorkflowName())
                .tenantId(context.tenantId())
                .userId(context.userId())
                .userMessage(request.getMessage())
                .status(AgentWorkflowRunStatus.RUNNING)
                .steps(new ArrayList<>())
                .startedAt(Instant.now())
                .build();
        runStore.save(run);

        log.info("AI workflow run started, tenantId={}, userId={}, runId={}, workflowCode={}, workflowName={}",
                context.tenantId(), context.userId(), runId, definition.getWorkflowCode(), definition.getWorkflowName());

        WorkflowInternalContext internalContext = new WorkflowInternalContext();
        executeMaterialStep(run, definition.getSteps().get(0), request, context, internalContext);
        executeInventoryStep(run, definition.getSteps().get(1), request, context, internalContext);
        executeSummaryStep(run, definition.getSteps().get(2), request, context, internalContext);

        boolean success = run.getSteps().stream().allMatch(step -> step.getStatus() == AgentWorkflowStepStatus.SUCCESS);
        run.setStatus(success ? AgentWorkflowRunStatus.SUCCESS : AgentWorkflowRunStatus.FAILED);
        run.setFinishedAt(Instant.now());
        run.setLatencyMs(elapsedMs(startedAt));
        runStore.save(run);
        log.info("AI workflow run finished, tenantId={}, userId={}, runId={}, workflowCode={}, status={}, latencyMs={}",
                context.tenantId(), context.userId(), runId, definition.getWorkflowCode(), run.getStatus(), run.getLatencyMs());
        return viewMapper.toRunResponse(run);
    }

    private void executeMaterialStep(AgentWorkflowRun run,
                                     AgentWorkflowStepDefinition definition,
                                     AgentWorkflowRunRequest request,
                                     AgentRequestContext context,
                                     WorkflowInternalContext internalContext) {
        AgentWorkflowStep step = newStep(definition);
        run.getSteps().add(step);
        long startedAt = beginStep(run, step, context);
        String materialCode = parameterResolver.resolveMaterialCode(request.getMessage(), request.getParameters());
        if (!StringUtils.hasText(materialCode)) {
            skipStep(run, step, context, "缺少物料编码 materialCode", startedAt);
            run.setFinalAnswer("无法生成补货建议草案：缺少物料编码 materialCode。");
            return;
        }
        internalContext.materialCode = materialCode;
        Map<String, Object> parameters = Map.of("materialCode", materialCode);
        ToolResponse response = invokeTool(run.getRunId(), definition.getToolName(), parameters, context);
        finishToolStep(run, step, context, response, startedAt);
        if (response.isSuccess()) {
            ToolCallingDisplayData displayData = displaySchemaBuilder.build(response.getToolName(), response.getData());
            Map<String, Object> safeFields = safeFields(displayData);
            Object materialId = firstNonNull(asMap(response.getData()).get("id"), safeFields.get("id"), safeFields.get("materialId"));
            safeFields.put("materialId", materialId);
            step.setDisplayTitle(displayData.displayTitle());
            step.setDisplaySummary(displayData.displaySummary());
            step.setSafeFields(safeFields);
            internalContext.materialId = materialId;
            internalContext.materialSafeFields = safeFields;
        } else {
            run.setFinalAnswer("无法生成补货建议草案：物料查询失败，原因：" + response.getErrorMessage());
        }
    }

    private void executeInventoryStep(AgentWorkflowRun run,
                                      AgentWorkflowStepDefinition definition,
                                      AgentWorkflowRunRequest request,
                                      AgentRequestContext context,
                                      WorkflowInternalContext internalContext) {
        AgentWorkflowStep step = newStep(definition);
        run.getSteps().add(step);
        long startedAt = beginStep(run, step, context);
        if (internalContext.materialId == null) {
            skipStep(run, step, context, "缺少上一步物料ID", startedAt);
            return;
        }
        Object warehouseId = parameterResolver.resolveWarehouseId(request.getMessage(), request.getParameters());
        Object locationId = parameterResolver.resolveLocationId(request.getMessage(), request.getParameters());
        if (warehouseId == null || locationId == null) {
            String missing = warehouseId == null && locationId == null ? "warehouseId、locationId"
                    : warehouseId == null ? "warehouseId" : "locationId";
            skipStep(run, step, context, "缺少库存查询参数：" + missing, startedAt);
            run.setFinalAnswer("无法生成完整补货建议草案：缺少库存查询参数 " + missing + "。");
            return;
        }
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("materialId", internalContext.materialId);
        parameters.put("warehouseId", warehouseId);
        parameters.put("locationId", locationId);
        ToolResponse response = invokeTool(run.getRunId(), definition.getToolName(), parameters, context);
        finishToolStep(run, step, context, response, startedAt);
        if (response.isSuccess()) {
            ToolCallingDisplayData displayData = displaySchemaBuilder.build(response.getToolName(), response.getData());
            Map<String, Object> safeFields = safeFields(displayData);
            step.setDisplayTitle(displayData.displayTitle());
            step.setDisplaySummary(displayData.displaySummary());
            step.setSafeFields(safeFields);
            internalContext.inventorySafeFields = safeFields;
        } else {
            run.setFinalAnswer("无法生成补货建议草案：库存查询失败，原因：" + response.getErrorMessage());
        }
    }

    private void executeSummaryStep(AgentWorkflowRun run,
                                    AgentWorkflowStepDefinition definition,
                                    AgentWorkflowRunRequest request,
                                    AgentRequestContext context,
                                    WorkflowInternalContext internalContext) {
        AgentWorkflowStep step = newStep(definition);
        run.getSteps().add(step);
        long startedAt = beginStep(run, step, context);
        if (internalContext.materialSafeFields == null || internalContext.inventorySafeFields == null) {
            skipStep(run, step, context, "前置只读查询未全部成功", startedAt);
            if (!StringUtils.hasText(run.getFinalAnswer())) {
                run.setFinalAnswer("无法生成补货建议草案：前置只读查询未全部成功。");
            }
            return;
        }
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setTaskType("workflow_stock_replenishment_advice");
        chatRequest.setRequiredCapabilities(List.of("CHAT"));
        Map<String, Object> ragSummary = retrieveRagSummary(run, step, request, context, internalContext);
        internalContext.ragSummary = ragSummary;
        chatRequest.setMessage(buildSummaryPrompt(request.getMessage(), internalContext));
        ChatResponse response = agentChatService.chat(chatRequest, context);
        step.setStatus(AgentWorkflowStepStatus.SUCCESS);
        step.setInputResolved(true);
        step.setDisplayTitle("补货建议草案");
        step.setDisplaySummary("已基于只读查询结果生成补货建议草案");
        Map<String, Object> summarySafeFields = new LinkedHashMap<>();
        summarySafeFields.put("material", internalContext.materialSafeFields == null ? Map.of() : internalContext.materialSafeFields);
        summarySafeFields.put("inventory", internalContext.inventorySafeFields == null ? Map.of() : internalContext.inventorySafeFields);
        if (!ragSummary.isEmpty()) {
            summarySafeFields.put("rag", ragSummary);
        }
        step.setSafeFields(summarySafeFields);
        step.setFinishedAt(Instant.now());
        step.setLatencyMs(elapsedMs(startedAt));
        run.setFinalAnswer(response.getAnswer());
        logStepFinished(run, step, context);
    }

    private ToolResponse invokeTool(String runId, String toolName, Map<String, Object> parameters, AgentRequestContext context) {
        ToolInvokeRequest request = new ToolInvokeRequest();
        request.setRunId(runId);
        request.setToolName(toolName);
        request.setParameters(parameters);
        return toolInvocationService.invoke(request, context);
    }

    private Map<String, Object> retrieveRagSummary(AgentWorkflowRun run,
                                                   AgentWorkflowStep step,
                                                   AgentWorkflowRunRequest request,
                                                   AgentRequestContext context,
                                                   WorkflowInternalContext internalContext) {
        if (!StringUtils.hasText(request.getKnowledgeBaseId())) {
            return Map.of();
        }
        RagRetrieveRequest retrieveRequest = new RagRetrieveRequest();
        retrieveRequest.setKnowledgeBaseId(request.getKnowledgeBaseId());
        retrieveRequest.setQuery(buildRagQuery(run, request, internalContext));
        retrieveRequest.setTopK(request.getTopK());
        retrieveRequest.setScoreThreshold(request.getScoreThreshold());
        retrieveRequest.setFilters(request.getFilters() == null ? Map.of() : request.getFilters());
        RagRetrieveResponse response = ragService.retrieve(retrieveRequest, context);
        log.info("AI workflow rag retrieve finished, tenantId={}, userId={}, runId={}, workflowCode={}, workflowName={}, stepCode={}, ragKnowledgeBaseId={}, ragRetrievedCount={}, latencyMs={}",
                context.tenantId(), context.userId(), run.getRunId(), run.getWorkflowCode(), run.getWorkflowName(),
                step.getStepCode(), response.getKnowledgeBaseId(), response.getRetrievedCount(), response.getLatencyMs());
        return toRagSummary(response);
    }

    private String buildRagQuery(AgentWorkflowRun run,
                                 AgentWorkflowRunRequest request,
                                 WorkflowInternalContext context) {
        return """
                %s
                workflow=%s
                material=%s
                inventory=%s
                rules=库存可用数量口径,锁定数量含义,物料状态含义,补货建议规则,审批人工确认边界
                """.formatted(request.getMessage(), run.getWorkflowName(),
                context.materialSafeFields == null ? Map.of() : context.materialSafeFields,
                context.inventorySafeFields == null ? Map.of() : context.inventorySafeFields);
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

    private AgentWorkflowStep newStep(AgentWorkflowStepDefinition definition) {
        return AgentWorkflowStep.builder()
                .stepCode(definition.getStepCode())
                .stepName(definition.getStepName())
                .stepNo(definition.getStepNo())
                .stepType(definition.getStepType())
                .toolName(definition.getToolName())
                .status(AgentWorkflowStepStatus.PENDING)
                .safeFields(Map.of())
                .build();
    }

    private long beginStep(AgentWorkflowRun run, AgentWorkflowStep step, AgentRequestContext context) {
        long startedAt = System.nanoTime();
        step.setStatus(AgentWorkflowStepStatus.RUNNING);
        step.setStartedAt(Instant.now());
        log.info("AI workflow step started, tenantId={}, userId={}, runId={}, workflowCode={}, workflowName={}, stepCode={}, stepNo={}, stepType={}, toolName={}, status={}",
                context.tenantId(), context.userId(), run.getRunId(), run.getWorkflowCode(), run.getWorkflowName(),
                step.getStepCode(), step.getStepNo(), step.getStepType(), step.getToolName(), step.getStatus());
        return startedAt;
    }

    private void finishToolStep(AgentWorkflowRun run, AgentWorkflowStep step, AgentRequestContext context,
                                ToolResponse response, long startedAt) {
        step.setStatus(response.isSuccess() ? AgentWorkflowStepStatus.SUCCESS : AgentWorkflowStepStatus.FAILED);
        step.setInputResolved(true);
        step.setErrorCode(response.getErrorCode());
        step.setErrorMessage(response.getErrorMessage());
        step.setFinishedAt(Instant.now());
        step.setLatencyMs(elapsedMs(startedAt));
        if (!response.isSuccess()) {
            step.setDisplaySummary(response.getErrorMessage());
        }
        logStepFinished(run, step, context);
    }

    private void skipStep(AgentWorkflowRun run, AgentWorkflowStep step, AgentRequestContext context,
                          String reason, long startedAt) {
        step.setStatus(AgentWorkflowStepStatus.SKIPPED);
        step.setInputResolved(false);
        step.setSkipReason(reason);
        step.setFinishedAt(Instant.now());
        step.setLatencyMs(elapsedMs(startedAt));
        logStepFinished(run, step, context);
    }

    private void logStepFinished(AgentWorkflowRun run, AgentWorkflowStep step, AgentRequestContext context) {
        log.info("AI workflow step finished, tenantId={}, userId={}, runId={}, workflowCode={}, workflowName={}, stepCode={}, stepNo={}, stepType={}, toolName={}, status={}, success={}, errorCode={}, latencyMs={}",
                context.tenantId(), context.userId(), run.getRunId(), run.getWorkflowCode(), run.getWorkflowName(),
                step.getStepCode(), step.getStepNo(), step.getStepType(), step.getToolName(), step.getStatus(),
                step.getStatus() == AgentWorkflowStepStatus.SUCCESS, step.getErrorCode(), step.getLatencyMs());
    }

    private String buildSummaryPrompt(String userMessage, WorkflowInternalContext context) {
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
                """.formatted(userMessage, context.materialSafeFields, context.inventorySafeFields,
                context.ragSummary == null || context.ragSummary.isEmpty() ? "未检索到知识库规则摘要，请不要编造知识库内容。" : context.ragSummary);
    }

    private String snippet(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private Map<String, Object> safeFields(ToolCallingDisplayData displayData) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (displayData.displayFields() != null) {
            for (ToolCallingDisplayField field : displayData.displayFields()) {
                result.put(field.key(), field.value());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object data) {
        if (data instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, value) -> result.put(String.valueOf(key), value));
            return result;
        }
        return Map.of();
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private static class WorkflowInternalContext {
        private String materialCode;
        private Object materialId;
        private Map<String, Object> materialSafeFields;
        private Map<String, Object> inventorySafeFields;
        private Map<String, Object> ragSummary = Map.of();
    }
}
