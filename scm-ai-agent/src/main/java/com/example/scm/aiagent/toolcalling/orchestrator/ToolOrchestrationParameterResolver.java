package com.example.scm.aiagent.toolcalling.orchestrator;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orchestrator 后续步骤参数解析器。
 *
 * <p>当前只支持面试演示价值较高且低风险的固定组合：
 * {@code mdm.getMaterial -> inventory.getBalance}。解析器只使用白名单来源，
 * 重点从前置 Tool 的安全执行摘要中提取业务 ID，不读取完整 rawData、prompt、模型响应或敏感头。</p>
 */
@Component
public class ToolOrchestrationParameterResolver {

    private static final Pattern MATERIAL_CODE_PATTERN =
            Pattern.compile("(?i)(?:materialCode\\s*[:=]\\s*)?\\b(MAT[-_A-Z0-9]{2,})\\b");
    private static final Pattern MATERIAL_ID_PATTERN =
            Pattern.compile("(?i)(?:materialId|物料ID|物料id)\\s*[:=：]?\\s*(\\d+)");
    private static final Pattern WAREHOUSE_ID_PATTERN =
            Pattern.compile("(?i)(?:warehouseId|仓库ID|仓库id)\\s*[:=：]?\\s*(\\d+)");
    private static final Pattern LOCATION_ID_PATTERN =
            Pattern.compile("(?i)(?:locationId|库位ID|库位id)\\s*[:=：]?\\s*(\\d+)");
    private static final String INVENTORY_BALANCE_TOOL = "inventory.getBalance";

    /**
     * 为后续库存步骤解析安全参数。库存接口优先使用物料 ID、仓库 ID、库位 ID。
     */
    public ToolOrchestrationParameterResolveResult resolve(ToolOrchestrationRun run,
                                                           ToolOrchestrationStep previousStep,
                                                           ToolOrchestrationStep targetStep) {
        if (targetStep == null || !INVENTORY_BALANCE_TOOL.equals(targetStep.getToolName())) {
            return ToolOrchestrationParameterResolveResult.unresolved("unsupported follow-up tool");
        }
        String userMessage = run == null ? null : run.getUserMessage();
        Map<String, Object> previousArguments = previousStep == null ? Map.of() : previousStep.getArguments();
        Map<String, Object> safeFields = safeFields(previousStep);
        String outputSummary = previousStep == null ? null : previousStep.getOutputSummary();

        Long materialId = firstLong(
                longFromSafeFields(safeFields, "materialId"),
                longFromArguments(previousArguments, "materialId"),
                extractLong(userMessage, MATERIAL_ID_PATTERN),
                extractLong(outputSummary, MATERIAL_ID_PATTERN)
        );
        Long warehouseId = firstLong(
                longFromArguments(previousArguments, "warehouseId"),
                longFromSafeFields(safeFields, "warehouseId"),
                extractLong(userMessage, WAREHOUSE_ID_PATTERN),
                extractLong(outputSummary, WAREHOUSE_ID_PATTERN)
        );
        Long locationId = firstLong(
                longFromArguments(previousArguments, "locationId"),
                longFromSafeFields(safeFields, "locationId"),
                extractLong(userMessage, LOCATION_ID_PATTERN),
                extractLong(outputSummary, LOCATION_ID_PATTERN)
        );

        if (materialId == null) {
            return ToolOrchestrationParameterResolveResult.unresolved("materialId is required for inventory.getBalance");
        }
        if (warehouseId == null) {
            return ToolOrchestrationParameterResolveResult.unresolved("warehouseId is required for inventory.getBalance");
        }

        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("materialId", materialId);
        arguments.put("warehouseId", warehouseId);
        if (locationId != null) {
            arguments.put("locationId", locationId);
        }
        return ToolOrchestrationParameterResolveResult.resolved(arguments);
    }

    private Map<String, Object> safeFields(ToolOrchestrationStep previousStep) {
        if (previousStep == null || previousStep.getExecution() == null || previousStep.getExecution().getSafeFields() == null) {
            return Map.of();
        }
        return previousStep.getExecution().getSafeFields();
    }

    private Long longFromSafeFields(Map<String, Object> safeFields, String key) {
        return longValue(safeFields == null ? null : safeFields.get(key));
    }

    private Long longFromArguments(Map<String, Object> arguments, String key) {
        if (arguments == null || !arguments.containsKey(key)) {
            return null;
        }
        Object value = arguments.get(key);
        if (value == null || containsSensitiveKeyword(String.valueOf(value))) {
            return null;
        }
        return longValue(value);
    }

    private Long extractLong(String value, Pattern pattern) {
        if (!StringUtils.hasText(value) || containsSensitiveKeyword(value)) {
            return null;
        }
        Matcher matcher = pattern.matcher(value);
        if (!matcher.find()) {
            return null;
        }
        return longValue(matcher.group(1));
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text) || containsSensitiveKeyword(text)) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @SafeVarargs
    private final <T> T firstLong(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    private String extractMaterialCode(String value) {
        if (!StringUtils.hasText(value) || containsSensitiveKeyword(value)) {
            return null;
        }
        Matcher matcher = MATERIAL_CODE_PATTERN.matcher(value);
        return matcher.find() ? matcher.group(1).toUpperCase(Locale.ROOT) : null;
    }

    private boolean containsSensitiveKeyword(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains("rawdata")
                || normalized.contains("prompt")
                || normalized.contains("token")
                || normalized.contains("authorization")
                || normalized.contains("cookie")
                || normalized.contains("api-key")
                || normalized.contains("apikey")
                || normalized.contains("secret");
    }
}
