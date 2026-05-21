package com.example.scm.aiagent.toolcalling;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.model.ChatModelResult;
import com.example.scm.aiagent.model.ModelRoute;
import com.example.scm.aiagent.tool.model.ToolDefinition;
import com.example.scm.aiagent.tool.service.ToolRegistry;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatRequest;
import com.example.scm.aiagent.toolcalling.model.ToolCallingPlan;
import com.example.scm.aiagent.toolcalling.planning.SpringAiToolPlanner;
import com.example.scm.aiagent.toolcalling.planning.ToolPlanParser;
import com.example.scm.aiagent.toolcalling.planning.ToolPlanningPromptBuilder;
import com.example.scm.aiagent.toolcalling.schema.ToolSchemaConverter;
import com.example.scm.aiagent.service.ChatModelClient;
import com.example.scm.aiagent.service.ModelRouter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringAiToolPlannerTest {

    private ModelRouter modelRouter;
    private ChatModelClient chatModelClient;
    private ToolRegistry toolRegistry;
    private SpringAiToolPlanner planner;
    private AgentRequestContext context;

    @BeforeEach
    void setUp() {
        modelRouter = mock(ModelRouter.class);
        chatModelClient = mock(ChatModelClient.class);
        toolRegistry = mock(ToolRegistry.class);

        AiAgentProperties properties = new AiAgentProperties();
        properties.getToolCalling().getSpringAiPlanner().setEnabled(true);
        properties.getToolCalling().getSpringAiPlanner().setMaxRetries(1);
        properties.getToolCalling().getSpringAiPlanner().setTaskType("tool_calling");

        ToolDefinition definition = ToolDefinition.builder()
                .name("mdm.getMaterial")
                .domain("mdm")
                .description("查询物料信息")
                .readOnly(true)
                .parameters(Map.of("materialCode", "物料编码"))
                .requiredParameters(List.of())
                .oneOfRequiredGroups(List.of(List.of("materialId", "materialCode")))
                .build();

        when(toolRegistry.listDefinitions()).thenReturn(List.of(definition));
        when(toolRegistry.findDefinition("mdm.getMaterial")).thenReturn(Optional.of(definition));
        when(modelRouter.route(any())).thenReturn(new ModelRoute(
                "qwen-plus",
                "qwen-plus",
                "dashscope",
                "dashscope",
                "spring-ai",
                "task_type:tool_calling",
                List.of("TOOL_CALLING", "STRUCTURED_OUTPUT"),
                List.of("qwen-turbo")
        ));
        when(chatModelClient.chat(any())).thenReturn(new ChatModelResult("""
                {"toolName":"mdm.getMaterial","arguments":{"materialCode":"MAT-001"},"reason":"用户在查物料"}
                """));

        planner = new SpringAiToolPlanner(
                properties,
                modelRouter,
                chatModelClient,
                toolRegistry,
                new ToolSchemaConverter(),
                new ToolPlanningPromptBuilder(new ObjectMapper()),
                new ToolPlanParser(new ObjectMapper())
        );
        context = new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));
    }

    @Test
    void shouldPlanWithRealModelResponse() {
        ToolCallingChatRequest request = new ToolCallingChatRequest();
        request.setMessage("帮我查一下物料 MAT-001");

        ToolCallingPlan plan = planner.plan(request, context, "run-plan-1");

        assertEquals("spring-ai", plan.plannerMode());
        assertEquals("spring-ai", plan.planningSource());
        assertEquals("mdm.getMaterial", plan.selectedTool());
        assertEquals("MAT-001", plan.toolArguments().get("materialCode"));
    }
}
