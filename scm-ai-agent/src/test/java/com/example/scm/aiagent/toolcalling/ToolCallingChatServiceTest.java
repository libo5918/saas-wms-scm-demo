package com.example.scm.aiagent.toolcalling;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatRequest;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatResponse;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecuteResponse;
import com.example.scm.aiagent.toolcalling.service.MockToolPlanner;
import com.example.scm.aiagent.toolcalling.service.SpringAiToolCallingService;
import com.example.scm.aiagent.toolcalling.service.ToolCallingChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ToolCallingChatServiceTest {

    private SpringAiToolCallingService springAiToolCallingService;
    private ToolCallingChatService service;
    private AgentRequestContext context;

    @BeforeEach
    void setUp() {
        springAiToolCallingService = mock(SpringAiToolCallingService.class);
        AiAgentProperties properties = new AiAgentProperties();
        properties.getToolCalling().setPlannerMode("mock");
        service = new ToolCallingChatService(properties, new MockToolPlanner(), springAiToolCallingService);
        context = new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));
    }

    @Test
    void shouldUseRequestedToolFirst() {
        when(springAiToolCallingService.execute(any(), eq(context))).thenReturn(ToolCallingExecuteResponse.builder()
                .success(true)
                .toolName("mdm.getMaterial")
                .arguments(Map.of("materialCode", "MAT-001"))
                .latencyMs(5)
                .build());

        ToolCallingChatRequest request = new ToolCallingChatRequest();
        request.setMessage("帮我查库存");
        request.setRequestedTool("mdm.getMaterial");
        request.setToolArguments(Map.of("materialCode", "MAT-001"));
        request.setRunId("run-chat-1");

        ToolCallingChatResponse response = service.chat(request, context);

        assertEquals("mock", response.getPlannerMode());
        assertEquals("mdm.getMaterial", response.getSelectedTool());
        assertEquals("MAT-001", response.getToolArguments().get("materialCode"));
        assertTrue(response.getAnswer().contains("mdm.getMaterial"));
        verify(springAiToolCallingService).execute(any(), eq(context));
    }

    @Test
    void shouldSupportSpringAiPlannerFallback() {
        when(springAiToolCallingService.execute(any(), eq(context))).thenReturn(ToolCallingExecuteResponse.builder()
                .success(true)
                .toolName("sales.getOrder")
                .arguments(Map.of("orderNo", "SO-20260520-001"))
                .latencyMs(5)
                .build());

        ToolCallingChatRequest request = new ToolCallingChatRequest();
        request.setMessage("帮我查一下销售订单");
        request.setPlannerMode("spring-ai");

        ToolCallingChatResponse response = service.chat(request, context);

        assertEquals("spring-ai", response.getPlannerMode());
        assertEquals("sales.getOrder", response.getSelectedTool());
    }
}
