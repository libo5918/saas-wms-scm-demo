package com.example.scm.aiagent.workflow.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Workflow 参数解析器，仅做白名单编码和 ID 提取。 */
@Component
public class AgentWorkflowParameterResolver {

    private static final Pattern MATERIAL_CODE_PATTERN = Pattern.compile("(?i)(MAT[-_A-Z0-9]+)");

    public String resolveMaterialCode(String message, Map<String, Object> parameters) {
        Object configured = parameters == null ? null : parameters.get("materialCode");
        if (configured != null && StringUtils.hasText(String.valueOf(configured))) {
            return String.valueOf(configured);
        }
        if (!StringUtils.hasText(message)) {
            return null;
        }
        Matcher matcher = MATERIAL_CODE_PATTERN.matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }

    public Object resolveWarehouseId(String message, Map<String, Object> parameters) {
        return firstPresent(parameters, List.of("warehouseId", "warehouse_id", "仓库ID", "仓库id", "仓库"),
                () -> resolveNumberAfterLabel(message, "仓库ID", "仓库id", "仓库"));
    }

    public Object resolveLocationId(String message, Map<String, Object> parameters) {
        return firstPresent(parameters, List.of("locationId", "location_id", "库位ID", "库位id", "库位"),
                () -> resolveNumberAfterLabel(message, "库位ID", "库位id", "库位"));
    }

    private Object firstPresent(Map<String, Object> parameters, List<String> keys, java.util.function.Supplier<Object> fallback) {
        if (parameters != null) {
            for (String key : keys) {
                Object value = parameters.get(key);
                if (value != null && StringUtils.hasText(String.valueOf(value))) {
                    return value;
                }
            }
        }
        return fallback.get();
    }

    private Object resolveNumberAfterLabel(String message, String... labels) {
        if (!StringUtils.hasText(message)) {
            return null;
        }
        for (String label : labels) {
            Pattern pattern = Pattern.compile(Pattern.quote(label) + "\\s*[:：#号为是-]?\\s*(\\d+)");
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                return Long.valueOf(matcher.group(1));
            }
        }
        return null;
    }
}
