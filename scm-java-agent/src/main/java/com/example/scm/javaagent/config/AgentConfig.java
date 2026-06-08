package com.example.scm.javaagent.config;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Java Agent 运行参数配置，来自 -javaagent:path=key=value,key2=value2。
 */
public record AgentConfig(
        boolean enabled,
        String includePackage,
        String excludePackage,
        boolean traceTool,
        boolean traceWorkflow,
        boolean traceMultiAgent,
        boolean asmPrint,
        long slowThresholdMs
) {

    public static AgentConfig parse(String args) {
        Map<String, String> values = parseArgs(args);
        return new AgentConfig(
                getBoolean(values, "enabled", true),
                values.getOrDefault("include", "com.example.scm.aiagent"),
                values.getOrDefault("exclude", "com.example.scm.javaagent"),
                getBoolean(values, "traceTool", true),
                getBoolean(values, "traceWorkflow", true),
                getBoolean(values, "traceMultiAgent", true),
                getBoolean(values, "asmPrint", false),
                getLong(values, "slowThresholdMs", 0L)
        );
    }

    public boolean shouldTraceClass(String className) {
        if (className == null) {
            return false;
        }
        if (!className.startsWith(includePackage)) {
            return false;
        }
        return excludePackage == null || excludePackage.isBlank() || !className.startsWith(excludePackage);
    }

    public boolean shouldTraceKnownAiAgentClass(String className) {
        if (!shouldTraceClass(className)) {
            return false;
        }
        if (traceTool && className.endsWith(".tool.service.ToolInvocationService")) {
            return true;
        }
        if (traceWorkflow && className.contains(".workflow.")) {
            return true;
        }
        if (traceMultiAgent && className.contains(".multiagent.")) {
            return true;
        }
        return className.endsWith("Controller") || className.endsWith("Service");
    }

    private static Map<String, String> parseArgs(String args) {
        Map<String, String> values = new LinkedHashMap<>();
        if (args == null || args.isBlank()) {
            return values;
        }
        for (String item : args.split(",")) {
            String trimmed = item.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int idx = trimmed.indexOf('=');
            if (idx <= 0) {
                values.put(trimmed, "true");
            } else {
                values.put(trimmed.substring(0, idx).trim(), trimmed.substring(idx + 1).trim());
            }
        }
        return values;
    }

    private static boolean getBoolean(Map<String, String> values, String key, boolean defaultValue) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true", "1", "yes", "y", "on" -> true;
            case "false", "0", "no", "n", "off" -> false;
            default -> defaultValue;
        };
    }

    private static long getLong(Map<String, String> values, String key, long defaultValue) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
