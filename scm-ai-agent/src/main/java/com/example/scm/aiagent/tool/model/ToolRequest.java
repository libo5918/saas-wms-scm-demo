package com.example.scm.aiagent.tool.model;

import com.example.scm.aiagent.model.AgentRequestContext;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Tool 执行请求。
 *
 * <p>由 ToolInvocationService 统一构建，确保工具执行时一定携带租户、用户和 runId。</p>
 */
@Getter
@Builder
public class ToolRequest {

    /** 当前工具调用所属 Agent runId。 */
    private String runId;

    /** 工具名称。 */
    private String toolName;

    /** 网关透传解析后的租户和用户上下文。 */
    private AgentRequestContext context;

    /** 工具参数。 */
    private Map<String, Object> parameters;
}
