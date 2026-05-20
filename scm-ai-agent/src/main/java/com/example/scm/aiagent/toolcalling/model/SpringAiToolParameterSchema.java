package com.example.scm.aiagent.toolcalling.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Spring AI Tool Calling 参数 schema。
 */
@Getter
@Builder
public class SpringAiToolParameterSchema {

    /** 参数类型，当前使用简化的 JSON schema 类型。 */
    private String type;

    /** 参数说明。 */
    private String description;

    /** 是否直接必填。 */
    private boolean required;
}
