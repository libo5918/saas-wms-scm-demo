package com.example.scm.aiagent.multiagent.model;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/** 一个 conversationId 下的安全摘要记忆集合。 */
@Getter
@Builder
public class MultiAgentMemory {

    private String conversationId;
    @Builder.Default
    private List<MultiAgentMemoryEntry> entries = new ArrayList<>();
}
