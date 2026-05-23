package com.example.scm.aiagent.agent.service;

import com.example.scm.aiagent.agent.dto.AgentChatRequest;
import com.example.scm.aiagent.agent.dto.AgentChatResponse;
import com.example.scm.aiagent.agent.dto.AgentOrchestrationStepView;
import com.example.scm.aiagent.agent.dto.AgentOrchestrationView;
import com.example.scm.aiagent.agent.dto.AgentRagChunkView;
import com.example.scm.aiagent.agent.dto.AgentRagView;
import com.example.scm.aiagent.agent.dto.AgentToolExecutionView;
import com.example.scm.aiagent.agent.dto.AgentToolView;
import com.example.scm.aiagent.dto.ChatRequest;
import com.example.scm.aiagent.dto.ChatResponse;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.rag.dto.RagRetrieveRequest;
import com.example.scm.aiagent.rag.dto.RagRetrieveResponse;
import com.example.scm.aiagent.rag.dto.RagRetrievedChunk;
import com.example.scm.aiagent.rag.service.RagService;
import com.example.scm.aiagent.service.AgentChatService;
import com.example.scm.aiagent.toolcalling.application.ToolCallingChatService;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatRequest;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatResponse;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecutionView;
import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayData;
import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayField;
import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayItem;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolCallingOrchestratorService;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationExecutionSummary;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationRun;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RAG + Tool 组合问答服务。
 *
 * <p>Phase 5.1 以面试展示为目标，串联知识库检索、实时 Tool 查询和最终模型总结。</p>
 */
@Slf4j
@Service
public class RagToolAgentChatService {

    private static final int MAX_CHUNK_SNIPPET_LENGTH = 300;

    private final RagToolIntentRouter intentRouter;
    private final RagService ragService;
    private final ToolCallingChatService toolCallingChatService;
    private final ToolCallingOrchestratorService orchestratorService;
    private final RagToolAnswerPromptBuilder promptBuilder;
    private final AgentChatService agentChatService;

    public RagToolAgentChatService(RagToolIntentRouter intentRouter,
                                   RagService ragService,
                                   ToolCallingChatService toolCallingChatService,
                                   ToolCallingOrchestratorService orchestratorService,
                                   RagToolAnswerPromptBuilder promptBuilder,
                                   AgentChatService agentChatService) {
        this.intentRouter = intentRouter;
        this.ragService = ragService;
        this.toolCallingChatService = toolCallingChatService;
        this.orchestratorService = orchestratorService;
        this.promptBuilder = promptBuilder;
        this.agentChatService = agentChatService;
    }

    /**
     * 执行一次组合问答。
     */
    public AgentChatResponse chat(AgentChatRequest request, AgentRequestContext context) {
        long startedAt = System.nanoTime();
        String runId = StringUtils.hasText(request.getRunId()) ? request.getRunId() : UUID.randomUUID().toString();
        AgentIntentType intentType = intentRouter.route(request.getMessage(), request.getKnowledgeBaseId());

        RagRetrieveResponse ragRetrieveResponse = shouldRetrieve(intentType, request)
                ? ragService.retrieve(toRagRetrieveRequest(request), context)
                : null;
        AgentRagView ragView = toRagView(ragRetrieveResponse);

        ToolCallingChatResponse toolResponse = shouldCallTool(intentType)
                ? toolCallingChatService.chat(toToolCallingRequest(request, runId), context)
                : null;
        ToolOrchestrationRun orchestrationRun = toolResponse == null ? null : orchestratorService.getRun(toolResponse.getRunId());
        AgentToolView toolView = toToolView(toolResponse);
        AgentOrchestrationView orchestrationView = toOrchestrationView(orchestrationRun);

        String prompt = promptBuilder.build(request.getMessage(), intentType, ragView, toolView, orchestrationRun);
        ChatResponse chatResponse = agentChatService.chat(toChatRequest(prompt), context);
        long latencyMs = elapsedMs(startedAt);

        log.info("AI agent rag-tool chat finished, tenantId={}, userId={}, runId={}, knowledgeBaseId={}, intentType={}, ragRetrievedCount={}, selectedTool={}, orchestrationEnabled={}, planMode={}, stepCount={}, success={}, fallbackUsed={}, latencyMs={}",
                context.tenantId(), context.userId(), runId, request.getKnowledgeBaseId(), intentType,
                ragView == null ? 0 : ragView.getRetrievedCount(),
                toolView == null ? null : toolView.getSelectedTool(),
                orchestrationView != null && orchestrationView.isEnabled(),
                orchestrationView == null ? null : orchestrationView.getPlanMode(),
                orchestrationView == null ? 0 : orchestrationView.getStepCount(),
                true, toolResponse != null && toolResponse.isFallbackUsed(), latencyMs);

        return AgentChatResponse.builder()
                .runId(runId)
                .intentType(intentType.name())
                .answer(chatResponse.getAnswer())
                .rag(ragView)
                .tool(toolView)
                .orchestration(orchestrationView)
                .fallbackUsed(toolResponse != null && toolResponse.isFallbackUsed())
                .latencyMs(latencyMs)
                .build();
    }

    private boolean shouldRetrieve(AgentIntentType intentType, AgentChatRequest request) {
        return StringUtils.hasText(request.getKnowledgeBaseId())
                && (intentType == AgentIntentType.RAG_ONLY || intentType == AgentIntentType.RAG_TOOL);
    }

