package com.example.scm.aiagent.multiagent.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/** conversationId 维度的 Memory 脱敏视图。 */
@Getter
@Builder
public class MultiAgentMemoryView {

    private String conversationId;
    private int count;
    private int clearedCount;
    @Builder.Default
    private List<MultiAgentMemoryEntryView> entries = new ArrayList<>();
}
