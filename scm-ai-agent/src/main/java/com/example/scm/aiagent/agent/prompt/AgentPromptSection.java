package com.example.scm.aiagent.agent.prompt;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 单个 prompt 上下文片段。
 *
 * <p>Provider 只负责产出结构化片段，Assembler 再统一裁剪、脱敏和排序。</p>
 */
@Getter
@Builder
public class AgentPromptSection {

    private AgentPromptContextType type;
    private AgentPromptContextSource source;
    private String title;
    private String content;
    private Map<String, Object> structuredData;
    private int priority;
    private int maxLength;
    private boolean included;
    private boolean truncated;
    private boolean sensitive;
}
