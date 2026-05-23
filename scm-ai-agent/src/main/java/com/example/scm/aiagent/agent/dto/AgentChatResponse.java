package com.example.scm.aiagent.agent.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * RAG + Tool 组合问答响应。
 *
 * <p>只返回面试演示需要的概要字段，不暴露完整 prompt、模型响应、token 或业务 rawData。</p>
 */
@Getter
@Builder
public class AgentChatResponse {

    /** 本次组合 Agent 运行 ID。 */
    private String runId;

    /** 意图路由结果：RAG_ONLY、TOOL_ONLY 或 RAG_TOOL。 */
    private String intentType;

    /** 最终中文回答。 */
    private String answer;

    /** RAG 检索概要。 */
    private AgentRagView rag;

    /** Tool Calling 执行概要。 */
    private AgentToolView tool;

    /** Orchestrator run 概要。 */
    private AgentOrchestrationView orchestration;

    /** 是否发生降级。 */
    private boolean fallbackUsed;

    /** 整体耗时。 */
    private long latencyMs;
}
