package com.example.scm.aiagent.toolcalling.orchestrator;

import lombok.Builder;
import lombok.Getter;

/**
 * Orchestration 状态接口使用的执行摘要。
 *
 * <p>只暴露成功标记、错误语义、耗时和展示摘要，不包含完整 rawData。</p>
 */
@Getter
@Builder
public class ToolOrchestrationExecutionSummary {

    /** Tool 执行是否成功。 */
    private boolean success;

    /** Tool 名称。 */
    private String toolName;

    /** 失败错误码。 */
    private String errorCode;

    /** 失败错误信息。 */
    private String errorMessage;

    /** 执行耗时，单位毫秒。 */
    private long latencyMs;

    /** 展示 schema 标题。 */
    private String displayTitle;

    /** 展示 schema 摘要。 */
    private String displaySummary;
}
