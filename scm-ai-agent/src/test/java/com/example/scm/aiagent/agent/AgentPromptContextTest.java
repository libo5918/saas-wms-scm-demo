package com.example.scm.aiagent.agent;

import com.example.scm.aiagent.agent.dto.AgentRagChunkView;
import com.example.scm.aiagent.agent.dto.AgentRagView;
import com.example.scm.aiagent.agent.dto.AgentToolExecutionView;
import com.example.scm.aiagent.agent.dto.AgentToolView;
import com.example.scm.aiagent.agent.prompt.AgentPromptBuildRequest;
import com.example.scm.aiagent.agent.prompt.AgentPromptContext;
import com.example.scm.aiagent.agent.prompt.AgentPromptContextAssembler;
import com.example.scm.aiagent.agent.prompt.AgentPromptContextProvider;
import com.example.scm.aiagent.agent.prompt.AgentPromptContextRenderer;
import com.example.scm.aiagent.agent.prompt.AgentPromptContextSource;
import com.example.scm.aiagent.agent.prompt.AgentPromptContextType;
import com.example.scm.aiagent.agent.prompt.AgentPromptSanitizer;
import com.example.scm.aiagent.agent.prompt.AgentPromptSection;
import com.example.scm.aiagent.agent.prompt.OrchestrationPromptContextProvider;
import com.example.scm.aiagent.agent.prompt.RagPromptContextProvider;
import com.example.scm.aiagent.agent.prompt.ToolPromptContextProvider;
import com.example.scm.aiagent.agent.service.AgentIntentType;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationExecutionSummary;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationRun;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationStep;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationStepStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentPromptContextTest {

    @Test
    void shouldCreateRagToolAndOrchestrationSections() {
        AgentPromptBuildRequest request = AgentPromptBuildRequest.builder()
                .userMessage("查询物料并解释库存口径")
                .intentType(AgentIntentType.RAG_TOOL)
                .rag(ragView())
                .tool(toolView())
                .orchestrationRun(orchestrationRun())
                .build();

        assertEquals(AgentPromptContextType.RAG_CONTEXT,
                new RagPromptContextProvider().provide(request).get(0).getType());
        assertEquals(AgentPromptContextType.TOOL_EXECUTION,
                new ToolPromptContextProvider().provide(request).get(0).getType());
        assertEquals(AgentPromptContextType.ORCHESTRATION_STEPS,
                new OrchestrationPromptContextProvider().provide(request).get(0).getType());
    }

    @Test
    void shouldSortTruncateAndFilterSensitiveSections() {
        AgentPromptContextAssembler assembler = new AgentPromptContextAssembler(List.of(customProvider()), new AgentPromptSanitizer());

        AgentPromptContext context = assembler.assemble(AgentPromptBuildRequest.builder()
                .runId("run-context")
                .intentType(AgentIntentType.RAG_TOOL)
                .build());

        assertEquals(3, context.sectionCount());
        assertEquals(2, context.includedSectionCount());
        assertEquals(1, context.truncatedSectionCount());
        assertEquals("高优先级", context.getSections().get(0).getTitle());
        assertFalse(context.getSections().get(2).isIncluded());
    }

    @Test
    void rendererShouldContainSectionsAndExcludeSensitiveKeywords() {
        AgentPromptContextAssembler assembler = new AgentPromptContextAssembler(List.of(
                new RagPromptContextProvider(),
                new ToolPromptContextProvider(),
                new OrchestrationPromptContextProvider(),
                customProvider()
        ), new AgentPromptSanitizer());
        AgentPromptContext context = assembler.assemble(AgentPromptBuildRequest.builder()
                .userMessage("查询物料并解释库存口径")
                .intentType(AgentIntentType.RAG_TOOL)
                .rag(ragView())
                .tool(toolView())
                .orchestrationRun(orchestrationRun())
                .build());

        String prompt = new AgentPromptContextRenderer(new ObjectMapper()).render(context);

        assertTrue(prompt.contains("知识库片段"));
        assertTrue(prompt.contains("工具执行结果"));
        assertTrue(prompt.contains("编排步骤摘要"));
        assertTrue(prompt.contains("MAT-001"));
        assertFalse(prompt.contains("rawData"));
        assertFalse(prompt.contains("authorization"));
        assertFalse(prompt.contains("cookie"));
        assertFalse(prompt.contains("token"));
        assertFalse(prompt.contains("API Key"));
    }

    private AgentPromptContextProvider customProvider() {
        return request -> List.of(
                AgentPromptSection.builder()
                        .type(AgentPromptContextType.SYSTEM_INSTRUCTIONS)
                        .source(AgentPromptContextSource.SYSTEM)
                        .title("低优先级")
                        .content("abcdefghijklmnopqrstuvwxyz")
                        .priority(50)
                        .maxLength(5)
                        .included(true)
                        .build(),
                AgentPromptSection.builder()
                        .type(AgentPromptContextType.USER_MESSAGE)
                        .source(AgentPromptContextSource.REQUEST)
                        .title("高优先级")
                        .content("用户问题")
                        .priority(1)
                        .maxLength(100)
                        .included(true)
                        .build(),
                AgentPromptSection.builder()
                        .type(AgentPromptContextType.SAFETY_CONSTRAINTS)
                        .source(AgentPromptContextSource.SYSTEM)
                        .title("敏感片段")
                        .content("authorization token cookie API Key rawData")
                        .priority(99)
                        .maxLength(100)
                        .included(true)
                        .build()
        );
    }

    private AgentRagView ragView() {
        return AgentRagView.builder()
                .knowledgeBaseId("kb-scm")
                .retrievedCount(1)
                .chunks(List.of(AgentRagChunkView.builder()
                        .documentId("doc-1")
                        .chunkId("chunk-1")
                        .title("库存口径")
                        .contentSnippet("库存可用数量等于现存数量减锁定数量")
                        .score(0.9)
                        .build()))
                .build();
    }

    private AgentToolView toolView() {
        return AgentToolView.builder()
                .selectedTool("mdm.getMaterial")
                .toolArguments(Map.of("materialCode", "MAT-001"))
                .execution(AgentToolExecutionView.builder()
                        .success(true)
                        .toolName("mdm.getMaterial")
                        .displayTitle("物料信息")
                        .displaySummary("已查询到物料 MAT-001")
                        .build())
                .build();
    }

    private ToolOrchestrationRun orchestrationRun() {
        return ToolOrchestrationRun.builder()
                .runId("run-context")
                .steps(List.of(ToolOrchestrationStep.builder()
                        .stepNo(1)
                        .stepRef("step-1")
                        .toolName("mdm.getMaterial")
                        .status(ToolOrchestrationStepStatus.SUCCESS)
                        .executed(true)
                        .execution(ToolOrchestrationExecutionSummary.builder()
                                .success(true)
                                .toolName("mdm.getMaterial")
                                .displayTitle("物料信息")
                                .displaySummary("已查询到物料 MAT-001")
                                .safeFields(Map.of("materialCode", "MAT-001"))
                                .build())
                        .build()))
                .build();
    }
}
