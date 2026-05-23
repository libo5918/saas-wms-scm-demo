package com.example.scm.aiagent.agent.prompt;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 安全约束 Provider，统一描述模型回答边界。
 */
@Component
public class SafetyPromptContextProvider implements AgentPromptContextProvider {

    @Override
    public List<AgentPromptSection> provide(AgentPromptBuildRequest request) {
        return List.of(AgentPromptSection.builder()
                .type(AgentPromptContextType.SAFETY_CONSTRAINTS)
                .source(AgentPromptContextSource.SYSTEM)
                .title("安全约束")
                .content("不要输出内部凭证、敏感请求头、完整原始业务对象、完整模型回包或调试链路。")
                .priority(5)
                .maxLength(300)
                .included(true)
                .build());
    }
}
