package com.example.scm.javaagent.config;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Java Agent 运行参数配置，来自 -javaagent:path=key=value,key2=value2。
 */
public record AgentConfig(
        boolean enabled,
        String includePackage,
        String excludePackage,
        Set<String> includeClasses,
        Set<String> excludeClasses,
        Set<String> includeMethods,
        Set<String> excludeMethods,
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
                getList(values, "includeClasses"),
                getList(values, "excludeClasses"),
                getList(values, "includeMethods"),
                getList(values, "excludeMethods"),
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
        if (excludePackage != null && !excludePackage.isBlank() && className.startsWith(excludePackage)) {
            return false;
        }
        String simpleName = simpleName(className);
        if (!includeClasses.isEmpty() && !includeClasses.contains(simpleName) && !includeClasses.contains(className)) {
            return false;
        }
        return !excludeClasses.contains(simpleName) && !excludeClasses.contains(className);
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

    public boolean shouldTraceMethod(String methodName) {
        if (methodName == null || methodName.isBlank()) {
            return false;
        }
        if (!includeMethods.isEmpty() && !includeMethods.contains(methodName)) {
            return false;
        }
        return !excludeMethods.contains(methodName);
    }

    private static Map<String, String> parseArgs(String args) {
        Map<String, String> values = new LinkedHashMap<>();
        if (args == null || args.isBlank()) {
            return values;
        }
        String previousKey = null;
        for (String item : args.split(",")) {
            String trimmed = item.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int idx = trimmed.indexOf('=');
            if (idx <= 0) {
                if (previousKey != null) {
                    values.put(previousKey, values.get(previousKey) + "," + trimmed);
                } else {
                    values.put(trimmed, "true");
                    previousKey = trimmed;
                }
            } else {
                previousKey = trimmed.substring(0, idx).trim();
                values.put(previousKey, trimmed.substring(idx + 1).trim());
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

    private static Set<String> getList(Map<String, String> values, String key) {
        String value = values.get(key);
        Set<String> result = new LinkedHashSet<>();
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        for (String item : value.split("[|,]")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return Set.copyOf(result);
    }

    private static String simpleName(String className) {
        int idx = className.lastIndexOf('.');
        return idx >= 0 ? className.substring(idx + 1) : className;
    }
}
