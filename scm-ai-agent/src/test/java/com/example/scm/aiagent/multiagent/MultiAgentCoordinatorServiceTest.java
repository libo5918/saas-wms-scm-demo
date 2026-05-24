package com.example.scm.aiagent.multiagent;

import com.example.scm.aiagent.config.AiAgentProperties;
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
import com.example.scm.aiagent.multiagent.service.MultiAgentPlannerService;
import com.example.scm.aiagent.multiagent.service.MultiAgentReviewService;
import com.example.scm.aiagent.multiagent.service.MultiAgentToolService;
import com.example.scm.aiagent.multiagent.store.InMemoryMultiAgentRunStore;
import com.example.scm.aiagent.rag.dto.RagRetrieveRequest;
import com.example.scm.aiagent.rag.dto.RagRetrieveResponse;
import com.example.scm.aiagent.rag.dto.RagRetrievedChunk;
import com.example.scm.aiagent.rag.service.RagService;
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
import static org.mockito.Mockito.when;

class MultiAgentCoordinatorServiceTest {

    @Test
    void shouldRunRagToolReviewSingleRound() {
        MultiAgentCoordinatorService service = service();
        MultiAgentChatRequest request = new MultiAgentChatRequest();
        request.setRunId("run-multi-agent-1");
        request.setKnowledgeBaseId("kb-scm-demo");
        request.setMessage("按库存可用数量口径解释，并查物料 MAT-001 的库存");

        MultiAgentChatResponse response = service.chat(request, context());

        assertEquals("run-multi-agent-1", response.getRunId());
        assertEquals(MultiAgentRunStatus.SUCCESS, response.getStatus());
        assertEquals(MultiAgentIntentType.RAG_TOOL, response.getIntentType());
        assertEquals(1, response.getRag().get("retrievedCount"));
        assertEquals("mdm.getMaterial", response.getTool().get("selectedTool"));
        assertEquals(true, response.getReview().get("passed"));
        assertTrue(response.getAnswer().contains("KnowledgeAgent"));
        assertTrue(response.getAnswer().contains("ToolAgent"));
        assertTrue(response.getAgents().stream().anyMatch(agent ->
                "KnowledgeAgent".equals(agent.getAgentName()) && agent.getStatus() == MultiAgentStepStatus.SUCCESS));
        assertTrue(response.getAgents().stream().anyMatch(agent ->
                "ToolAgent".equals(agent.getAgentName()) && agent.getStatus() == MultiAgentStepStatus.SUCCESS));
        assertTrue(response.getAgents().stream().anyMatch(agent ->
                "ReviewerAgent".equals(agent.getAgentName()) && agent.getRole() == MultiAgentRole.REVIEWER));
        assertEquals(6, response.getSteps().size());
        assertFalse(response.toString().toLowerCase().contains("authorization"));
        assertFalse(response.toString().toLowerCase().contains("rawdata"));
    }

    @Test
    void shouldQuerySavedRun() {
        MultiAgentCoordinatorService service = service();
        MultiAgentChatRequest request = new MultiAgentChatRequest();
        request.setRunId("run-query");
        request.setMessage("解释库存规则");
        service.chat(request, context());

        MultiAgentChatResponse response = service.getRun("run-query");

        assertNotNull(response);
        assertEquals("run-query", response.getRunId());
        assertEquals(MultiAgentRunStatus.SUCCESS, response.getStatus());
    }

    private MultiAgentCoordinatorService service() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getMultiAgent().setEnabled(true);
        RagService ragService = mock(RagService.class);
        RagRetrieveResponse ragResponse = new RagRetrieveResponse();
        ragResponse.setKnowledgeBaseId("kb-scm-demo");
        ragResponse.setRetrievedCount(1);
        ragResponse.setChunks(List.of(RagRetrievedChunk.builder()
                .documentId("doc-1")
                .chunkId("chunk-1")
                .title("库存规则")
                .source("docs/examples/scm-wms-rules.md")
                .content("库存可用数量等于现存数量减去锁定数量")
                .score(0.9)
                .build()));
        when(ragService.retrieve(any(RagRetrieveRequest.class), any())).thenReturn(ragResponse);

        ToolCallingChatService toolCallingChatService = mock(ToolCallingChatService.class);
        when(toolCallingChatService.chat(any(), any())).thenReturn(ToolCallingChatResponse.builder()
                .runId("run-multi-agent-1")
                .selectedTool("mdm.getMaterial")
                .toolArguments(Map.of("materialCode", "MAT-001"))
                .execution(ToolCallingExecutionView.builder()
                        .success(true)
                        .toolName("mdm.getMaterial")
                        .data(ToolCallingDisplayData.builder()
                                .displayTitle("物料信息")
                                .displaySummary("已查询到物料 MAT-001")
                                .displayFields(List.of(ToolCallingDisplayField.builder()
                                        .key("materialCode")
                                        .label("物料编码")
                                        .value("MAT-001")
                                        .build()))
                                .displayItems(List.of())
                                .rawData(Map.of("authorization", "secret"))
                                .build())
                        .latencyMs(10)
                        .build())
                .answer("查询成功")
                .latencyMs(11)
                .build());

        return new MultiAgentCoordinatorService(
                properties,
                new MultiAgentDefinitionRegistry(),
                new InMemoryMultiAgentRunStore(properties),
                new MultiAgentPlannerService(),
                new MultiAgentKnowledgeService(ragService),
                new MultiAgentToolService(toolCallingChatService),
                new MultiAgentReviewService());
    }

    private AgentRequestContext context() {
        return new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));
    }
}
