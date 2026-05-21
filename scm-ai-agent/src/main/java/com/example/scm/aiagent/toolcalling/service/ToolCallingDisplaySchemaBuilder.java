package com.example.scm.aiagent.toolcalling.service;

import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayData;
import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayField;
import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayItem;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool Calling 统一展示 schema 构建器。
 *
 * <p>负责把不同 Tool 的原始返回收敛成稳定的展示结构，同时通过 rawData 保留原始数据。</p>
 */
@Component
public class ToolCallingDisplaySchemaBuilder {

    /**
     * 根据工具名和原始返回构建展示数据。
     */
    public ToolCallingDisplayData build(String toolName, Object rawData) {
        Map<String, Object> data = asMap(rawData);
        return switch (toolName) {
            case "mdm.getMaterial" -> buildMaterial(data, rawData);
            case "mdm.getWarehouse" -> buildWarehouse(data, rawData);
            case "inventory.getBalance" -> buildInventoryBalance(data, rawData);
            case "sales.getOrder" -> buildOrder("销售订单", data, rawData);
            case "purchase.getOrder" -> buildOrder("采购订单", data, rawData);
            default -> buildFallback(toolName, data, rawData);
        };
    }

    private ToolCallingDisplayData buildMaterial(Map<String, Object> data, Object rawData) {
        String materialCode = firstText(data, "materialCode", "code");
        String materialName = firstText(data, "materialName", "name");
        List<ToolCallingDisplayField> fields = new ArrayList<>();
        addField(fields, "materialCode", "物料编码", materialCode);
        addField(fields, "materialName", "物料名称", materialName);
        addField(fields, "status", "状态", firstText(data, "status"));
        addField(fields, "category", "分类", firstText(data, "category", "materialType"));
        addField(fields, "unit", "单位", firstText(data, "unit"));

        return ToolCallingDisplayData.builder()
                .displayTitle("物料信息")
                .displaySummary(summary("已查询到物料", materialCode, materialName))
                .displayFields(fields)
                .displayItems(List.of())
                .rawData(rawData)
                .build();
    }

    private ToolCallingDisplayData buildWarehouse(Map<String, Object> data, Object rawData) {
        String warehouseCode = firstText(data, "warehouseCode", "code");
        String warehouseName = firstText(data, "warehouseName", "name");
        List<ToolCallingDisplayField> fields = new ArrayList<>();
        addField(fields, "warehouseCode", "仓库编码", warehouseCode);
        addField(fields, "warehouseName", "仓库名称", warehouseName);
        addField(fields, "warehouseType", "仓库类型", firstText(data, "warehouseType", "type"));
        addField(fields, "status", "状态", firstText(data, "status"));

        return ToolCallingDisplayData.builder()
                .displayTitle("仓库信息")
                .displaySummary(summary("已查询到仓库", warehouseCode, warehouseName))
                .displayFields(fields)
                .displayItems(List.of())
                .rawData(rawData)
                .build();
    }

    private ToolCallingDisplayData buildInventoryBalance(Map<String, Object> data, Object rawData) {
        List<ToolCallingDisplayField> fields = new ArrayList<>();
        addField(fields, "materialId", "物料ID", firstText(data, "materialId"));
        addField(fields, "materialCode", "物料编码", firstText(data, "materialCode", "code"));
        addField(fields, "warehouseId", "仓库ID", firstText(data, "warehouseId"));
        addField(fields, "warehouseCode", "仓库编码", firstText(data, "warehouseCode"));
        addField(fields, "locationId", "库位ID", firstText(data, "locationId"));
        addField(fields, "availableQty", "可用数量", firstText(data, "availableQty", "available"));
        addField(fields, "lockedQty", "锁定数量", firstText(data, "lockedQty", "locked"));
        addField(fields, "unit", "单位", firstText(data, "unit"));

        String material = firstText(data, "materialCode", "materialId");
        String availableQty = firstText(data, "availableQty", "available");
        String summary = "已查询到库存余额";
        if (StringUtils.hasText(material)) {
            summary += "，物料 " + material;
        }
        if (StringUtils.hasText(availableQty)) {
            summary += " 可用 " + availableQty;
        }

        return ToolCallingDisplayData.builder()
                .displayTitle("库存余额")
                .displaySummary(summary)
                .displayFields(fields)
                .displayItems(List.of())
                .rawData(rawData)
                .build();
    }

    private ToolCallingDisplayData buildOrder(String title, Map<String, Object> data, Object rawData) {
        String orderNo = firstText(data, "orderNo");
        List<ToolCallingDisplayField> fields = new ArrayList<>();
        addField(fields, "orderNo", "订单号", orderNo);
        addField(fields, "orderId", "订单ID", firstText(data, "orderId"));
        addField(fields, "status", "状态", firstText(data, "status"));
        addField(fields, "customerName", "客户", firstText(data, "customerName", "customer"));
        addField(fields, "supplierName", "供应商", firstText(data, "supplierName", "supplier"));

        return ToolCallingDisplayData.builder()
                .displayTitle(title)
                .displaySummary(summary("已查询到" + title, orderNo, null))
                .displayFields(fields)
                .displayItems(buildItems(data.get("items")))
                .rawData(rawData)
                .build();
    }

    private ToolCallingDisplayData buildFallback(String toolName, Map<String, Object> data, Object rawData) {
        return ToolCallingDisplayData.builder()
                .displayTitle(StringUtils.hasText(toolName) ? toolName : "Tool 查询结果")
                .displaySummary("已完成工具查询")
                .displayFields(buildScalarFields(data))
                .displayItems(List.of())
                .rawData(rawData)
                .build();
    }

    private List<ToolCallingDisplayItem> buildItems(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<ToolCallingDisplayItem> items = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> itemData = asMap(item);
            List<ToolCallingDisplayField> fields = buildScalarFields(itemData);
            String title = firstText(itemData, "materialName", "materialCode", "materialId", "skuCode", "lineNo");
            items.add(ToolCallingDisplayItem.builder()
                    .title(StringUtils.hasText(title) ? title : "明细")
                    .fields(fields)
                    .rawData(item)
                    .build());
        }
        return items;
    }

    private List<ToolCallingDisplayField> buildScalarFields(Map<String, Object> data) {
        List<ToolCallingDisplayField> fields = new ArrayList<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (isScalar(entry.getValue())) {
                addField(fields, entry.getKey(), entry.getKey(), entry.getValue());
            }
        }
        return fields;
    }

    private void addField(List<ToolCallingDisplayField> fields, String key, String label, Object value) {
        if (value != null && StringUtils.hasText(String.valueOf(value))) {
            fields.add(ToolCallingDisplayField.builder()
                    .key(key)
                    .label(label)
                    .value(value)
                    .build());
        }
    }

    private String summary(String prefix, String code, String name) {
        StringBuilder summary = new StringBuilder(prefix);
        if (StringUtils.hasText(code)) {
            summary.append(" ").append(code);
        }
        if (StringUtils.hasText(name)) {
            summary.append("（").append(name).append("）");
        }
        return summary.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object data) {
        if (data instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, value) -> result.put(String.valueOf(key), value));
            return result;
        }
        return Map.of();
    }

    private boolean isScalar(Object value) {
        return value == null
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character;
    }

    private String firstText(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object value = data.get(key);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return null;
    }
}
