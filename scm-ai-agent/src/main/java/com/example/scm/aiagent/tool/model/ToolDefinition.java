package com.example.scm.aiagent.tool.model;

import lombok.Builder;
import lombok.Getter;

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

    /** 工具中文描述，方便模型选择和面试讲解。 */
    private String description;

    /** 是否只读；Phase 4 默认只实现只读工具。 */
    private boolean readOnly;

    /** 参数说明，key 为参数名，value 为参数含义。 */
    private Map<String, String> parameters;
}
