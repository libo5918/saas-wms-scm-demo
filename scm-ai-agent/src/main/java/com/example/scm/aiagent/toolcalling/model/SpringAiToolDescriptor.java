package com.example.scm.aiagent.toolcalling.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Spring AI Tool Calling 工具描述。
 */
@Getter
@Builder
public class SpringAiToolDescriptor {

    /** 工具名称。 */
    private String toolName;

    /** 工具说明。 */
    private String description;

    /** 工具业务域，例如 mdm、inventory、sales。 */
    private String domain;

    /** 工具类别，例如 query。 */
    private String category;

    /** 是否只读。 */
    private boolean readOnly;

    /** 安全路由标签，用于模型理解工具所属场景。 */
    private List<String> routeTags;

    /** 工具输入 schema。 */
    private SpringAiToolInputSchema inputSchema;
}
