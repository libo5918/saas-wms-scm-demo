package com.example.scm.aiagent.tool.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Tool 候选集过滤结果。
 */
@Getter
@Builder
public class ToolCandidateFilterResult {

    /** 过滤后的候选工具。 */
    private List<ToolDefinition> candidates;

    /** 过滤前工具数量。 */
    private int beforeCount;

    /** 过滤后工具数量。 */
    private int afterCount;

    /** 是否因为过滤为空而回退到全量只读工具。 */
    private boolean fallbackUsed;

    /** 最终采用的业务域提示。 */
    private String resolvedDomain;

    /** 最终采用的类别提示。 */
    private String resolvedCategory;

    /** 最终采用的路由标签提示。 */
    private List<String> resolvedRouteTags;
}
