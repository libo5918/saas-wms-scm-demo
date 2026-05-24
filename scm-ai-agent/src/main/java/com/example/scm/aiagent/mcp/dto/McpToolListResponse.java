package com.example.scm.aiagent.mcp.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** MCP 风格 Tool 列表响应。 */
@Getter
@Builder
public class McpToolListResponse {

    private Long tenantId;
    private int toolCount;
    private List<McpToolView> tools;
}
