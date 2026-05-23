package com.example.scm.aiagent.agent.prompt;

import java.util.List;

/**
 * Advisor 风格上下文提供者。
 *
 * <p>Provider 不直接调用模型，只把自身领域的安全上下文转换为 prompt section。</p>
 */
public interface AgentPromptContextProvider {

    List<AgentPromptSection> provide(AgentPromptBuildRequest request);
}
