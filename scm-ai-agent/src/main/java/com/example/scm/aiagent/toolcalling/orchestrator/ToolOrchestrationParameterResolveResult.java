package com.example.scm.aiagent.toolcalling.orchestrator;

import java.util.Map;

/**
 * Orchestrator 后续步骤参数解析结果。
 *
 * <p>Phase 4.15 只允许从用户原始问题、前置步骤白名单参数和前置步骤安全摘要中解析参数，
 * 不读取 rawData、prompt、模型响应、token 或内部请求头。</p>
 */
public record ToolOrchestrationParameterResolveResult(
        boolean resolved,
        Map<String, Object> arguments,
        String error
) {
    public static ToolOrchestrationParameterResolveResult resolved(Map<String, Object> arguments) {
        return new ToolOrchestrationParameterResolveResult(true, arguments == null ? Map.of() : Map.copyOf(arguments), null);
    }

    public static ToolOrchestrationParameterResolveResult unresolved(String error) {
        return new ToolOrchestrationParameterResolveResult(false, Map.of(), error);
    }
}
