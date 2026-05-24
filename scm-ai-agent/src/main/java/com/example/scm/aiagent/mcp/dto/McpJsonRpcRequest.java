package com.example.scm.aiagent.mcp.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/** MCP Server HTTP transport 使用的最小 JSON-RPC 请求。 */
@Getter
@Setter
public class McpJsonRpcRequest {

    /** JSON-RPC 版本，当前固定兼容 2.0。 */
    private String jsonrpc = "2.0";

    /** 请求 ID，支持字符串或数字。 */
    private Object id;

    /** MCP 方法名，例如 tools/list、tools/call。 */
    private String method;

    /** 方法参数，不能包含 token、authorization、cookie 等敏感信息。 */
    private Map<String, Object> params = new HashMap<>();
}
