package com.example.scm.aiagent.multiagent.service;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.multiagent.dto.MultiAgentChatRequest;
import com.example.scm.aiagent.multiagent.dto.MultiAgentMemoryEntryView;
import com.example.scm.aiagent.multiagent.dto.MultiAgentMemoryView;
import com.example.scm.aiagent.multiagent.model.MultiAgentMemoryEntry;
import com.example.scm.aiagent.multiagent.model.MultiAgentMemoryType;
import com.example.scm.aiagent.multiagent.model.MultiAgentPlan;
import com.example.scm.aiagent.multiagent.model.MultiAgentReviewResult;
import com.example.scm.aiagent.multiagent.model.MultiAgentRun;
import com.example.scm.aiagent.multiagent.store.MultiAgentMemoryStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Multi-Agent 会话级安全摘要记忆服务，不保存完整 prompt、rawData 或敏感凭证。 */
@Service
public class MultiAgentMemoryService {

    private static final int SUMMARY_MAX_LENGTH = 500;

    private final MultiAgentMemoryStore memoryStore;
    private final AiAgentProperties properties;

    public MultiAgentMemoryService(MultiAgentMemoryStore memoryStore, AiAgentProperties properties) {
        this.memoryStore = memoryStore;
        this.properties = properties;
    }

    public boolean isActive(MultiAgentChatRequest request) {
        return properties.getMultiAgent().isMemoryEnabled()
                && Boolean.TRUE.equals(request.getMemoryEnabled())
                && StringUtils.hasText(request.getConversationId());
    }

    public List<MultiAgentMemoryEntry> read(MultiAgentChatRequest request, AgentRequestContext context) {
        if (!isActive(request)) {
            return List.of();
        }
        return memoryStore.listByConversationId(context.tenantId(), context.userId(), request.getConversationId(),
                properties.getMultiAgent().getMemoryReadLimit());
    }

    public int writeRunMemory(MultiAgentRun run, MultiAgentPlan plan, Map<String, Object> rag,
                              Map<String, Object> tool, MultiAgentReviewResult review) {
        if (!run.isMemoryEnabled() || !StringUtils.hasText(run.getConversationId())) {
            return 0;
        }
        List<MultiAgentMemoryEntry> entries = new ArrayList<>();
        entries.add(entry(run, MultiAgentMemoryType.USER_MESSAGE_SUMMARY, run.getUserMessage(),
                Map.of("runId", run.getRunId())));
        entries.add(entry(run, MultiAgentMemoryType.PLAN_SUMMARY, plan == null ? "" : plan.getReason(),
                plan == null ? Map.of() : plan.toSafeMap()));
        if (rag != null && !rag.isEmpty()) {
            entries.add(entry(run, MultiAgentMemoryType.RAG_SUMMARY, "retrievedCount=" + rag.getOrDefault("retrievedCount", 0), rag));
        }
        if (tool != null && !tool.isEmpty()) {
            entries.add(entry(run, MultiAgentMemoryType.TOOL_SUMMARY, toolSummary(tool), tool));
        }
        if (review != null) {
            entries.add(entry(run, MultiAgentMemoryType.REVIEW_SUMMARY,
                    review.isPassed() ? "review passed" : "review issues=" + review.getIssues(), review.toSafeMap()));
        }
        entries.add(entry(run, MultiAgentMemoryType.FINAL_ANSWER_SUMMARY, run.getFinalAnswer(),
                Map.of("status", String.valueOf(run.getStatus()))));
        entries.forEach(memoryStore::append);
        return entries.size();
    }

    public MultiAgentMemoryView getMemory(String conversationId, AgentRequestContext context) {
        List<MultiAgentMemoryEntry> entries = memoryStore.listByConversationId(context.tenantId(), context.userId(),
                conversationId, properties.getMultiAgent().getMemoryMaxRecords());
        return toView(conversationId, entries, 0);
    }

    public MultiAgentMemoryView clearMemory(String conversationId, AgentRequestContext context) {
        int cleared = memoryStore.clearByConversationId(context.tenantId(), context.userId(), conversationId);
        return toView(conversationId, List.of(), cleared);
    }

