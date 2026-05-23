package com.example.scm.aiagent.agent.service;

import com.example.scm.aiagent.agent.dto.AgentRagView;
import com.example.scm.aiagent.agent.dto.AgentToolView;
import com.example.scm.aiagent.agent.prompt.AgentPromptBuildRequest;
import com.example.scm.aiagent.agent.prompt.AgentPromptBuildResult;
import com.example.scm.aiagent.agent.prompt.AgentPromptContext;
import com.example.scm.aiagent.agent.prompt.AgentPromptContextAssembler;
import com.example.scm.aiagent.agent.prompt.AgentPromptContextRenderer;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationRun;
import org.springframework.stereotype.Component;

/**
 * RAG + Tool 组合回答提示词构建器。
 *
 * <p>Phase 5.2 起主路径改为先构造结构化 Prompt Context，再统一渲染模型输入；
 * 该类保留原有 build 入口，避免影响调用方。</p>
 */
@Component
public class RagToolAnswerPromptBuilder {

    private final AgentPromptContextAssembler contextAssembler;
    private final AgentPromptContextRenderer contextRenderer;

    public RagToolAnswerPromptBuilder(AgentPromptContextAssembler contextAssembler,
                                      AgentPromptContextRenderer contextRenderer) {
        this.contextAssembler = contextAssembler;
        this.contextRenderer = contextRenderer;
    }

    public String build(String userMessage,
                        AgentIntentType intentType,
                        AgentRagView rag,
                        AgentToolView tool,
                        ToolOrchestrationRun orchestrationRun) {
        return buildResult(null, userMessage, intentType, rag, tool, orchestrationRun).prompt();
    }

    public AgentPromptBuildResult buildResult(String runId,
                                              String userMessage,
                                              AgentIntentType intentType,
                                              AgentRagView rag,
                                              AgentToolView tool,
                                              ToolOrchestrationRun orchestrationRun) {
        AgentPromptContext assemble = contextAssembler.assemble(AgentPromptBuildRequest.builder()
                .runId(runId)
                .userMessage(userMessage)
                .intentType(intentType)
                .rag(rag)
                .tool(tool)
                .orchestrationRun(orchestrationRun)
                .build());
        AgentPromptContext context = assemble;
        return new AgentPromptBuildResult(
                contextRenderer.render(context),
                context,
                context.sectionCount(),
                context.includedSectionCount(),
                context.truncatedSectionCount());
    }
}
