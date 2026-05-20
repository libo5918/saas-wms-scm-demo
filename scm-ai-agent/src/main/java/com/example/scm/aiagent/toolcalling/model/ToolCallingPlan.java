package com.example.scm.aiagent.toolcalling.model;

import lombok.Builder;

import java.util.Map;

/**
 * Tool Calling 规划结果。
 */
@Builder
public record ToolCallingPlan(
        /** 实际使用的 planner 模式，例如 mock、spring-ai。 */
        String plannerMode,

        /** 本次规划来源，例如 requested、spring-ai、mock、mock-fallback。 */
        String planningSource,

        /** 是否发生了回退。 */
        boolean fallbackUsed,

        /** 本次选中的工具名称。 */
        String selectedTool,

        /** 最终执行时使用的工具参数。 */
        Map<String, Object> toolArguments,

        /** 规划原因或备注。 */
        String reason
) {
}
