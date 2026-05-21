package com.example.scm.aiagent.toolcalling.model;

import lombok.Builder;

/**
 * Tool Calling 展示字段。
 *
 * <p>用于把不同 Tool 的原始字段转换成适合前端展示和模型总结的键值对。</p>
 */
@Builder
public record ToolCallingDisplayField(
        /** 原始字段或规范化字段名。 */
        String key,

        /** 面向用户展示的中文标签。 */
        String label,

        /** 面向用户展示的字段值。 */
        Object value
) {
}
