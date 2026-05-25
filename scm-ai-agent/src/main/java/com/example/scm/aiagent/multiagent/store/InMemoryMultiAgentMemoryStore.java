package com.example.scm.aiagent.multiagent.store;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.multiagent.model.MultiAgentMemoryEntry;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 会话级 in-memory Memory store，只保留安全摘要并按配置裁剪。 */
@Service
public class InMemoryMultiAgentMemoryStore implements MultiAgentMemoryStore {

    private final AiAgentProperties properties;
    private final Map<String, MultiAgentMemoryEntry> entries = new LinkedHashMap<>();

    public InMemoryMultiAgentMemoryStore(AiAgentProperties properties) {
        this.properties = properties;
    }

    @Override
    public synchronized void append(MultiAgentMemoryEntry entry) {
        if (entry == null || !StringUtils.hasText(entry.getMemoryId())) {
            return;
        }
        entries.put(entry.getMemoryId(), entry);
        trim();
    }

    @Override
    public synchronized List<MultiAgentMemoryEntry> listByConversationId(Long tenantId, Long userId,
                                                                         String conversationId, int limit) {
        int effectiveLimit = Math.max(1, limit);
        return entries.values().stream()
                .filter(entry -> tenantId != null && tenantId.equals(entry.getTenantId()))
                .filter(entry -> userId != null && userId.equals(entry.getUserId()))
                .filter(entry -> conversationId.equals(entry.getConversationId()))
                .sorted(Comparator.comparing(MultiAgentMemoryEntry::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(effectiveLimit)
                .toList();
    }

    @Override
    public synchronized int clearByConversationId(Long tenantId, Long userId, String conversationId) {
        List<String> keys = entries.entrySet().stream()
                .filter(item -> tenantId != null && tenantId.equals(item.getValue().getTenantId()))
                .filter(item -> userId != null && userId.equals(item.getValue().getUserId()))
                .filter(item -> conversationId.equals(item.getValue().getConversationId()))
                .map(Map.Entry::getKey)
                .toList();
        keys.forEach(entries::remove);
        return keys.size();
    }

    private void trim() {
        int maxRecords = Math.max(1, properties.getMultiAgent().getMemoryMaxRecords());
        while (entries.size() > maxRecords) {
            String oldestKey = new ArrayList<>(entries.keySet()).get(0);
            entries.remove(oldestKey);
        }
    }
}
