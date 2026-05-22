package com.example.scm.aiagent.toolcalling.orchestrator;

import com.example.scm.aiagent.config.AiAgentProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * in-memory Orchestration run 存储。
 */
@Component
public class ToolOrchestrationRunStore {

    private final AiAgentProperties properties;
    private final Map<String, ToolOrchestrationRun> runs = new LinkedHashMap<>();

    public ToolOrchestrationRunStore(AiAgentProperties properties) {
        this.properties = properties;
    }

    /**
     * 保存或覆盖 run，并按 max-records 裁剪旧记录。
     */
    public synchronized void save(ToolOrchestrationRun run) {
        if (run == null || run.getRunId() == null) {
            return;
        }
        runs.put(run.getRunId(), run);
        trim();
    }

    /**
     * 根据 runId 查询运行记录。
     */
    public synchronized Optional<ToolOrchestrationRun> findByRunId(String runId) {
        return Optional.ofNullable(runs.get(runId));
    }

    /**
     * 查询最近运行记录，按插入顺序倒序返回。
     */
    public synchronized List<ToolOrchestrationRun> list(Integer limit) {
        int max = limit == null || limit <= 0 ? 20 : limit;
        List<ToolOrchestrationRun> values = new ArrayList<>(runs.values());
        List<ToolOrchestrationRun> result = new ArrayList<>();
        for (int i = values.size() - 1; i >= 0 && result.size() < max; i--) {
            result.add(values.get(i));
        }
        return result;
    }

    private void trim() {
        int maxRecords = Math.max(1, properties.getToolCalling().getOrchestrator().getMaxRecords());
        while (runs.size() > maxRecords) {
            String firstKey = runs.keySet().iterator().next();
            runs.remove(firstKey);
        }
    }
}
