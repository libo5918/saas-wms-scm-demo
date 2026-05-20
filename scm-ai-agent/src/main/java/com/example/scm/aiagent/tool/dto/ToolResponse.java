package com.example.scm.aiagent.tool.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Tool 调用响应。
 */
@Getter
@Builder
public class ToolResponse {

    /** 工具是否执行成功。 */
    private boolean success;

    /** 工具名称。 */
    private String toolName;

    /** 本次调用所属 Agent runId。 */
    private String runId;

    /** 工具返回数据，当前为 mock/local 结构化数据。 */
    private Object data;

    /** 失败错误码。 */
    private String errorCode;

    /** 失败错误信息。 */
    private String errorMessage;

    /** 工具调用耗时，单位毫秒。 */
    private long latencyMs;
}
