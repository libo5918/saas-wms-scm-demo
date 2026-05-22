package com.example.scm.aiagent.tool.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Tool runtime 保护状态快照。
 *
 * <p>仅包含计数、错误类型和熔断状态，不包含请求参数、业务数据或敏感信息。</p>
 */
@Getter
@Builder
public class ToolRuntimeStatus {

    /** 工具名称。 */
    private String toolName;

    /** 总调用次数，包含被熔断拒绝的调用。 */
    private long totalCalls;

    /** 成功次数。 */
    private long successCount;

    /** 失败次数。 */
    private long failureCount;

    /** retry 重试次数。 */
    private long retryCount;

    /** 最近一次失败时间。 */
    private Instant lastFailureAt;

    /** 最近一次失败异常类型。 */
    private String lastErrorType;

    /** 熔断状态：CLOSED / OPEN / HALF_OPEN。 */
    private String circuitState;

    /** 最近一次进入 OPEN 的时间。 */
    private Instant openedAt;
}
