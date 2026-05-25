package com.example.scm.aiagent.multiagent.service;

import com.example.scm.aiagent.multiagent.model.MultiAgentActionType;
import com.example.scm.aiagent.multiagent.model.MultiAgentAgentMetrics;
import com.example.scm.aiagent.multiagent.model.MultiAgentRole;
import com.example.scm.aiagent.multiagent.model.MultiAgentRun;
import com.example.scm.aiagent.multiagent.model.MultiAgentRunMetrics;
import com.example.scm.aiagent.multiagent.model.MultiAgentRunStatus;
import com.example.scm.aiagent.multiagent.model.MultiAgentStep;
import com.example.scm.aiagent.multiagent.model.MultiAgentStepStatus;
import com.example.scm.aiagent.multiagent.model.MultiAgentTraceSummary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Multi-Agent 可观测性服务，基于脱敏 run/step 摘要生成 metrics 和 trace summary。 */
@Service
public class MultiAgentObservabilityService {

    public MultiAgentRunMetrics buildMetrics(MultiAgentRun run) {
        List<MultiAgentAgentMetrics> agentMetrics = run.getAgents().stream()
                .map(agent -> buildAgentMetrics(run, agent.getAgentName(), agent.getRole()))
                .toList();
        boolean ragCalled = "SUCCESS".equals(String.valueOf(run.getRag().get("status")));
        boolean toolCalled = run.getToolCallCount() > 0;
        boolean reviewEnabled = !run.getReview().isEmpty()
                && !"SKIPPED".equals(String.valueOf(run.getReview().getOrDefault("safetyLevel", "")));
        boolean reviewPassed = Boolean.TRUE.equals(run.getReview().get("passed"));
        return MultiAgentRunMetrics.builder()
                .totalLatencyMs(run.getLatencyMs())
                .stepCount(run.getSteps().size())
                .agentCount(run.getAgents().size())
                .ragCalled(ragCalled)
                .ragRetrievedCount(numberValue(run.getRag().get("retrievedCount")))
                .toolCalled(toolCalled)
                .toolCallCount(run.getToolCallCount())
                .reviewEnabled(reviewEnabled)
                .reviewPassed(reviewPassed)
                .repairEnabled(run.isRepairEnabled())
                .repairAttempted(run.isRepairAttempted())
                .repairCount(run.getRepairCount())
                .memoryEnabled(run.isMemoryEnabled())
                .memoryReadCount(run.getMemoryReadCount())
                .memoryWriteCount(run.getMemoryWriteCount())
                .terminated(run.getStatus() == MultiAgentRunStatus.TERMINATED || StringUtils.hasText(run.getTerminatedReason()))
                .terminatedReason(safeText(run.getTerminatedReason(), 300))
                .agentMetrics(agentMetrics)
                .build();
    }

    public MultiAgentTraceSummary buildTraceSummary(MultiAgentRun run) {
        String planner = "PlannerAgent intent=" + run.getIntentType();
        String knowledge = run.getSteps().stream()
                .filter(step -> step.getActionType() == MultiAgentActionType.RAG_RETRIEVE)
                .findFirst()
                .map(step -> "KnowledgeAgent " + step.getStatus() + ", retrievedCount=" + run.getRag().getOrDefault("retrievedCount", 0))
                .orElse("KnowledgeAgent not executed");
        String tool = run.getSteps().stream()
                .filter(step -> step.getActionType() == MultiAgentActionType.TOOL_CALL)
                .findFirst()
                .map(step -> "ToolAgent " + step.getStatus() + ", selectedTool=" + safeText(String.valueOf(run.getTool().getOrDefault("selectedTool", "")), 80))
                .orElse("ToolAgent not executed");
        String review = "ReviewerAgent passed=" + run.getReview().getOrDefault("passed", "SKIPPED");
        String repair = "repairAttempted=" + run.isRepairAttempted() + ", repairCount=" + run.getRepairCount();
        String memory = "memoryEnabled=" + run.isMemoryEnabled() + ", read=" + run.getMemoryReadCount()
                + ", write=" + run.getMemoryWriteCount();
        String termination = StringUtils.hasText(run.getTerminatedReason())
                ? "terminatedReason=" + safeText(run.getTerminatedReason(), 160)
                : "terminatedReason=none";
        return MultiAgentTraceSummary.builder()
                .summary(safeText(String.join("; ", planner, knowledge, tool, review, repair, memory, termination), 1000))
                .build();
    }

    private MultiAgentAgentMetrics buildAgentMetrics(MultiAgentRun run, String agentName, MultiAgentRole role) {
        List<MultiAgentStep> steps = run.getSteps().stream()
                .filter(step -> agentName.equals(step.getAgentName()))
                .toList();
        return MultiAgentAgentMetrics.builder()
                .agentName(agentName)
                .role(role)
                .actionCount(steps.size())
                .successCount(count(steps, MultiAgentStepStatus.SUCCESS))
                .failedCount(count(steps, MultiAgentStepStatus.FAILED))
                .skippedCount(count(steps, MultiAgentStepStatus.SKIPPED))
                .latencyMs(steps.stream().mapToLong(MultiAgentStep::getLatencyMs).sum())
                .build();
    }

    private int count(List<MultiAgentStep> steps, MultiAgentStepStatus status) {
        return (int) steps.stream().filter(step -> step.getStatus() == status).count();
    }

    private long numberValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String safeText(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return value == null ? "" : value;
        }
        String sanitized = value
                .replaceAll("(?i)authorization\\s*[:=]\\s*\\S+", "[REDACTED]")
                .replaceAll("(?i)cookie\\s*[:=]\\s*\\S+", "[REDACTED]")
                .replaceAll("(?i)token\\s*[:=]\\s*\\S+", "[REDACTED]")
                .replaceAll("(?i)api\\s*key\\s*[:=]\\s*\\S+", "[REDACTED]")
                .replaceAll("(?i)rawData", "[REDACTED]")
                .replaceAll("(?i)prompt", "[REDACTED]")
                .replaceAll("(?i)model\\s*response", "[REDACTED]");
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
    }
}
