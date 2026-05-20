package com.example.scm.aiagent.toolcalling.model;

import lombok.Builder;

import java.util.Map;

/**
 * Tool Calling 规划结果。
 */
@Builder
public record ToolCallingPlan(
        String plannerMode,
        String selectedTool,
        Map<String, Object> toolArguments,
        String reason
) {
}
