package com.example.scm.aiagent.agent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG + Tool 组合问答请求。
 *
 * <p>该 DTO 面向企业级 Agent 展示入口，允许前端一次性传入知识库检索参数和 Tool 路由提示。</p>
 */
@Getter
@Setter
public class AgentChatRequest {

    /** 用户原始问题。 */
    @NotBlank(message = "message must not be blank")
    private String message;

    /** 本次运行 ID，不传时服务端自动生成。 */
    private String runId;

    /** 可选知识库 ID；不传时只按意图执行 Tool 或普通模型回答。 */
    private String knowledgeBaseId;

    /** RAG 召回数量。 */
    private Integer topK;

    /** RAG 最低相似度。 */
    private Double scoreThreshold;

    /** RAG metadata 过滤条件。 */
    private Map<String, Object> filters = new HashMap<>();

    /** Tool Calling planner 模式。 */
    private String plannerMode;

    /** 显式指定 Tool。 */
    private String requestedTool;

    /** 显式 Tool 参数。 */
    private Map<String, Object> toolArguments = new HashMap<>();

    /** Tool 候选过滤的业务域提示。 */
    private String requestedDomain;

    /** Tool 候选过滤的类别提示。 */
    private String requestedCategory;

    /** Tool 候选过滤的标签提示。 */
    private List<String> routeTags;
}
