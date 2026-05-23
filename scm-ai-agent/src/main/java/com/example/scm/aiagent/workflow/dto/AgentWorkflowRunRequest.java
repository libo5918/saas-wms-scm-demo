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
}
