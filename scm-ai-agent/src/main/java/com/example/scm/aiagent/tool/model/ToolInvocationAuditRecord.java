package com.example.scm.aiagent.tool.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Tool 调用审计记录。
 *
 * <p>用于记录每次 Tool 调用的最小可追踪信息，为后续 Workflow、MCP 和 Orchestrator 提供观测基础。</p>
 */
@Getter
@Builder
public class ToolInvocationAuditRecord {

    /** 当前租户 ID。 */
    private Long tenantId;

    /** 当前用户 ID。 */
    private Long userId;

    /** Tool 所属 Agent runId。 */
    private String runId;

    /** Tool 名称。 */
    private String toolName;

    /** 当前调用使用的 adapter 模式，例如 mock、http。 */
    private String adapterMode;

    /** Tool 调用是否成功。 */
    private boolean success;

    /** Tool 失败时的错误码。 */
    private String errorCode;

    /** Tool 调用耗时，单位毫秒。 */
    private long latencyMs;

    /** 记录创建时间。 */
    private Instant createdAt;
}
