package com.example.scm.aiagent.tool.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Agent Tool 定义信息。
 *
 * <p>用于向 Agent、MCP 或前端说明工具名称、业务用途、是否只读以及参数结构。</p>
 */
@Getter
@Builder
public class ToolDefinition {

    /** 工具唯一名称，例如 inventory.getBalance。 */
    private String name;

    /** 工具所属业务域，例如 inventory、mdm、sales。 */
    private String domain;

    /** 工具类别，用于后续 Orchestrator 按业务意图归类。 */
    private String category;

    /** 工具中文描述，方便模型选择和面试讲解。 */
    private String description;

    /** 是否只读；Phase 4 默认只实现只读工具。 */
    private boolean readOnly;

    /** 运行时适配模式说明，真实 adapterMode 仍以配置为准。 */
    private String adapterMode;

    /** 调用该工具需要具备的权限标签。 */
    private List<String> requiredPermissions;

    /** 调用该工具需要具备的角色标签。 */
    private List<String> requiredRoles;

    /** 是否必须在租户上下文内执行。 */
    private boolean tenantScoped;

    /** 是否必须在用户上下文内执行。 */
    private boolean userScoped;

    /** 路由标签，用于后续多轮 Tool Calling / Orchestrator 分流。 */
    private List<String> routeTags;

    /** 参数说明，key 为参数名，value 为参数含义。 */
    private Map<String, String> parameters;

    /** 必填参数列表。 */
    private List<String> requiredParameters;

    /**
     * 至少命中一项的参数组。
     *
     * <p>例如销售订单查询可以配置为 `[["orderId", "orderNo"]]`，表示 `orderId` 或 `orderNo` 至少传一个。</p>
     */
    private List<List<String>> oneOfRequiredGroups;
}
