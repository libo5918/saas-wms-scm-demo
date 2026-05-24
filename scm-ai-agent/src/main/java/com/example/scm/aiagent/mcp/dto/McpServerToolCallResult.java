package com.example.scm.aiagent.mcp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/** MCP tools/call 成功响应，包含文本 content 和结构化安全结果。 */
@Getter
@Builder
public class McpServerToolCallResult {

    /** MCP content 列表，优先放 displaySummary。 */
    private List<McpServerContent> content;

    /** 结构化安全结果，不包含完整 rawData。 */
    private Map<String, Object> structuredContent;

    /** 是否为 Tool 语义错误。正常工具成功时为 false。 */
    @JsonProperty("isError")
    private boolean isError;
}
