package com.example.scm.aiagent.agent.prompt;

import com.example.scm.aiagent.agent.service.AgentIntentType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 一次 Agent 回答生成所需的结构化 prompt 上下文。
 */
@Getter
@Builder
public class AgentPromptContext {

    private String runId;
    private AgentIntentType intentType;
    private List<AgentPromptSection> sections;

    public int sectionCount() {
        return sections == null ? 0 : sections.size();
    }

    public int includedSectionCount() {
        return sections == null ? 0 : (int) sections.stream().filter(AgentPromptSection::isIncluded).count();
    }

    public int truncatedSectionCount() {
        return sections == null ? 0 : (int) sections.stream().filter(AgentPromptSection::isTruncated).count();
    }
}
