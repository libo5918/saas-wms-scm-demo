package com.example.scm.aiagent.toolcalling;

import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayData;
import com.example.scm.aiagent.toolcalling.display.ToolCallingDisplaySchemaBuilder;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolCallingDisplaySchemaBuilderTest {

    private final ToolCallingDisplaySchemaBuilder builder = new ToolCallingDisplaySchemaBuilder();

    @Test
    void shouldBuildMaterialDisplaySchema() {
        Map<String, Object> rawData = Map.of(
                "materialCode", "MAT-001",
                "materialName", "标准零件",
                "status", "ENABLED",
                "unit", "PCS"
        );

        ToolCallingDisplayData displayData = builder.build("mdm.getMaterial", rawData);

        assertEquals("物料信息", displayData.displayTitle());
        assertTrue(displayData.displaySummary().contains("MAT-001"));
        assertTrue(displayData.displayFields().stream().anyMatch(field -> "materialCode".equals(field.key())));
        assertSame(rawData, displayData.rawData());
    }

    @Test
    void shouldBuildInventoryBalanceDisplaySchema() {
        Map<String, Object> rawData = Map.of(
                "materialId", 1001L,
                "warehouseId", 1L,
                "availableQty", 128,
                "lockedQty", 12,
                "unit", "PCS"
        );

        ToolCallingDisplayData displayData = builder.build("inventory.getBalance", rawData);

        assertEquals("库存余额", displayData.displayTitle());
        assertTrue(displayData.displaySummary().contains("128"));
        assertTrue(displayData.displayFields().stream().anyMatch(field -> "availableQty".equals(field.key())));
        assertSame(rawData, displayData.rawData());
    }

    @Test
    void shouldFallbackForUnknownTool() {
        Map<String, Object> rawData = Map.of(
                "code", "X-001",
                "count", 3,
                "nested", Map.of("ignored", true)
        );

        ToolCallingDisplayData displayData = builder.build("unknown.tool", rawData);

        assertEquals("unknown.tool", displayData.displayTitle());
        assertEquals("已完成工具查询", displayData.displaySummary());
        assertTrue(displayData.displayFields().stream().anyMatch(field -> "code".equals(field.key())));
        assertTrue(displayData.displayFields().stream().noneMatch(field -> "nested".equals(field.key())));
        assertSame(rawData, displayData.rawData());
    }
}
