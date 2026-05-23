package com.example.scm.aiagent.agent.prompt;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 系统回答策略 Provider。
 */
@Component
public class SystemInstructionsPromptContextProvider implements AgentPromptContextProvider {

    @Override
    public List<AgentPromptSection> provide(AgentPromptBuildRequest request) {
        return List.of(AgentPromptSection.builder()
                .type(AgentPromptContextType.SYSTEM_INSTRUCTIONS)
                .source(AgentPromptContextSource.SYSTEM)
                .title("系统指令")
                .content("你需要输出自然、准确的中文回答。实时业务事实以工具结果为准，规则解释以知识库片段为准。")
                .structuredData(Map.of("intentType", request.getIntentType() == null ? "UNKNOWN" : request.getIntentType().name()))
                .priority(0)
                .maxLength(500)
                .included(true)
                .build());
    }
}
