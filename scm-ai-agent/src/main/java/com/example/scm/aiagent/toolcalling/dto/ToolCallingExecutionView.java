package com.example.scm.aiagent.toolcalling.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Tool Calling Chat 中的执行结果视图。
 *
 * <p>这个对象专门给 `/api/v1/ai/tool-calling/chat` 返回使用，
 * 用来压平底层 Tool 执行结果，避免出现双层 `toolResponse` 嵌套。</p>
 */
@Getter
@Builder
public class ToolCallingExecutionView {

    /** 工具执行是否成功。 */
    private boolean success;

    /** 实际执行的工具名称。 */
    private String toolName;

    /** 失败时的错误码。 */
    private String errorCode;

    /** 失败时的错误信息。 */
    private String errorMessage;

    /** 工具返回的数据载荷。 */
    private Object data;

    /** 本次执行整体耗时，单位毫秒。 */
    private long latencyMs;
}
