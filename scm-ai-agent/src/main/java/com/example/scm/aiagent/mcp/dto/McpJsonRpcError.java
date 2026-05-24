package com.example.scm.aiagent.mcp.dto;

import lombok.Builder;
import lombok.Getter;

/** MCP JSON-RPC 错误视图，避免把内部异常栈或敏感信息暴露给外部客户端。 */
@Getter
@Builder
public class McpJsonRpcError {

    /** JSON-RPC 错误码。 */
    private Integer code;

    /** 对外可读错误说明。 */
    private String message;

    /** 脱敏后的错误扩展信息。 */
    private Object data;
}
