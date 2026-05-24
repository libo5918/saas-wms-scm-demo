package com.example.scm.aiagent.mcp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/** MCP Server HTTP transport 使用的最小 JSON-RPC 响应。 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpJsonRpcResponse {

    /** JSON-RPC 版本，固定为 2.0。 */
    @Builder.Default
    private String jsonrpc = "2.0";

    /** 原样回传请求 ID。 */
    private Object id;

    /** 成功结果。 */
    private Object result;

    /** 失败错误。 */
    private McpJsonRpcError error;

    public static McpJsonRpcResponse success(Object id, Object result) {
        return McpJsonRpcResponse.builder().id(id).result(result).build();
    }

    public static McpJsonRpcResponse failure(Object id, int code, String message, Object data) {
        return McpJsonRpcResponse.builder()
                .id(id)
                .error(McpJsonRpcError.builder()
                        .code(code)
                        .message(message)
                        .data(data)
                        .build())
                .build();
    }
}
