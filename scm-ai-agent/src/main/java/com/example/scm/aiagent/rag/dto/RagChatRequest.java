package com.example.scm.aiagent.rag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG Chat 请求。
 */
@Getter
@Setter
public class RagChatRequest {

    /** 知识库 ID，RAG Chat 会先在该知识库检索上下文。 */
    @NotBlank(message = "knowledgeBaseId must not be blank")
    private String knowledgeBaseId;

    /** 用户问题，会用于检索和最终模型调用。 */
    @NotBlank(message = "message must not be blank")
    private String message;

    /** 会话 ID，不传时沿用 Chat 服务自动生成逻辑。 */
    private String conversationId;

    /** 任务类型，默认 rag_qa，用于 ModelRouter 选择具备 RAG 能力的模型。 */
    private String taskType = "rag_qa";

    /** 显式指定逻辑模型名称。 */
    private String requestedModel;

    /** provider 模式，mock 表示本地模拟，spring-ai 表示真实模型调用。 */
    private String providerMode;

    /** 本次任务要求的模型能力标签，默认由服务补充 CHAT 和 RAG。 */
    private List<String> requiredCapabilities;

    /** 成本等级偏好。 */
    private String costLevel;

    /** 最大可接受延迟，单位毫秒。 */
    private Long maxLatencyMs;

    /** RAG 检索返回的最大切片数量。 */
    private Integer topK;

    /** 预留 metadata 过滤条件，后续映射到 Milvus scalar filter。 */
    private Map<String, Object> filters = new HashMap<>();

    /** 调用方扩展元数据，透传给 ChatRequest。 */
    private Map<String, Object> metadata = new HashMap<>();
}
