package com.example.scm.aiagent.toolcalling.orchestrator;

import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatRequest;
import lombok.Builder;
import lombok.Getter;

/**
 * Orchestrator 执行上下文。
 */
@Getter
@Builder
public class ToolOrchestrationContext {

    /** Tool Calling Chat 请求。 */
    private ToolCallingChatRequest request;

    /** gateway 透传的租户和用户上下文。 */
    private AgentRequestContext agentContext;

    /** 本次运行 ID。 */
    private String runId;

    /** Planner 模式。 */
    private String plannerMode;

    /** Answer 模式。 */
    private String answerMode;
}
