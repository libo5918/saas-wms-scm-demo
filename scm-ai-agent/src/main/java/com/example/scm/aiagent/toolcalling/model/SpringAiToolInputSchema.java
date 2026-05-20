package com.example.scm.aiagent.toolcalling.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Spring AI Tool Calling 输入 schema。
 */
@Getter
@Builder
public class SpringAiToolInputSchema {

    /** schema 类型，当前固定为 object。 */
    private String type;

    /** 参数属性定义。 */
    private Map<String, SpringAiToolParameterSchema> properties;

    /** 直接必填参数列表。 */
    private List<String> required;

    /**
     * 二选一或多选一必填组。
     *
     * <p>例如 [[orderId, orderNo]] 表示 orderId 和 orderNo 至少传一个。</p>
     */
    private List<List<String>> oneOfRequiredGroups;
}
