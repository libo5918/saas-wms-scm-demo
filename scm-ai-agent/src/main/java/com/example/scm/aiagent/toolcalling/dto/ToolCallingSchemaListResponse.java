package com.example.scm.aiagent.toolcalling.dto;

import com.example.scm.aiagent.toolcalling.model.SpringAiToolDescriptor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Tool Calling schema 列表响应。
 */
@Getter
@Builder
public class ToolCallingSchemaListResponse {

    /** 当前租户 ID。 */
    private Long tenantId;

    /** 当前返回的工具数量。 */
    private int toolCount;

    /** Tool schema 列表。 */
    private List<SpringAiToolDescriptor> tools;
}
