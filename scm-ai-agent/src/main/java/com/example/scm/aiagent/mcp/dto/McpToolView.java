package com.example.scm.aiagent.mcp.dto;

import com.example.scm.aiagent.toolcalling.model.SpringAiToolInputSchema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/** MCP 风格 Tool 定义安全视图，不暴露内部 adapter、URL 或敏感头。 */
@Getter
@Builder
public class McpToolView {

    private String name;
    private String description;
    private SpringAiToolInputSchema inputSchema;
    private Map<String, Object> displaySchema;
    private String domain;
    private String category;
    private List<String> routeTags;
    private boolean readOnly;
    private List<String> requiredPermissions;
}
