package com.example.scm.aiagent.toolcalling.model;

import lombok.Builder;
import lombok.Getter;

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

    /** 是否只读。 */
    private boolean readOnly;

    /** 工具输入 schema。 */
    private SpringAiToolInputSchema inputSchema;
}
