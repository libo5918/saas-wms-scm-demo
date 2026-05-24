package com.example.scm.aiagent.mcp.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** MCP tools/list 结果。 */
@Getter
@Builder
public class McpServerToolsListResult {

    /** 当前允许暴露给 MCP 的工具列表。 */
    private List<McpServerToolView> tools;
}
