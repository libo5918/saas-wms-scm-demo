package com.example.scm.aiagent.multiagent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

/** Multi-Agent Chat 请求，本阶段主要使用 runId/message/mode，其余字段预留后续接入。 */
@Getter
@Setter
public class MultiAgentChatRequest {

    private String runId;

    @NotBlank(message = "message must not be blank")
    private String message;

    private String mode;
    private String knowledgeBaseId;
    private Integer topK;
    private Double scoreThreshold;
    private Map<String, Object> filters = new HashMap<>();
    private String plannerMode;
    private String requestedTool;
    private String requestedDomain;
    private List<String> routeTags;
}
