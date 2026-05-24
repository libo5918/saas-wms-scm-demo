package com.example.scm.aiagent.mcp.dto;

import lombok.Builder;
import lombok.Getter;

/** MCP tools/call 的 content 文本片段。 */
@Getter
@Builder
public class McpServerContent {

    /** content 类型，Phase 9.1 仅使用 text。 */
    @Builder.Default
    private String type = "text";

    /** 脱敏后的文本内容。 */
    private String text;
}
