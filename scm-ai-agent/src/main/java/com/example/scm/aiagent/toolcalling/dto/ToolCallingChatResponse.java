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

    /** 本次选中的工具名。 */
    private String selectedTool;

    /** 最终执行时使用的工具参数。 */
    private Map<String, Object> toolArguments;

    /** 工具调用结果。 */
    private ToolCallingExecuteResponse toolResponse;

    /** 最终返回给用户的答案。 */
    private String answer;

    /** 整体耗时。 */
    private long latencyMs;
}
