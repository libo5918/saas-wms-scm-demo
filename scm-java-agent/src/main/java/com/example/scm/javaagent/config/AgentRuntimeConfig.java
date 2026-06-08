package com.example.scm.javaagent.config;

/**
 * Advice 运行时配置持有器，避免业务模块依赖 Agent API。
 */
public final class AgentRuntimeConfig {

    private static volatile AgentConfig config = AgentConfig.parse("");

    private AgentRuntimeConfig() {
    }

    public static AgentConfig get() {
        return config;
    }

    public static void set(AgentConfig agentConfig) {
        if (agentConfig != null) {
            config = agentConfig;
        }
    }
}
