package com.example.scm.aiagent.multiagent.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/** PlannerAgent 输出的结构化安全计划，不包含 prompt 或模型原始响应。 */
@Getter
@Builder
public class MultiAgentPlan {

    private MultiAgentIntentType intentType;
    private boolean needRag;
    private boolean needTool;
    private boolean needWorkflow;
    private boolean needReview;
    private String requestedTool;
    private String requestedDomain;
    private List<String> routeTags;
    private String reason;

    public Map<String, Object> toSafeMap() {
        return Map.of(
                "intentType", intentType == null ? MultiAgentIntentType.GENERAL.name() : intentType.name(),
                "needRag", needRag,
                "needTool", needTool,
                "needWorkflow", needWorkflow,
                "needReview", needReview,
                "requestedTool", requestedTool == null ? "" : requestedTool,
                "requestedDomain", requestedDomain == null ? "" : requestedDomain,
                "routeTags", routeTags == null ? List.of() : routeTags,
                "reason", reason == null ? "" : reason
        );
    }
}
