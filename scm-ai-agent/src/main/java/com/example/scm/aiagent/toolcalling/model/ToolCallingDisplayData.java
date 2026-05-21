package com.example.scm.aiagent.toolcalling.model;

import lombok.Builder;

import java.util.List;

/**
 * Tool Calling 统一展示数据。
 *
 * <p>Phase 4.8 开始，成功的 Tool 执行结果会在 execution.data 中包装为该结构。
 * display 字段用于模型总结和前端展示，rawData 保留原始业务返回。</p>
 */
@Builder
public record ToolCallingDisplayData(
        /** 展示标题，例如“物料信息”。 */
        String displayTitle,

        /** 展示摘要，例如“已查询到物料 MAT-001”。 */
        String displaySummary,

        /** 展示字段列表。 */
        List<ToolCallingDisplayField> displayFields,

        /** 展示明细列表。 */
        List<ToolCallingDisplayItem> displayItems,

        /** Tool 原始返回数据。 */
        Object rawData
) {
}
