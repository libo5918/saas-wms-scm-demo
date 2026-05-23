package com.example.scm.aiagent.workflow.service;

import com.example.scm.aiagent.workflow.model.AgentWorkflowRun;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Workflow run in-memory 存储。 */
@Component
public class AgentWorkflowRunStore {

    private static final int MAX_RECORDS = 100;

    private final ConcurrentHashMap<String, AgentWorkflowRun> runs = new ConcurrentHashMap<>();
    private final LinkedList<String> order = new LinkedList<>();

    public synchronized void save(AgentWorkflowRun run) {
        runs.put(run.getRunId(), run);
        order.remove(run.getRunId());
        order.addFirst(run.getRunId());
        while (order.size() > MAX_RECORDS) {
            runs.remove(order.removeLast());
        }
    }

    public Optional<AgentWorkflowRun> get(String runId) {
        return Optional.ofNullable(runs.get(runId));
    }

    public List<AgentWorkflowRun> list(int limit) {
        int boundedLimit = limit <= 0 ? 20 : Math.min(limit, MAX_RECORDS);
        return order.stream()
                .limit(boundedLimit)
                .map(runs::get)
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(AgentWorkflowRun::getStartedAt).reversed())
                .toList();
    }
}
