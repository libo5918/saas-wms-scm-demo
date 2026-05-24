package com.example.scm.aiagent.multiagent.store;

import com.example.scm.aiagent.multiagent.model.MultiAgentRun;

import java.util.List;
import java.util.Optional;

/** Multi-Agent run 状态存储接口，Phase 10.1 仅提供 in-memory 实现。 */
public interface MultiAgentRunStore {

    void save(MultiAgentRun run);

    Optional<MultiAgentRun> get(String runId);

    List<MultiAgentRun> list(int limit);
}
