package com.example.scm.aiagent.multiagent.service;

import com.example.scm.aiagent.multiagent.model.MultiAgentReviewResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** ReviewerAgent 规则化审查服务，Phase 10.2 不调用模型。 */
@Service
public class MultiAgentReviewService {

    public MultiAgentReviewResult review(String answer, Map<String, Object> rag, Map<String, Object> tool) {
        List<String> issues = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        String normalized = answer == null ? "" : answer.toLowerCase();

        if (containsSensitive(normalized)) {
            issues.add("finalAnswer 包含敏感关键词");
            suggestions.add("移除 token、authorization、cookie、apikey、rawData、prompt 等内部信息");
        }
        int retrievedCount = numberValue(rag.get("retrievedCount"));
        if (retrievedCount <= 0 && answerContains(answer, "根据知识库")) {
            issues.add("RAG 未召回但回答声称使用知识库依据");
            suggestions.add("未召回知识库时应明确说明未找到知识片段，不能编造规则");
        }
        Map<?, ?> execution = tool.get("execution") instanceof Map<?, ?> map ? map : Map.of();
        Object success = execution.get("success");
        if (Boolean.FALSE.equals(success) && !answerContains(answer, String.valueOf(execution.get("errorMessage")))) {
            issues.add("Tool 失败时最终回答未保留真实失败原因");
            suggestions.add("补充 Tool errorMessage，避免用户误以为查询成功");
        }
        boolean passed = issues.isEmpty();
        return MultiAgentReviewResult.builder()
                .passed(passed)
                .issues(issues)
                .suggestions(suggestions)
                .safetyLevel(passed ? "PASS" : "WARN")
                .build();
    }

    public boolean containsSensitive(String value) {
        if (value == null) {
            return false;
        }
        String lower = value.toLowerCase();
        return lower.contains("authorization")
                || lower.contains("cookie")
                || lower.contains("api key")
                || lower.contains("apikey")
                || lower.contains("rawdata")
                || lower.contains("prompt")
                || lower.contains("token");
    }

    private boolean answerContains(String answer, String expected) {
        return answer != null && expected != null && !expected.isBlank() && answer.contains(expected);
    }

    private int numberValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return 0;
        }
    }
}
