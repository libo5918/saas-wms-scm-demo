package com.example.scm.aiagent.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/** Workflow 运行请求。 */
@Getter
@Setter
public class AgentWorkflowRunRequest {

    private String runId;

    @NotBlank(message = "message is required")
    private String message;

    private Map<String, Object> parameters = new HashMap<>();
    private String plannerMode;
    private String answerMode;

    /** Summary 阶段可选知识库 ID；为空时保持 Phase 6.1 行为，不执行 RAG 检索。 */
    private String knowledgeBaseId;

    /** RAG 检索返回片段数。 */
    private Integer topK;

    /** RAG 检索相似度阈值。 */
    private Double scoreThreshold;

    /** RAG metadata 过滤条件。 */
    private Map<String, Object> filters = new HashMap<>();
}
