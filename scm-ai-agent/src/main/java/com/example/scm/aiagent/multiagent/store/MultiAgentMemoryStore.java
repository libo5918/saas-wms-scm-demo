package com.example.scm.aiagent.multiagent.store;

import com.example.scm.aiagent.multiagent.model.MultiAgentMemoryEntry;

import java.util.List;

/** Multi-Agent Memory 存储接口，后续可扩展 MySQL/Redis/Vector Memory。 */
public interface MultiAgentMemoryStore {

    void append(MultiAgentMemoryEntry entry);

    List<MultiAgentMemoryEntry> listByConversationId(Long tenantId, Long userId, String conversationId, int limit);

    int clearByConversationId(Long tenantId, Long userId, String conversationId);
}
