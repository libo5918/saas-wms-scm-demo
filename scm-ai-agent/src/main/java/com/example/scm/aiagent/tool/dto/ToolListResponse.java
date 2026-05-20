package com.example.scm.aiagent.tool.dto;

import com.example.scm.aiagent.tool.model.ToolDefinition;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Tool 列表响应。
 */
@Getter
@Builder
public class ToolListResponse {

    /** 当前租户 ID。 */
    private Long tenantId;

    /** 当前可用工具数量。 */
    private int toolCount;

    /** 可用工具定义列表。 */
    private List<ToolDefinition> tools;
}
