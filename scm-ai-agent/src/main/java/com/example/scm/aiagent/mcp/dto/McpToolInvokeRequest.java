package com.example.scm.aiagent.mcp.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/** MCP 风格 Tool 调用请求。 */
@Getter
@Setter
public class McpToolInvokeRequest {

    private String runId;

    /** Tool 参数。字段名使用 arguments 以贴近 MCP tool call 语义。 */
    private Map<String, Object> arguments = new HashMap<>();

    /** 兼容通用 HTTP 调用方传 parameters 的场景。 */
    private Map<String, Object> parameters = new HashMap<>();

    public Map<String, Object> effectiveArguments() {
        if (arguments != null && !arguments.isEmpty()) {
            return arguments;
        }
        return parameters == null ? Map.of() : parameters;
    }
}