    public Map<String, Object> toSummaryMap(String conversationId, List<MultiAgentMemoryEntry> entries) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("conversationId", conversationId);
        summary.put("count", entries == null ? 0 : entries.size());
        summary.put("entries", entries == null ? List.of() : entries.stream().map(this::toMap).toList());
        return summary;
    }

    private MultiAgentMemoryEntry entry(MultiAgentRun run, MultiAgentMemoryType type,
                                        String contentSummary, Map<?, ?> structuredData) {
        return MultiAgentMemoryEntry.builder()
                .memoryId("mem-" + UUID.randomUUID())
                .conversationId(run.getConversationId())
                .runId(run.getRunId())
                .tenantId(run.getTenantId())
                .userId(run.getUserId())
                .type(type)
                .contentSummary(safeText(contentSummary, SUMMARY_MAX_LENGTH))
                .structuredData(sanitizeMap(structuredData))
                .createdAt(Instant.now())
                .build();
    }

    private MultiAgentMemoryView toView(String conversationId, List<MultiAgentMemoryEntry> entries, int cleared) {
        List<MultiAgentMemoryEntryView> views = entries.stream().map(this::toEntryView).toList();
        return MultiAgentMemoryView.builder()
                .conversationId(conversationId)
                .count(views.size())
                .clearedCount(cleared)
                .entries(views)
                .build();
    }

    private MultiAgentMemoryEntryView toEntryView(MultiAgentMemoryEntry entry) {
        return MultiAgentMemoryEntryView.builder()
                .memoryId(entry.getMemoryId())
                .conversationId(entry.getConversationId())
                .runId(entry.getRunId())
                .type(entry.getType())
                .contentSummary(entry.getContentSummary())
                .structuredData(sanitizeMap(entry.getStructuredData()))
                .createdAt(entry.getCreatedAt())
                .build();
    }

    private Map<String, Object> toMap(MultiAgentMemoryEntry entry) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("memoryId", entry.getMemoryId());
        map.put("runId", entry.getRunId());
        map.put("type", entry.getType());
        map.put("contentSummary", entry.getContentSummary());
        map.put("structuredData", sanitizeMap(entry.getStructuredData()));
        map.put("createdAt", entry.getCreatedAt());
        return map;
    }

    private String toolSummary(Map<String, Object> tool) {
        Object executionObject = tool.get("execution");
        if (executionObject instanceof Map<?, ?> execution) {
            return "tool=" + tool.getOrDefault("selectedTool", "")
                    + ", success=" + execution.get("success")
                    + ", displaySummary=" + execution.get("displaySummary")
                    + ", errorMessage=" + execution.get("errorMessage");
        }
        return String.valueOf(tool.getOrDefault("status", "SUCCESS"));
    }

    private Map<String, Object> sanitizeMap(Map<?, ?> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            String keyText = String.valueOf(key);
            if (!isSensitiveKey(keyText)) {
                sanitized.put(keyText, sanitizeValue(value, 0));
            }
        });
        return sanitized;
    }

    @SuppressWarnings("unchecked")
    private Object sanitizeValue(Object value, int depth) {
        if (value == null) {
            return null;
        }
        if (depth >= 2) {
            return safeText(String.valueOf(value), 200);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new HashMap<>();
            map.forEach((key, item) -> {
                String keyText = String.valueOf(key);
                if (!isSensitiveKey(keyText)) {
                    sanitized.put(keyText, sanitizeValue(item, depth + 1));
                }
            });
            return sanitized;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .limit(5)
                    .map(item -> sanitizeValue(item, depth + 1))
                    .toList();
        }
        return safeText(String.valueOf(value), 300);
    }

    private boolean isSensitiveKey(String key) {
        String normalized = key == null ? "" : key.toLowerCase();
        return normalized.contains("authorization")
                || normalized.contains("cookie")
                || normalized.contains("token")
                || normalized.contains("api_key")
                || normalized.contains("apikey")
                || normalized.contains("api key")
                || normalized.contains("rawdata")
                || normalized.contains("prompt")
                || normalized.contains("modelresponse")
                || normalized.contains("model_response");
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
                .replaceAll("(?i)model\\s*response", "[REDACTED]")
                .replaceAll("(?i)prompt", "[REDACTED]");
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
    }
}
