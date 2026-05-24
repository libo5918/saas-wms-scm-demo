package com.example.scm.aiagent.multiagent;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.multiagent.dto.MultiAgentChatRequest;
import com.example.scm.aiagent.multiagent.dto.MultiAgentChatResponse;
import com.example.scm.aiagent.multiagent.model.MultiAgentRole;
import com.example.scm.aiagent.multiagent.model.MultiAgentRunStatus;
import com.example.scm.aiagent.multiagent.model.MultiAgentStepStatus;
import com.example.scm.aiagent.multiagent.service.MultiAgentCoordinatorService;
import com.example.scm.aiagent.multiagent.service.MultiAgentDefinitionRegistry;
import com.example.scm.aiagent.multiagent.store.InMemoryMultiAgentRunStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiAgentCoordinatorServiceTest {

    @Test
    void shouldCreateSkeletonRunWithCoordinatorAndPlanner() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getMultiAgent().setEnabled(true);
        MultiAgentCoordinatorService service = new MultiAgentCoordinatorService(
                properties, new MultiAgentDefinitionRegistry(), new InMemoryMultiAgentRunStore(properties));

        MultiAgentChatRequest request = new MultiAgentChatRequest();
        request.setRunId("run-multi-agent-1");
        request.setMessage("按库存可用数量口径解释，并查物料 MAT-001 的库存");

        MultiAgentChatResponse response = service.chat(request,
                new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN")));

        assertEquals("run-multi-agent-1", response.getRunId());
        assertEquals(MultiAgentRunStatus.SUCCESS, response.getStatus());
        assertTrue(response.getAnswer().contains("Multi-Agent"));
        assertTrue(response.getAgents().stream().anyMatch(agent ->
                "CoordinatorAgent".equals(agent.getAgentName())
                        && agent.getRole() == MultiAgentRole.COORDINATOR
                        && agent.getStatus() == MultiAgentStepStatus.SUCCESS));
        assertTrue(response.getAgents().stream().anyMatch(agent ->
                "PlannerAgent".equals(agent.getAgentName())
                        && agent.getRole() == MultiAgentRole.PLANNER
                        && agent.getStatus() == MultiAgentStepStatus.SUCCESS));
        assertEquals(2, response.getSteps().size());
        assertFalse(response.getMessages().isEmpty());
        assertFalse(response.toString().toLowerCase().contains("authorization"));
        assertFalse(response.toString().toLowerCase().contains("rawdata"));
    }

    @Test
    void shouldQuerySavedRun() {
        AiAgentProperties properties = new AiAgentProperties();
        MultiAgentCoordinatorService service = new MultiAgentCoordinatorService(
                properties, new MultiAgentDefinitionRegistry(), new InMemoryMultiAgentRunStore(properties));

        MultiAgentChatRequest request = new MultiAgentChatRequest();
        request.setRunId("run-query");
        request.setMessage("解释库存规则");
        service.chat(request, new AgentRequestContext(1L, 10001L, "admin", List.of()));

        MultiAgentChatResponse response = service.getRun("run-query");

        assertNotNull(response);
        assertEquals("run-query", response.getRunId());
        assertEquals(MultiAgentRunStatus.SUCCESS, response.getStatus());
    }
}
