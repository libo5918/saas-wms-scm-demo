package com.example.scm.aiagent.multiagent.store;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.multiagent.model.MultiAgentRun;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 轻量 in-memory run store，用于面试演示和单元测试，不保存敏感原文。 */
@Service
public class InMemoryMultiAgentRunStore implements MultiAgentRunStore {

    private final AiAgentProperties properties;
    private final Map<String, MultiAgentRun> runs = new LinkedHashMap<>();

    public InMemoryMultiAgentRunStore(AiAgentProperties properties) {
        this.properties = properties;
    }

    @Override
    public synchronized void save(MultiAgentRun run) {
        if (run == null || !StringUtils.hasText(run.getRunId())) {
            return;
        }
        runs.put(run.getRunId(), run);
        trim();
    }

    @Override
    public synchronized Optional<MultiAgentRun> get(String runId) {
        return Optional.ofNullable(runs.get(runId));
    }

    @Override
    public synchronized List<MultiAgentRun> list(int limit) {
        int effectiveLimit = Math.max(1, limit);
        return runs.values().stream()
                .sorted(Comparator.comparing(MultiAgentRun::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(effectiveLimit)
                .toList();
    }

    private void trim() {
        int maxRecords = Math.max(1, properties.getMultiAgent().getMaxRecords());
        while (runs.size() > maxRecords) {
            String oldestKey = new ArrayList<>(runs.keySet()).get(0);
            runs.remove(oldestKey);
        }
    }
}
