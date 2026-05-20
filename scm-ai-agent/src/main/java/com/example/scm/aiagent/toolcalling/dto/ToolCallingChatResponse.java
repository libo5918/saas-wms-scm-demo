package com.example.scm.aiagent.toolcalling.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Tool Calling Chat 响应。
 */
@Getter
@Builder
public class ToolCallingChatResponse {

    /** 本次运行 ID。 */
    private String runId;

    /** 实际使用的 planner 模式。 */
    private String plannerMode;

    /** 本次规划来源，例如 requested、spring-ai、mock、mock-fallback。 */
    private String planningSource;

    /** 是否发生 mock fallback。 */
    private boolean fallbackUsed;

    /** 本次选中的工具名称。 */
    private String selectedTool;

    /** 最终执行时使用的工具参数。 */
    private Map<String, Object> toolArguments;

    /** 本次规划原因或备注。 */
    private String planningReason;

    /** 已压平的工具执行结果。 */
    private ToolCallingExecutionView execution;

    /** 最终返回给用户的答案。 */
    private String answer;

    /** 整体耗时。 */
    private long latencyMs;
}
