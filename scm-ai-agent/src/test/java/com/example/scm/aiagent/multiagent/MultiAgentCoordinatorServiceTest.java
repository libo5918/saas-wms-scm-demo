package com.example.scm.aiagent.multiagent;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.dto.ChatResponse;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.multiagent.dto.MultiAgentChatRequest;
import com.example.scm.aiagent.multiagent.dto.MultiAgentChatResponse;
import com.example.scm.aiagent.multiagent.model.MultiAgentIntentType;
import com.example.scm.aiagent.multiagent.model.MultiAgentRole;
import com.example.scm.aiagent.multiagent.model.MultiAgentRunStatus;
import com.example.scm.aiagent.multiagent.model.MultiAgentStepStatus;
import com.example.scm.aiagent.multiagent.service.MultiAgentCoordinatorService;
import com.example.scm.aiagent.multiagent.service.MultiAgentDefinitionRegistry;
import com.example.scm.aiagent.multiagent.service.MultiAgentKnowledgeService;
import com.example.scm.aiagent.multiagent.service.MultiAgentMemoryService;
import com.example.scm.aiagent.multiagent.service.MultiAgentPlannerService;
import com.example.scm.aiagent.multiagent.service.MultiAgentReviewService;
import com.example.scm.aiagent.multiagent.service.MultiAgentToolService;
import com.example.scm.aiagent.multiagent.store.InMemoryMultiAgentMemoryStore;
import com.example.scm.aiagent.multiagent.store.InMemoryMultiAgentRunStore;
import com.example.scm.aiagent.rag.dto.RagRetrieveRequest;
import com.example.scm.aiagent.rag.dto.RagRetrieveResponse;
import com.example.scm.aiagent.rag.dto.RagRetrievedChunk;
import com.example.scm.aiagent.rag.service.RagService;
import com.example.scm.aiagent.service.AgentChatService;
import com.example.scm.aiagent.toolcalling.application.ToolCallingChatService;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatResponse;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecutionView;
import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayData;
import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayField;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MultiAgentCoordinatorServiceTest {

    @Test
    void shouldRunRagToolReviewSingleRound() {
        MultiAgentCoordinatorService service = service(defaultProperties(), mockRagService(),
                mockToolCallingChatService(), mock(AgentChatService.class));
        MultiAgentChatRequest request = ragToolRequest("run-multi-agent-1");

        MultiAgentChatResponse response = service.chat(request, context());

        assertEquals("run-multi-agent-1", response.getRunId());
        assertEquals(MultiAgentRunStatus.SUCCESS, response.getStatus());
        assertEquals(MultiAgentIntentType.RAG_TOOL, response.getIntentType());
        assertEquals(1, response.getRag().get("retrievedCount"));
        assertEquals("mdm.getMaterial", response.getTool().get("selectedTool"));
        assertEquals(true, response.getReview().get("passed"));
        assertEquals("template", response.getSummaryMode());
        assertEquals(1, response.getRoundCount());
        assertEquals(1, response.getToolCallCount());
        assertEquals(false, response.getConstraints().get("exceeded"));
        assertTrue(response.getAnswer().contains("KnowledgeAgent"));
        assertTrue(response.getAnswer().contains("ToolAgent"));
        assertTrue(response.getAgents().stream().anyMatch(agent ->
                "KnowledgeAgent".equals(agent.getAgentName()) && agent.getStatus() == MultiAgentStepStatus.SUCCESS));
        assertTrue(response.getAgents().stream().anyMatch(agent ->
                "ToolAgent".equals(agent.getAgentName()) && agent.getStatus() == MultiAgentStepStatus.SUCCESS));
        assertTrue(response.getAgents().stream().anyMatch(agent ->
                "ReviewerAgent".equals(agent.getAgentName()) && agent.getRole() == MultiAgentRole.REVIEWER));
        assertFalse(response.toString().toLowerCase().contains("authorization"));
        assertFalse(response.toString().toLowerCase().contains("rawdata"));
    }

    @Test
    void shouldQuerySavedRun() {
        MultiAgentCoordinatorService service = service(defaultProperties(), mockRagService(),
                mockToolCallingChatService(), mock(AgentChatService.class));
        MultiAgentChatRequest request = new MultiAgentChatRequest();
        request.setRunId("run-query");
        request.setMessage("explain inventory rule");
        service.chat(request, context());

        MultiAgentChatResponse response = service.getRun("run-query");

        assertNotNull(response);
        assertEquals("run-query", response.getRunId());
        assertEquals(MultiAgentRunStatus.SUCCESS, response.getStatus());
    }

    @Test
    void shouldSkipToolWhenMaxToolCallsExceeded() {
        AiAgentProperties properties = defaultProperties();
        properties.getMultiAgent().setMaxToolCalls(0);
        ToolCallingChatService toolCallingChatService = mock(ToolCallingChatService.class);
        MultiAgentCoordinatorService service = service(properties, mockRagService(), toolCallingChatService,
                mock(AgentChatService.class));
        MultiAgentChatRequest request = new MultiAgentChatRequest();
        request.setRunId("run-tool-limit");
        request.setMessage("闂佹眹鍩勯崹濂稿窗濡ゅ懎鍨傛繝濠傜墕閽冪喖鏌曟竟顖氬暊閹稿懘姊?MAT-001");

        request.setRequestedDomain("mdm");
        MultiAgentChatResponse response = service.chat(request, context());

        assertEquals(0, response.getToolCallCount());
        assertEquals(true, response.getConstraints().get("exceeded"));
        assertTrue(response.getTerminatedReason().contains("maxToolCalls"));
        verify(toolCallingChatService, never()).chat(any(), any());
    }

    @Test
    void shouldTerminateWhenMaxRoundsExceeded() {
        AiAgentProperties properties = defaultProperties();
        properties.getMultiAgent().setMaxRounds(0);
        ToolCallingChatService toolCallingChatService = mock(ToolCallingChatService.class);
        MultiAgentCoordinatorService service = service(properties, mockRagService(), toolCallingChatService,
                mock(AgentChatService.class));
        MultiAgentChatRequest request = new MultiAgentChatRequest();
        request.setRunId("run-round-limit");
        request.setMessage("闂佹眹鍩勯崹濂稿窗濡ゅ懎鍨傛繝濠傜墕閽冪喖鏌曟竟顖氬暊閹稿懘姊?MAT-001");

        MultiAgentChatResponse response = service.chat(request, context());

        assertEquals(MultiAgentRunStatus.TERMINATED, response.getStatus());
        assertEquals(true, response.getConstraints().get("exceeded"));
        verify(toolCallingChatService, never()).chat(any(), any());
    }

    @Test
    void shouldUseModelSummaryWhenEnabled() {
        AiAgentProperties properties = defaultProperties();
        properties.getMultiAgent().setModelSummaryEnabled(true);
        AgentChatService agentChatService = mock(AgentChatService.class);
        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setAnswer("model summary with 闂佽崵鍠愰悷銉р偓姘煎墴瀹曞綊顢涢悙鑼厬闂佹寧绻傞幊鎰玻?and material summary MAT-001");
        when(agentChatService.chat(any(), any())).thenReturn(chatResponse);
        MultiAgentCoordinatorService service = service(properties, mockRagService(),
                mockToolCallingChatServiceWithSummary("material summary MAT-001"), agentChatService);

        MultiAgentChatResponse response = service.chat(ragToolRequest("run-model-summary"), context());

        assertEquals("model", response.getSummaryMode());
        assertFalse(response.isFallbackUsed());
        assertTrue(response.getAnswer().contains("material summary MAT-001"));
    }

    @Test
    void shouldFallbackToTemplateWhenModelSummaryFails() {
        AiAgentProperties properties = defaultProperties();
        properties.getMultiAgent().setModelSummaryEnabled(true);
        AgentChatService agentChatService = mock(AgentChatService.class);
        when(agentChatService.chat(any(), any())).thenThrow(new IllegalStateException("model down"));
        MultiAgentCoordinatorService service = service(properties, mockRagService(),
                mockToolCallingChatServiceWithSummary("material summary MAT-001"), agentChatService);

        MultiAgentChatResponse response = service.chat(ragToolRequest("run-model-fallback"), context());

        assertEquals("template", response.getSummaryMode());
        assertTrue(response.isFallbackUsed());
        assertTrue(response.getAnswer().contains("ToolAgent"));
    }

    @Test
    void shouldNotRepairWhenReviewRepairDisabled() {
        AiAgentProperties properties = defaultProperties();
        properties.getMultiAgent().setModelSummaryEnabled(true);
        AgentChatService agentChatService = mock(AgentChatService.class);
        ChatResponse badAnswer = new ChatResponse();
        badAnswer.setAnswer("model answer without tool facts");
        when(agentChatService.chat(any(), any())).thenReturn(badAnswer);
        MultiAgentCoordinatorService service = service(properties, mockRagService(),
                mockToolCallingChatServiceWithSummary("material summary MAT-001"), agentChatService);

        MultiAgentChatResponse response = service.chat(ragToolRequest("run-repair-disabled"), context());

        assertFalse(response.isRepairEnabled());
        assertFalse(response.isRepairAttempted());
        assertEquals(0, response.getRepairCount());
        assertEquals(false, response.getReview().get("passed"));
        assertTrue(response.getAnswer().contains("ReviewerAgent"));
    }

    @Test
    void shouldRepairOnceWithTemplateWhenReviewFails() {
        AiAgentProperties properties = defaultProperties();
        properties.getMultiAgent().setModelSummaryEnabled(true);
        properties.getMultiAgent().setReviewRepairEnabled(true);
        properties.getMultiAgent().setRepairMode("template");
        AgentChatService agentChatService = mock(AgentChatService.class);
        ChatResponse badAnswer = new ChatResponse();
        badAnswer.setAnswer("model answer without tool facts");
        when(agentChatService.chat(any(), any())).thenReturn(badAnswer);
        MultiAgentCoordinatorService service = service(properties, mockRagService(),
                mockToolCallingChatServiceWithSummary("material summary MAT-001"), agentChatService);

        MultiAgentChatResponse response = service.chat(ragToolRequest("run-template-repair"), context());

        assertTrue(response.isRepairEnabled());
        assertTrue(response.isRepairAttempted());
        assertEquals(1, response.getRepairCount());
        assertEquals("template", response.getRepairMode());
        assertFalse(response.isRepairFallbackUsed());
        assertEquals(2, response.getRoundCount());
        assertEquals(true, response.getReviewAfterRepair().get("passed"));
        assertTrue(response.getAnswer().contains("material summary MAT-001"));
        verify(agentChatService).chat(any(), any());
    }

    @Test
    void shouldNotRepairWhenMaxRoundsInsufficient() {
        AiAgentProperties properties = defaultProperties();
        properties.getMultiAgent().setModelSummaryEnabled(true);
        properties.getMultiAgent().setReviewRepairEnabled(true);
        properties.getMultiAgent().setMaxRounds(1);
        AgentChatService agentChatService = mock(AgentChatService.class);
        ChatResponse badAnswer = new ChatResponse();
        badAnswer.setAnswer("model answer without tool facts");
        when(agentChatService.chat(any(), any())).thenReturn(badAnswer);
        MultiAgentCoordinatorService service = service(properties, mockRagService(),
                mockToolCallingChatServiceWithSummary("material summary MAT-001"), agentChatService);

        MultiAgentChatResponse response = service.chat(ragToolRequest("run-repair-round-limit"), context());

        assertFalse(response.isRepairAttempted());
        assertEquals(0, response.getRepairCount());
        assertEquals(1, response.getRoundCount());
        assertEquals(true, response.getConstraints().get("exceeded"));
        assertTrue(response.getTerminatedReason().contains("maxRounds"));
    }

    @Test
    void shouldRepairWithModelWhenEnabled() {
        AiAgentProperties properties = defaultProperties();
        properties.getMultiAgent().setModelSummaryEnabled(true);
        properties.getMultiAgent().setReviewRepairEnabled(true);
        properties.getMultiAgent().setRepairMode("model");
        AgentChatService agentChatService = mock(AgentChatService.class);
        ChatResponse badAnswer = new ChatResponse();
        badAnswer.setAnswer("model answer without tool facts");
        ChatResponse repairedAnswer = new ChatResponse();
        repairedAnswer.setAnswer("KnowledgeAgent rule summary and Tool result: material summary MAT-001");
        when(agentChatService.chat(any(), any())).thenReturn(badAnswer, repairedAnswer);
        MultiAgentCoordinatorService service = service(properties, mockRagService(),
                mockToolCallingChatServiceWithSummary("material summary MAT-001"), agentChatService);

        MultiAgentChatResponse response = service.chat(ragToolRequest("run-model-repair"), context());

        assertTrue(response.isRepairAttempted());
        assertEquals(1, response.getRepairCount());
        assertEquals("model", response.getRepairMode());
        assertFalse(response.isRepairFallbackUsed());
        assertEquals(true, response.getReviewAfterRepair().get("passed"));
        assertTrue(response.getAnswer().contains("material summary MAT-001"));
    }

    @Test
    void shouldFallbackToTemplateWhenModelRepairFails() {
        AiAgentProperties properties = defaultProperties();
        properties.getMultiAgent().setModelSummaryEnabled(true);
        properties.getMultiAgent().setReviewRepairEnabled(true);
        properties.getMultiAgent().setRepairMode("model");
        AgentChatService agentChatService = mock(AgentChatService.class);
        ChatResponse badAnswer = new ChatResponse();
        badAnswer.setAnswer("model answer without tool facts");
        when(agentChatService.chat(any(), any())).thenReturn(badAnswer).thenThrow(new IllegalStateException("repair down"));
        MultiAgentCoordinatorService service = service(properties, mockRagService(),
                mockToolCallingChatServiceWithSummary("material summary MAT-001"), agentChatService);

        MultiAgentChatResponse response = service.chat(ragToolRequest("run-model-repair-fallback"), context());

        assertTrue(response.isRepairAttempted());
        assertEquals("template", response.getRepairMode());
        assertTrue(response.isRepairFallbackUsed());
        assertEquals(true, response.getReviewAfterRepair().get("passed"));
        assertTrue(response.getAnswer().contains("material summary MAT-001"));
    }

    @Test
    void shouldNotReadOrWriteMemoryWhenDisabled() {
        MultiAgentCoordinatorService service = service(defaultProperties(), mockRagService(),
                mockToolCallingChatService(), mock(AgentChatService.class));
        MultiAgentChatRequest request = ragToolRequest("run-memory-disabled");
        request.setConversationId("conv-1");
        request.setMemoryEnabled(true);

        MultiAgentChatResponse response = service.chat(request, context());

        assertFalse(response.isMemoryEnabled());
        assertEquals(0, response.getMemoryReadCount());
        assertEquals(0, response.getMemoryWriteCount());
    }

    @Test
    void shouldReadAndWriteSafeMemoryWhenEnabled() {
        AiAgentProperties properties = defaultProperties();
        properties.getMultiAgent().setMemoryEnabled(true);
        properties.getMultiAgent().setMemoryReadLimit(3);
        MultiAgentCoordinatorService service = service(properties, mockRagService(),
                mockToolCallingChatService(), mock(AgentChatService.class));

        MultiAgentChatRequest first = ragToolRequest("run-memory-1");
        first.setConversationId("conv-1");
        first.setMemoryEnabled(true);
        MultiAgentChatResponse firstResponse = service.chat(first, context());
        assertTrue(firstResponse.isMemoryEnabled());
        assertEquals(0, firstResponse.getMemoryReadCount());
        assertTrue(firstResponse.getMemoryWriteCount() > 0);

        MultiAgentChatRequest second = ragToolRequest("run-memory-2");
        second.setConversationId("conv-1");
        second.setMemoryEnabled(true);
        MultiAgentChatResponse secondResponse = service.chat(second, context());

        assertTrue(secondResponse.isMemoryEnabled());
        assertTrue(secondResponse.getMemoryReadCount() > 0);
        assertTrue(String.valueOf(secondResponse.getMemory()).contains("FINAL_ANSWER_SUMMARY"));
        assertFalse(String.valueOf(secondResponse.getMemory()).toLowerCase().contains("authorization"));
        assertFalse(String.valueOf(secondResponse.getMemory()).toLowerCase().contains("rawdata"));
    }

    private MultiAgentChatRequest ragToolRequest(String runId) {
        MultiAgentChatRequest request = new MultiAgentChatRequest();
        request.setRunId(runId);
        request.setKnowledgeBaseId("kb-scm-demo");
        request.setMessage("explain inventory available quantity rule and query material MAT-001 inventory");
        request.setRequestedDomain("mdm");
        return request;
    }

    private MultiAgentCoordinatorService service(AiAgentProperties properties, RagService ragService,
                                                 ToolCallingChatService toolCallingChatService,
                                                 AgentChatService agentChatService) {
        return new MultiAgentCoordinatorService(
                properties,
                new MultiAgentDefinitionRegistry(),
                new InMemoryMultiAgentRunStore(properties),
                new MultiAgentPlannerService(),
                new MultiAgentKnowledgeService(ragService),
                new MultiAgentToolService(toolCallingChatService),
                new MultiAgentReviewService(),
                new MultiAgentMemoryService(new InMemoryMultiAgentMemoryStore(properties), properties),
                agentChatService);
    }

    private AiAgentProperties defaultProperties() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getMultiAgent().setEnabled(true);
        return properties;
    }

    private RagService mockRagService() {
        RagService ragService = mock(RagService.class);
        RagRetrieveResponse ragResponse = new RagRetrieveResponse();
        ragResponse.setKnowledgeBaseId("kb-scm-demo");
        ragResponse.setRetrievedCount(1);
        ragResponse.setChunks(List.of(RagRetrievedChunk.builder()
                .documentId("doc-1")
                .chunkId("chunk-1")
                .title("inventory rule")
                .source("docs/examples/scm-wms-rules.md")
                .content("available quantity equals on hand quantity minus locked quantity")
                .score(0.9)
                .build()));
        when(ragService.retrieve(any(RagRetrieveRequest.class), any())).thenReturn(ragResponse);
        return ragService;
    }

    private ToolCallingChatService mockToolCallingChatService() {
        ToolCallingChatService toolCallingChatService = mock(ToolCallingChatService.class);
        when(toolCallingChatService.chat(any(), any())).thenReturn(ToolCallingChatResponse.builder()
                .runId("run-multi-agent-1")
                .selectedTool("mdm.getMaterial")
                .toolArguments(Map.of("materialCode", "MAT-001"))
                .execution(ToolCallingExecutionView.builder()
                        .success(true)
                        .toolName("mdm.getMaterial")
                        .data(ToolCallingDisplayData.builder()
                                .displayTitle("Material")
                                .displaySummary("material summary MAT-001")
                                .displayFields(List.of(ToolCallingDisplayField.builder()
                                        .key("materialCode")
                                        .label("Material Code")
                                        .value("MAT-001")
                                        .build()))
                                .displayItems(List.of())
                                .rawData(Map.of("authorization", "secret"))
                                .build())
                        .latencyMs(10)
                        .build())
                .answer("query success")
                .latencyMs(11)
                .build());
        return toolCallingChatService;
    }

    private ToolCallingChatService mockToolCallingChatServiceWithSummary(String displaySummary) {
        ToolCallingChatService toolCallingChatService = mock(ToolCallingChatService.class);
        when(toolCallingChatService.chat(any(), any())).thenReturn(ToolCallingChatResponse.builder()
                .runId("run-multi-agent-1")
                .selectedTool("mdm.getMaterial")
                .toolArguments(Map.of("materialCode", "MAT-001"))
                .execution(ToolCallingExecutionView.builder()
                        .success(true)
                        .toolName("mdm.getMaterial")
                        .data(ToolCallingDisplayData.builder()
                                .displayTitle("Material")
                                .displaySummary(displaySummary)
                                .displayFields(List.of(ToolCallingDisplayField.builder()
                                        .key("materialCode")
                                        .label("Material Code")
                                        .value("MAT-001")
                                        .build()))
                                .displayItems(List.of())
                                .rawData(Map.of("authorization", "secret"))
                                .build())
                        .latencyMs(10)
                        .build())
                .answer("query success")
                .latencyMs(11)
                .build());
        return toolCallingChatService;
    }

    private AgentRequestContext context() {
        return new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));
    }
}