    private boolean shouldCallTool(AgentIntentType intentType) {
        return intentType == AgentIntentType.TOOL_ONLY || intentType == AgentIntentType.RAG_TOOL;
    }

    private RagRetrieveRequest toRagRetrieveRequest(AgentChatRequest request) {
        RagRetrieveRequest retrieveRequest = new RagRetrieveRequest();
        retrieveRequest.setKnowledgeBaseId(request.getKnowledgeBaseId());
        retrieveRequest.setQuery(request.getMessage());
        retrieveRequest.setTopK(request.getTopK());
        retrieveRequest.setScoreThreshold(request.getScoreThreshold());
        retrieveRequest.setFilters(request.getFilters());
        return retrieveRequest;
    }

    private ToolCallingChatRequest toToolCallingRequest(AgentChatRequest request, String runId) {
        ToolCallingChatRequest toolRequest = new ToolCallingChatRequest();
        toolRequest.setRunId(runId);
        toolRequest.setMessage(request.getMessage());
        toolRequest.setPlannerMode(request.getPlannerMode());
        toolRequest.setRequestedTool(request.getRequestedTool());
        toolRequest.setToolArguments(request.getToolArguments());
        toolRequest.setRequestedDomain(request.getRequestedDomain());
        toolRequest.setRequestedCategory(request.getRequestedCategory());
        toolRequest.setRouteTags(request.getRouteTags());
        return toolRequest;
    }

    private ChatRequest toChatRequest(String prompt) {
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setMessage(prompt);
        chatRequest.setTaskType("rag_tool_agent_answer");
        chatRequest.setRequiredCapabilities(List.of("CHAT"));
        return chatRequest;
    }

    private AgentRagView toRagView(RagRetrieveResponse response) {
        if (response == null) {
            return AgentRagView.builder()
                    .retrievedCount(0)
                    .chunks(List.of())
                    .build();
        }
        return AgentRagView.builder()
                .knowledgeBaseId(response.getKnowledgeBaseId())
                .retrievedCount(response.getRetrievedCount())
                .latencyMs(response.getLatencyMs())
                .chunks(response.getChunks() == null ? List.of() : response.getChunks().stream()
                        .map(this::toChunkView)
                        .toList())
                .build();
    }

    private AgentRagChunkView toChunkView(RagRetrievedChunk chunk) {
        return AgentRagChunkView.builder()
                .documentId(chunk.getDocumentId())
                .chunkId(chunk.getChunkId())
                .title(chunk.getTitle())
                .source(chunk.getSource())
                .contentSnippet(snippet(chunk.getContent(), MAX_CHUNK_SNIPPET_LENGTH))
                .score(chunk.getScore())
                .build();
    }

    private AgentToolView toToolView(ToolCallingChatResponse response) {
        if (response == null) {
            return null;
        }
        return AgentToolView.builder()
                .selectedTool(response.getSelectedTool())
                .toolArguments(response.getToolArguments() == null ? Map.of() : response.getToolArguments())
                .execution(toExecutionView(response.getExecution()))
                .build();
    }

    private AgentToolExecutionView toExecutionView(ToolCallingExecutionView execution) {
        if (execution == null) {
            return null;
        }
        String displayTitle = null;
        String displaySummary = null;
        List<ToolCallingDisplayField> displayFields = List.of();
        List<ToolCallingDisplayItem> displayItems = List.of();
        if (execution.getData() instanceof ToolCallingDisplayData displayData) {
            displayTitle = displayData.displayTitle();
            displaySummary = displayData.displaySummary();
            displayFields = displayData.displayFields() == null ? List.of() : displayData.displayFields();
            displayItems = displayData.displayItems() == null ? List.of() : displayData.displayItems();
        }
        return AgentToolExecutionView.builder()
                .success(execution.isSuccess())
                .toolName(execution.getToolName())
                .errorCode(execution.getErrorCode())
                .errorMessage(execution.getErrorMessage())
                .displayTitle(displayTitle)
                .displaySummary(displaySummary)
                .displayFields(displayFields)
                .displayItems(displayItems)
                .latencyMs(execution.getLatencyMs())
                .build();
    }

    private AgentOrchestrationView toOrchestrationView(ToolOrchestrationRun run) {
        if (run == null) {
            return AgentOrchestrationView.builder()
                    .enabled(false)
                    .stepCount(0)
                    .steps(List.of())
                    .build();
        }
        return AgentOrchestrationView.builder()
                .enabled(true)
                .runId(run.getRunId())
                .planMode(run.getPlan() == null ? null : String.valueOf(run.getPlan().getMode()))
                .stepCount(run.getSteps() == null ? 0 : run.getSteps().size())
                .steps(run.getSteps() == null ? List.of() : run.getSteps().stream()
                        .map(this::toStepView)
                        .toList())
                .build();
    }

    private AgentOrchestrationStepView toStepView(ToolOrchestrationStep step) {
        ToolOrchestrationExecutionSummary execution = step.getExecution();
        return AgentOrchestrationStepView.builder()
                .stepNo(step.getStepNo())
                .stepRef(step.getStepRef())
                .toolName(step.getToolName())
                .status(step.getStatus() == null ? null : step.getStatus().name())
                .executed(step.getExecuted())
                .inputResolved(step.getInputResolved())
                .skipReason(step.getSkipReason())
                .displayTitle(execution == null ? null : execution.getDisplayTitle())
                .displaySummary(execution == null ? null : execution.getDisplaySummary())
                .safeFields(execution == null || execution.getSafeFields() == null ? Map.of() : execution.getSafeFields())
                .build();
    }

    private String snippet(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
