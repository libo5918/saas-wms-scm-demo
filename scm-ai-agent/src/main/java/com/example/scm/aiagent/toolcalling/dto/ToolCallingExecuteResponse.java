package com.example.scm.aiagent.toolcalling.dto;

import com.example.scm.aiagent.tool.dto.ToolResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Tool Calling 执行结果。
 */
@Getter
@Builder
public class ToolCallingExecuteResponse {

    /** Tool Calling 是否成功。 */
    private boolean success;

    /** 工具名称。 */
    private String toolName;

    /** 本次执行传入的 arguments。 */
    private Map<String, Object> arguments;

    /** 实际工具调用结果。 */
    private ToolResponse toolResponse;

    /** 整体执行耗时。 */
    private long latencyMs;
}
