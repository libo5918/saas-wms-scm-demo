package com.example.scm.aiagent.toolcalling.service;

import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecutionView;
import com.example.scm.aiagent.toolcalling.model.ToolCallingPlan;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * Tool Calling Chat 答案生成器。
 *
 * <p>当前阶段先基于服务端模板和工具返回字段组织中文答案，
 * 为后续“工具执行后再由模型总结答案”预留清晰边界。</p>
 */
@Component
public class ToolCallingAnswerBuilder {

    /**
     * 基于规划结果和执行结果构造最终回答。
     */
    public String buildAnswer(ToolCallingPlan plan, ToolCallingExecutionView execution) {
        if (!execution.isSuccess()) {
            return "我已尝试调用工具 `" + plan.selectedTool() + "`，但执行失败："
                    + defaultText(execution.getErrorMessage(), "未知错误");
        }

        Map<String, Object> data = asMap(execution.getData());
        return switch (plan.selectedTool()) {
            case "mdm.getMaterial" -> buildMaterialAnswer(data);
            case "inventory.getBalance" -> buildInventoryAnswer(data);
            case "sales.getOrder" -> buildSalesOrderAnswer(data);
            case "purchase.getOrder" -> buildPurchaseOrderAnswer(data);
            case "mdm.getWarehouse" -> buildWarehouseAnswer(data);
            default -> "已根据你的问题调用工具 `" + plan.selectedTool() + "` 完成查询。";
        };
    }

    private String buildMaterialAnswer(Map<String, Object> data) {
        String materialName = firstText(data, "materialName", "name");
        String materialCode = firstText(data, "materialCode", "code");
        String status = firstText(data, "status");
        String category = firstText(data, "category", "materialType");
        String unit = firstText(data, "unit");

        StringBuilder answer = new StringBuilder("已查询到物料信息");
        if (StringUtils.hasText(materialName)) {
            answer.append("：").append(materialName);
        }
        if (StringUtils.hasText(materialCode)) {
            answer.append("（编码 ").append(materialCode).append("）");
        }
        appendSegment(answer, status, "状态");
        appendSegment(answer, category, "分类");
        appendSegment(answer, unit, "单位");
        answer.append("。");
        return answer.toString();
    }

    private String buildInventoryAnswer(Map<String, Object> data) {
        String materialId = firstText(data, "materialId");
        String warehouseId = firstText(data, "warehouseId");
        String locationId = firstText(data, "locationId");
        String availableQty = firstText(data, "availableQty", "available");
        String lockedQty = firstText(data, "lockedQty", "locked");
        String unit = defaultText(firstText(data, "unit"), "");

        StringBuilder answer = new StringBuilder("已查询到库存余额");
        if (StringUtils.hasText(materialId)) {
            answer.append("：物料 ").append(materialId);
        }
        if (StringUtils.hasText(warehouseId)) {
            answer.append(" 在仓库 ").append(warehouseId);
        }
        if (StringUtils.hasText(locationId)) {
            answer.append("、库位 ").append(locationId);
        }
        if (StringUtils.hasText(availableQty)) {
            answer.append("，可用 ").append(availableQty);
            if (StringUtils.hasText(unit)) {
                answer.append(" ").append(unit);
            }
        }
        if (StringUtils.hasText(lockedQty)) {
            answer.append("，锁定 ").append(lockedQty);
            if (StringUtils.hasText(unit)) {
                answer.append(" ").append(unit);
            }
        }
        answer.append("。");
        return answer.toString();
    }

    private String buildSalesOrderAnswer(Map<String, Object> data) {
        String orderNo = firstText(data, "orderNo");
        String orderId = firstText(data, "orderId");
        String status = firstText(data, "status");
        String customerName = firstText(data, "customerName", "customer");
        int itemCount = listSize(data.get("items"));

        StringBuilder answer = new StringBuilder("已查询到销售订单");
        if (StringUtils.hasText(orderNo)) {
            answer.append(" ").append(orderNo);
        } else if (StringUtils.hasText(orderId)) {
            answer.append(" ID=").append(orderId);
        }
        appendSegment(answer, status, "状态");
        appendSegment(answer, customerName, "客户");
        if (itemCount > 0) {
            answer.append("，明细 ").append(itemCount).append(" 行");
        }
        answer.append("。");
        return answer.toString();
    }

    private String buildPurchaseOrderAnswer(Map<String, Object> data) {
        String orderNo = firstText(data, "orderNo");
        String orderId = firstText(data, "orderId");
        String status = firstText(data, "status");
        String supplierName = firstText(data, "supplierName", "supplier");
        int itemCount = listSize(data.get("items"));

        StringBuilder answer = new StringBuilder("已查询到采购订单");
        if (StringUtils.hasText(orderNo)) {
            answer.append(" ").append(orderNo);
        } else if (StringUtils.hasText(orderId)) {
            answer.append(" ID=").append(orderId);
        }
        appendSegment(answer, status, "状态");
        appendSegment(answer, supplierName, "供应商");
        if (itemCount > 0) {
            answer.append("，明细 ").append(itemCount).append(" 行");
        }
        answer.append("。");
        return answer.toString();
    }

    private String buildWarehouseAnswer(Map<String, Object> data) {
        String warehouseName = firstText(data, "warehouseName", "name");
        String warehouseCode = firstText(data, "warehouseCode", "code");
        String warehouseType = firstText(data, "warehouseType", "type");
        String status = firstText(data, "status");

        StringBuilder answer = new StringBuilder("已查询到仓库信息");
        if (StringUtils.hasText(warehouseName)) {
            answer.append("：").append(warehouseName);
        }
        if (StringUtils.hasText(warehouseCode)) {
            answer.append("（编码 ").append(warehouseCode).append("）");
        }
        appendSegment(answer, warehouseType, "类型");
        appendSegment(answer, status, "状态");
        answer.append("。");
        return answer.toString();
    }

    private void appendSegment(StringBuilder answer, String value, String label) {
        if (StringUtils.hasText(value)) {
            answer.append("，").append(label).append(" ").append(value);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object data) {
        if (data instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private int listSize(Object value) {
        if (value instanceof List<?> list) {
            return list.size();
        }
        return 0;
    }

    private String firstText(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object value = data.get(key);
            if (value != null) {
                String text = String.valueOf(value);
                if (StringUtils.hasText(text)) {
                    return text;
                }
            }
        }
        return null;
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
