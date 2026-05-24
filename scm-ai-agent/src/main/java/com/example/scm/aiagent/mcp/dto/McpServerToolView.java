package com.example.scm.aiagent.mcp.dto;

import com.example.scm.aiagent.toolcalling.model.SpringAiToolInputSchema;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/** 标准 MCP tools/list 的 Tool 视图。 */
@Getter
@Builder
public class McpServerToolView {

    /** Tool 名称。 */
    private String name;

    /** Tool 描述。 */
    private String description;

    /** MCP input schema。 */
    private SpringAiToolInputSchema inputSchema;

    /** 安全 metadata，用于描述 domain、readOnly 等治理标签。 */
    private Map<String, Object> annotations;
}
