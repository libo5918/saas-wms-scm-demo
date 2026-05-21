package com.example.scm.aiagent.tool.persistence.po;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Tool 调用审计 MySQL 持久化对象。
 */
@Getter
@Setter
public class ToolInvocationAuditPO {

    private Long id;
    private Long tenantId;
    private Long userId;
    private String runId;
    private String toolName;
    private String adapterMode;
    private Boolean success;
    private String errorCode;
    private Long latencyMs;
    private Instant createdAt;
}
