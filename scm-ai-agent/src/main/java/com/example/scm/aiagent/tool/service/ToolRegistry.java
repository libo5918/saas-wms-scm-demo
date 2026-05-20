package com.example.scm.aiagent.tool.service;

import com.example.scm.aiagent.tool.model.ToolDefinition;
import com.example.scm.aiagent.tool.spi.ToolExecutor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tool 注册表。
 *
 * <p>启动时收集所有 ToolExecutor，按 toolName 建立索引，供 ToolInvocationService 查询。</p>
 */
@Component
public class ToolRegistry {

    private final Map<String, ToolExecutor> executors;

    public ToolRegistry(List<ToolExecutor> executors) {
        this.executors = executors.stream()
                .collect(Collectors.toUnmodifiableMap(executor -> executor.definition().getName(), Function.identity()));
    }

    /**
     * 查询全部工具定义。
     */
    public List<ToolDefinition> listDefinitions() {
        return executors.values().stream()
                .map(ToolExecutor::definition)
                .sorted(Comparator.comparing(ToolDefinition::getName))
                .toList();
    }

    /**
     * 按工具名称查找执行器。
     */
    public Optional<ToolExecutor> findExecutor(String toolName) {
        return Optional.ofNullable(executors.get(toolName));
    }
}
