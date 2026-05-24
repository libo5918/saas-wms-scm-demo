package com.example.scm.aiagent.mcp.dto;

import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayField;
import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayItem;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** MCP 风格 Tool 调用结果展示视图，不包含 rawData。 */
@Getter
@Builder
public class McpToolDisplayView {

    private String displayTitle;
    private String displaySummary;
    private List<ToolCallingDisplayField> displayFields;
    private List<ToolCallingDisplayItem> displayItems;
}
