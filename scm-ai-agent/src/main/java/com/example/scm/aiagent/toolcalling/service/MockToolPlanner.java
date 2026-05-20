package com.example.scm.aiagent.toolcalling.service;

import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatRequest;
import com.example.scm.aiagent.toolcalling.model.ToolCallingPlan;
import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 基于规则的 mock planner。
 *
 * <p>用于测试和真实模型不可用时的兜底规划，不依赖外部模型服务。</p>
 */
@Component
public class MockToolPlanner {

    /**
     * 兼容旧调用方式的默认入口。
     */
    public ToolCallingPlan plan(ToolCallingChatRequest request) {
        if (StringUtils.hasText(request.getRequestedTool())) {
            return planRequestedTool("mock", request.getRequestedTool(), request.getToolArguments());
        }
        return planByRules("mock", request);
    }

    /**
     * 针对显式指定工具的规划结果。
     */
    public ToolCallingPlan planRequestedTool(String plannerMode, String requestedTool, Map<String, Object> toolArguments) {
        return ToolCallingPlan.builder()
                .plannerMode(plannerMode)
                .planningSource("requested")
                .fallbackUsed(false)
                .selectedTool(requestedTool)
                .toolArguments(applyDefaults(requestedTool, toolArguments))
                .reason("requested_tool")
                .build();
    }

    /**
     * 按关键字规则进行工具规划。
     */
    public ToolCallingPlan planByRules(String plannerMode, ToolCallingChatRequest request) {
        String message = request.getMessage() == null ? "" : request.getMessage().toLowerCase(Locale.ROOT);
        String selectedTool;
        if (containsAny(message, "库存", "balance", "available")) {
            selectedTool = "inventory.getBalance";
        } else if (containsAny(message, "销售订单", "sales order")) {
            selectedTool = "sales.getOrder";
        } else if (containsAny(message, "采购订单", "purchase order")) {
            selectedTool = "purchase.getOrder";
        } else if (containsAny(message, "仓库", "warehouse")) {
            selectedTool = "mdm.getWarehouse";
        } else if (containsAny(message, "物料", "material")) {
            selectedTool = "mdm.getMaterial";
        } else {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(),
                    "Unable to determine tool from message, please provide requestedTool");
        }

        return ToolCallingPlan.builder()
                .plannerMode(plannerMode)
                .planningSource("mock")
                .fallbackUsed(false)
                .selectedTool(selectedTool)
                .toolArguments(applyDefaults(selectedTool, request.getToolArguments()))
                .reason("mock_rule")
                .build();
    }

    /**
     * 当模型规划失败时，按规则兜底，并在结果里标记 fallback。
     */
    public ToolCallingPlan planFallback(String plannerMode, ToolCallingChatRequest request, String reason) {
        ToolCallingPlan fallbackPlan = planByRules(plannerMode, request);
        return ToolCallingPlan.builder()
                .plannerMode(plannerMode)
                .planningSource("mock-fallback")
                .fallbackUsed(true)
                .selectedTool(fallbackPlan.selectedTool())
                .toolArguments(fallbackPlan.toolArguments())
                .reason(reason)
                .build();
    }

    /**
     * 按工具类型补齐最小默认参数，便于本地联调时快速验证链路。
     */
    public Map<String, Object> applyDefaults(String selectedTool, Map<String, Object> inputArguments) {
        Map<String, Object> arguments = new HashMap<>(inputArguments == null ? Map.of() : inputArguments);
        return switch (selectedTool) {
            case "inventory.getBalance" -> fillInventoryDefaults(arguments);
            case "mdm.getMaterial" -> fillMaterialDefaults(arguments);
            case "sales.getOrder" -> fillSalesDefaults(arguments);
            case "purchase.getOrder" -> fillPurchaseDefaults(arguments);
            case "mdm.getWarehouse" -> fillWarehouseDefaults(arguments);
            default -> arguments;
        };
    }

    private Map<String, Object> fillInventoryDefaults(Map<String, Object> arguments) {
        arguments.putIfAbsent("materialId", 1001L);
        arguments.putIfAbsent("warehouseId", 1L);
        return arguments;
    }

    private Map<String, Object> fillMaterialDefaults(Map<String, Object> arguments) {
        if (!arguments.containsKey("materialId") && !arguments.containsKey("materialCode")) {
            arguments.put("materialId", 1001L);
        }
        return arguments;
    }

    private Map<String, Object> fillSalesDefaults(Map<String, Object> arguments) {
        if (!arguments.containsKey("orderId") && !arguments.containsKey("orderNo")) {
            arguments.put("orderNo", "SO-20260520-001");
        }
        return arguments;
    }

    private Map<String, Object> fillPurchaseDefaults(Map<String, Object> arguments) {
        if (!arguments.containsKey("orderId") && !arguments.containsKey("orderNo")) {
            arguments.put("orderNo", "PO-20260520-001");
        }
        return arguments;
    }

    private Map<String, Object> fillWarehouseDefaults(Map<String, Object> arguments) {
        if (!arguments.containsKey("warehouseId") && !arguments.containsKey("warehouseCode")) {
            arguments.put("warehouseId", 1L);
        }
        return arguments;
    }

    private boolean containsAny(String message, String... keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
