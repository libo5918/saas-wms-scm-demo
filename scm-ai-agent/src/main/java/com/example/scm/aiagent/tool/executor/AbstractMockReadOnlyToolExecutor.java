package com.example.scm.aiagent.tool.executor;

import com.example.scm.aiagent.tool.model.ToolDefinition;
import com.example.scm.aiagent.tool.spi.ToolExecutor;

import java.util.List;
import java.util.Map;

/**
 * mock 只读工具基类。
 *
 * <p>Phase 4 先用本地 mock adapter 固定工具协议，后续可替换为 REST / Feign / WebClient 调用真实业务服务。</p>
 */
public abstract class AbstractMockReadOnlyToolExecutor implements ToolExecutor {

    private final ToolDefinition definition;

    protected AbstractMockReadOnlyToolExecutor(String name, String domain, String description,
                                               Map<String, String> parameters) {
        this(name, domain, description, parameters, List.of(), List.of());
    }

    protected AbstractMockReadOnlyToolExecutor(String name, String domain, String description,
                                               Map<String, String> parameters,
                                               List<String> requiredParameters,
                                               List<List<String>> oneOfRequiredGroups) {
        this.definition = ToolDefinition.builder()
                .name(name)
                .domain(domain)
                .description(description)
                .readOnly(true)
                .parameters(parameters)
                .requiredParameters(requiredParameters)
                .oneOfRequiredGroups(oneOfRequiredGroups)
                .build();
    }

    @Override
    public ToolDefinition definition() {
        return definition;
    }

    protected String stringParam(Map<String, Object> parameters, String key, String defaultValue) {
        Object value = parameters == null ? null : parameters.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    protected Long longParam(Map<String, Object> parameters, String key, Long defaultValue) {
        Object value = parameters == null ? null : parameters.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
