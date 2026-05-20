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
 * <p>当前阶段不依赖真实大模型，先把“问题 -> 工具 -> 参数”的最小链路打通。</p>
 */
@Component
public class MockToolPlanner {

    /**
     * 根据用户问题和显式参数规划本次 Tool Calling。
     */
    public ToolCallingPlan plan(ToolCallingChatRequest request) {
        if (StringUtils.hasText(request.getRequestedTool())) {
            return ToolCallingPlan.builder()
                    .plannerMode("mock")
                    .selectedTool(request.getRequestedTool())
                    .toolArguments(withDefaults(request.getRequestedTool(), request.getToolArguments()))
                    .reason("requested_tool")
                    .build();
        }

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
                .plannerMode("mock")
                .selectedTool(selectedTool)
                .toolArguments(withDefaults(selectedTool, request.getToolArguments()))
                .reason("mock_rule")
                .build();
    }

    private Map<String, Object> withDefaults(String selectedTool, Map<String, Object> inputArguments) {
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
