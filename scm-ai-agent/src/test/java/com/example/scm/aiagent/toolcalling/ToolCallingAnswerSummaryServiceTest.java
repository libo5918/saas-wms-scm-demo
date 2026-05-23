package com.example.scm.aiagent.toolcalling;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.model.ChatModelInvocation;
import com.example.scm.aiagent.model.ChatModelResult;
import com.example.scm.aiagent.model.ModelRoute;
import com.example.scm.aiagent.service.ChatModelClient;
import com.example.scm.aiagent.service.ModelRouter;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatRequest;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecutionView;
import com.example.scm.aiagent.toolcalling.model.ToolCallingAnswerSummaryResult;
import com.example.scm.aiagent.toolcalling.model.ToolCallingPlan;
import com.example.scm.aiagent.toolcalling.answer.ToolCallingAnswerBuilder;
import com.example.scm.aiagent.toolcalling.answer.ToolCallingAnswerPromptBuilder;
import com.example.scm.aiagent.toolcalling.answer.strategy.ToolCallingAnswerPromptStrategy;
import com.example.scm.aiagent.toolcalling.answer.strategy.ToolCallingAnswerPromptStrategyRegistry;
import com.example.scm.aiagent.toolcalling.answer.ToolCallingAnswerSummaryService;
import com.example.scm.aiagent.toolcalling.display.ToolCallingDisplaySchemaBuilder;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationExecutionSummary;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationRun;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationStep;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationStepStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ToolCallingAnswerSummaryServiceTest {

    private ModelRouter modelRouter;
    private ChatModelClient chatModelClient;
    private AiAgentProperties properties;
    private ToolCallingAnswerSummaryService service;
    private ToolCallingDisplaySchemaBuilder displaySchemaBuilder;
    private AgentRequestContext context;
    private ToolCallingPlan plan;
    private ToolCallingChatRequest request;

    @BeforeEach
    void setUp() {
        modelRouter = mock(ModelRouter.class);
        chatModelClient = mock(ChatModelClient.class);
        properties = new AiAgentProperties();
        displaySchemaBuilder = new ToolCallingDisplaySchemaBuilder();
        service = new ToolCallingAnswerSummaryService(
                properties,
                modelRouter,
                chatModelClient,
                new ToolCallingAnswerBuilder(),
                new ToolCallingAnswerPromptBuilder(new ObjectMapper(), promptStrategyRegistry())
        );
        context = new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));
        plan = ToolCallingPlan.builder()
                .plannerMode("spring-ai")
                .planningSource("spring-ai")
                .fallbackUsed(false)
                .selectedTool("mdm.getMaterial")
                .toolArguments(Map.of("materialCode", "MAT-001"))
                .reason("plan")
                .build();
        request = new ToolCallingChatRequest();
        request.setMessage("帮我查物料 MAT-001");
    }

    @Test
    void shouldUseTemplateModeByDefault() {
        ToolCallingExecutionView execution = ToolCallingExecutionView.builder()
                .success(true)
                .toolName("mdm.getMaterial")
                .data(Map.of("materialCode", "MAT-001", "materialName", "标准零件", "status", "ENABLED"))
                .latencyMs(8)
                .build();

        ToolCallingAnswerSummaryResult result = service.summarize(request, context, plan, execution, "run-answer-1");

        assertEquals("template", result.answerMode());
        assertFalse(result.fallbackUsed());
        assertTrue(result.answer().contains("标准零件"));
    }

    @Test
    void shouldUseSpringAiAnswerWhenEnabled() {
        properties.getToolCalling().setAnswerMode("spring-ai");
        properties.getToolCalling().getSpringAiAnswer().setEnabled(true);
        when(modelRouter.route(any())).thenReturn(new ModelRoute(
                "qwen-plus",
                "qwen-plus",
                "dashscope",
                "dashscope",
                "spring-ai",
                "task_type:tool_calling_answer",
                List.of("CHAT"),
                List.of("qwen-turbo")
        ));
        when(chatModelClient.chat(any())).thenReturn(new ChatModelResult("物料 MAT-001 当前状态为 ENABLED，名称是标准零件。"));

        ToolCallingExecutionView execution = ToolCallingExecutionView.builder()
                .success(true)
                .toolName("mdm.getMaterial")
                .data(displaySchemaBuilder.build("mdm.getMaterial",
                        Map.of("materialCode", "MAT-001", "materialName", "标准零件", "status", "ENABLED")))
                .latencyMs(8)
                .build();

        ToolCallingAnswerSummaryResult result = service.summarize(request, context, plan, execution, "run-answer-2");

        assertEquals("spring-ai", result.answerMode());
        assertFalse(result.fallbackUsed());
        assertTrue(result.answer().contains("MAT-001"));
        ArgumentCaptor<ChatModelInvocation> invocationCaptor = ArgumentCaptor.forClass(ChatModelInvocation.class);
        verify(chatModelClient).chat(invocationCaptor.capture());
        assertTrue(invocationCaptor.getValue().message().contains("\"displayTitle\""));
        assertTrue(invocationCaptor.getValue().message().contains("物料信息"));
        assertTrue(invocationCaptor.getValue().message().contains("\"rawData\""));
    }

    @Test
    void shouldFallbackToTemplateWhenSpringAiAnswerFails() {
        properties.getToolCalling().setAnswerMode("spring-ai");
        properties.getToolCalling().getSpringAiAnswer().setEnabled(true);
        properties.getToolCalling().getSpringAiAnswer().setFallbackToTemplate(true);
        when(modelRouter.route(any())).thenThrow(new IllegalStateException("model unavailable"));

        ToolCallingExecutionView execution = ToolCallingExecutionView.builder()
                .success(false)
                .toolName("mdm.getMaterial")
                .errorCode("404")
                .errorMessage("MDM service failed: Material not found")
                .latencyMs(8)
                .build();

        ToolCallingAnswerSummaryResult result = service.summarize(request, context, plan, execution, "run-answer-3");

        assertEquals("template", result.answerMode());
        assertTrue(result.fallbackUsed());
        assertTrue(result.answer().contains("Material not found"));
    }

    @Test
    void shouldIncludeControlledFollowUpSummaryInSpringAiPrompt() {
        properties.getToolCalling().setAnswerMode("spring-ai");
        properties.getToolCalling().getSpringAiAnswer().setEnabled(true);
        when(modelRouter.route(any())).thenReturn(new ModelRoute(
                "qwen-plus",
                "qwen-plus",
                "dashscope",
                "dashscope",
                "spring-ai",
                "task_type:tool_calling_answer",
                List.of("CHAT"),
                List.of("qwen-turbo")
        ));
        when(chatModelClient.chat(any())).thenReturn(new ChatModelResult("物料 MAT-001 的可用库存为 128，锁定库存为 12。"));

        ToolCallingExecutionView execution = ToolCallingExecutionView.builder()
                .success(true)
                .toolName("mdm.getMaterial")
                .data(displaySchemaBuilder.build("mdm.getMaterial",
                        Map.of("id", 1001L, "materialCode", "MAT-001", "materialName", "螺丝")))
                .latencyMs(8)
                .build();
        ToolOrchestrationRun run = ToolOrchestrationRun.builder()
                .runId("run-answer-5")
                .steps(List.of(
                        ToolOrchestrationStep.builder()
                                .stepNo(1)
                                .stepRef("step-1")
                                .toolName("mdm.getMaterial")
                                .status(ToolOrchestrationStepStatus.SUCCESS)
                                .executed(true)
                                .outputSummary("material queried")
                                .execution(ToolOrchestrationExecutionSummary.builder()
                                        .success(true)
                                        .toolName("mdm.getMaterial")
                                        .displayTitle("物料信息")
                                        .displaySummary("已查询到物料 MAT-001")
                                        .safeFields(Map.of("materialId", 1001L, "materialCode", "MAT-001"))
                                        .latencyMs(8)
                                        .build())
                                .build(),
                        ToolOrchestrationStep.builder()
                                .stepNo(2)
                                .stepRef("step-2")
                                .toolName("inventory.getBalance")
                                .status(ToolOrchestrationStepStatus.SUCCESS)
                                .executed(true)
                                .inputResolved(true)
                                .outputSummary("inventory queried")
                                .execution(ToolOrchestrationExecutionSummary.builder()
                                        .success(true)
                                        .toolName("inventory.getBalance")
                                        .displayTitle("库存余额")
                                        .displaySummary("已查询到库存余额，物料 1001 可用 128")
                                        .safeFields(Map.of("materialId", 1001L, "warehouseId", 2001L,
                                                "locationId", 3001L, "availableQty", 128, "lockedQty", 12))
                                        .latencyMs(10)
                                        .build())
                                .build()))
                .build();

        ToolCallingAnswerSummaryResult result = service.summarize(request, context, plan, execution, "run-answer-5", run);

        assertEquals("spring-ai", result.answerMode());
        ArgumentCaptor<ChatModelInvocation> invocationCaptor = ArgumentCaptor.forClass(ChatModelInvocation.class);
        verify(chatModelClient).chat(invocationCaptor.capture());
        String prompt = invocationCaptor.getValue().message();
        assertTrue(prompt.contains("\"orchestrationSteps\""));
        assertTrue(prompt.contains("inventory.getBalance"));
        assertTrue(prompt.contains("availableQty"));
        assertTrue(prompt.contains("128"));
    }

    @Test
    void shouldThrowExceptionWhenSpringAiAnswerDisabledAndNoFallback() {
        properties.getToolCalling().setAnswerMode("spring-ai");
        properties.getToolCalling().getSpringAiAnswer().setEnabled(false);
        properties.getToolCalling().getSpringAiAnswer().setFallbackToTemplate(false);

        ToolCallingExecutionView execution = ToolCallingExecutionView.builder()
                .success(true)
                .toolName("mdm.getMaterial")
                .data(Map.of("materialCode", "MAT-001"))
                .latencyMs(8)
                .build();

        try {
            service.summarize(request, context, plan, execution, "run-answer-4");
            throw new AssertionError("Expected BusinessException to be thrown");
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("Spring AI answer summary is disabled"));
        }
    }

    private ToolCallingAnswerPromptStrategyRegistry promptStrategyRegistry() {
        ToolCallingAnswerPromptStrategy materialStrategy = new ToolCallingAnswerPromptStrategy() {
            @Override
            public boolean supports(String toolName) {
                return "mdm.getMaterial".equals(toolName);
            }

            @Override
            public String instructions() {
                return "物料查询结果：优先说明物料编码、物料名称、状态、单位和分类。";
            }
        };
        return new ToolCallingAnswerPromptStrategyRegistry(List.of(materialStrategy));
    }
}
