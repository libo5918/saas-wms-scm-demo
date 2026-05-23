package com.example.scm.aiagent.agent.prompt;

import com.example.scm.aiagent.agent.dto.AgentRagView;
import com.example.scm.aiagent.agent.dto.AgentToolView;
import com.example.scm.aiagent.agent.service.AgentIntentType;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationRun;
import lombok.Builder;
import lombok.Getter;

/**
 * 构造 Agent prompt context 的输入快照。
 */
@Getter
@Builder
public class AgentPromptBuildRequest {

    private String runId;
    private String userMessage;
    private AgentIntentType intentType;
    private AgentRagView rag;
    private AgentToolView tool;
    private ToolOrchestrationRun orchestrationRun;
}
