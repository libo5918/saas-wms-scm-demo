package com.example.scm.aiagent.toolcalling.orchestrator;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Orchestration 状态接口使用的执行摘要。
 *
 * <p>这里只暴露成功标记、错误语义、耗时、展示摘要和安全白名单字段，
 * 不包含完整 rawData、prompt、模型响应或敏感请求头。</p>
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

    /** display schema 标题。 */
    private String displayTitle;

    /** display schema 摘要。 */
    private String displaySummary;

    /** 从 Tool 返回结果中提取出的安全白名单字段，例如 materialId、warehouseId、locationId。 */
    private Map<String, Object> safeFields;
}
