package com.example.scm.aiagent.mcp.dto;

import lombok.Builder;
import lombok.Getter;

/** MCP 风格 Tool 调用响应，只返回安全展示结果。 */
@Getter
@Builder
public class McpToolInvokeResponse {

    private String runId;
    private String toolName;
    private boolean success;
    private String errorCode;
    private String errorMessage;
    private McpToolDisplayView display;
    private long latencyMs;
}
