package com.example.scm.aiagent.toolcalling.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Tool Calling 调试执行请求。
 */
@Getter
@Setter
public class ToolCallingExecuteRequest {

    /** 工具名称。 */
    @NotBlank(message = "toolName is required")
    private String toolName;

    /** 本次 Tool Calling 的运行 ID。 */
    private String runId;

    /** 模拟模型返回的 arguments。 */
    private Map<String, Object> arguments = new HashMap<>();
}
