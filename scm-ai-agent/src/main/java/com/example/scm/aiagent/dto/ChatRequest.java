package com.example.scm.aiagent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI Chat 请求参数。
 */
@Getter
@Setter
public class ChatRequest {

    /**
     * 用户输入的自然语言问题或指令。
     */
    @NotBlank(message = "message must not be blank")
    private String message;

    /**
     * 会话 ID，不传时服务端自动生成。
     */
    private String conversationId;

    /**
     * 任务类型，例如 simple_chat、summary、workflow_planning。
     */
    private String taskType = "simple_chat";

    /**
     * 调用方显式指定的逻辑模型名称。
     */
    private String requestedModel;

    /**
     * 本次请求期望使用的 provider 模式，默认由 ai.agent.provider-mode 决定。
     */
    private String providerMode;

    /**
     * 本次任务要求模型具备的能力标签。
     */
    private List<String> requiredCapabilities;

    /**
     * 成本等级偏好，例如 low、medium、high。
     */
    private String costLevel;

    /**
     * 最大可接受延迟，单位毫秒。
     */
    private Long maxLatencyMs;

    /**
     * 调用方传入的扩展元数据，后续可承载业务单号、来源页面等信息。
     */
    private Map<String, Object> metadata = new HashMap<>();
}
