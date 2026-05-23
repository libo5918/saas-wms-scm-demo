package com.example.scm.aiagent.agent.prompt;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用户问题 Provider。
 */
@Component
public class UserMessagePromptContextProvider implements AgentPromptContextProvider {

    @Override
    public List<AgentPromptSection> provide(AgentPromptBuildRequest request) {
        return List.of(AgentPromptSection.builder()
                .type(AgentPromptContextType.USER_MESSAGE)
                .source(AgentPromptContextSource.REQUEST)
                .title("用户问题")
                .content(request.getUserMessage() == null ? "" : request.getUserMessage())
                .priority(10)
                .maxLength(1000)
                .included(true)
                .build());
    }
}
