package com.example.scm.aiagent.multiagent.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/** ReviewerAgent 规则化审查结果，聚焦事实一致性与敏感信息拦截。 */
@Getter
@Builder
public class MultiAgentReviewResult {

    private boolean passed;
    private List<String> issues;
    private List<String> suggestions;
    private String safetyLevel;

    public Map<String, Object> toSafeMap() {
        return Map.of(
                "passed", passed,
                "issues", issues == null ? List.of() : issues,
                "suggestions", suggestions == null ? List.of() : suggestions,
                "safetyLevel", safetyLevel == null ? "UNKNOWN" : safetyLevel
        );
    }
}
