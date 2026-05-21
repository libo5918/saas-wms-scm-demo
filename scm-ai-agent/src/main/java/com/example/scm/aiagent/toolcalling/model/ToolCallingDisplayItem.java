package com.example.scm.aiagent.toolcalling.model;

import lombok.Builder;

import java.util.List;

/**
 * Tool Calling 展示明细项。
 *
 * <p>用于订单明细、库存明细等列表型数据的稳定展示。</p>
 */
@Builder
public record ToolCallingDisplayItem(
        /** 明细项标题。 */
        String title,

        /** 明细项字段列表。 */
        List<ToolCallingDisplayField> fields,

        /** 明细项原始数据，便于后续追溯。 */
        Object rawData
) {
}
