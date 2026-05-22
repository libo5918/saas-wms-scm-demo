package com.example.scm.aiagent.tool.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Tool 候选集过滤请求。
 *
 * <p>该对象只承载安全的路由提示，不包含 prompt、token、请求头或业务 rawData。</p>
 */
@Getter
@Builder
public class ToolCandidateFilterRequest {

    /** 用户原始问题，仅用于轻量关键词推断，不写入日志。 */
    private String userMessage;

    /** 显式期望的业务域。 */
    private String requestedDomain;

    /** 显式期望的工具类别。 */
    private String requestedCategory;

    /** 显式期望匹配的路由标签。 */
    private List<String> routeTags;

    /** 是否只保留只读工具。 */
    private boolean readOnlyOnly;

    /** 最多保留的候选工具数；小于等于 0 表示不截断。 */
    private int maxCandidates;
}
