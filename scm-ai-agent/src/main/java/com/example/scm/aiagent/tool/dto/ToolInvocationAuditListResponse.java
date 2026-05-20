package com.example.scm.aiagent.tool.dto;

import com.example.scm.aiagent.tool.model.ToolInvocationAuditRecord;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Tool 调用审计列表响应。
 */
@Getter
@Builder
public class ToolInvocationAuditListResponse {

    /** 当前租户 ID。 */
    private Long tenantId;

    /** 返回记录数。 */
    private int count;

    /** 审计记录列表。 */
    private List<ToolInvocationAuditRecord> records;
}
