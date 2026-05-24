package com.example.scm.aiagent.multiagent;

import com.example.scm.aiagent.multiagent.model.MultiAgentReviewResult;
import com.example.scm.aiagent.multiagent.service.MultiAgentReviewService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiAgentReviewServiceTest {

    private final MultiAgentReviewService reviewService = new MultiAgentReviewService();

    @Test
    void shouldDetectSensitiveKeywords() {
        assertTrue(reviewService.containsSensitive("authorization: Bearer xxx"));
        assertTrue(reviewService.containsSensitive("rawData={secret}"));
    }

    @Test
    void shouldWarnWhenRagNotRetrievedButAnswerClaimsKnowledgeBase() {
        MultiAgentReviewResult result = reviewService.review(
                "根据知识库规则，库存可用数量这样计算",
                Map.of("retrievedCount", 0),
                Map.of());

        assertFalse(result.isPassed());
    }

    @Test
    void shouldPassSafeAnswer() {
        MultiAgentReviewResult result = reviewService.review(
                "KnowledgeAgent 未召回知识库片段，不编造知识库规则。",
                Map.of("retrievedCount", 0),
                Map.of());

        assertTrue(result.isPassed());
    }
    @Test
    void shouldWarnWhenToolSuccessButAnswerMissesDisplaySummary() {
        MultiAgentReviewResult result = reviewService.review(
                "ToolAgent 已完成查询。",
                Map.of("retrievedCount", 0),
                Map.of("execution", Map.of("success", true, "displaySummary", "已查询到物料 MAT-001")));

        assertFalse(result.isPassed());
    }

    @Test
    void shouldSuggestWhenRagRetrievedButAnswerMissesRuleSummary() {
        MultiAgentReviewResult result = reviewService.review(
                "ToolAgent 已完成查询，已查询到物料 MAT-001。",
                Map.of("retrievedCount", 1),
                Map.of("execution", Map.of("success", true, "displaySummary", "已查询到物料 MAT-001")));

        assertTrue(result.isPassed());
        assertFalse(result.getSuggestions().isEmpty());
    }
}
