package com.example.scm.aiagent.toolcalling;

import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecutionView;
import com.example.scm.aiagent.toolcalling.answer.strategy.InventoryBalanceAnswerPromptStrategy;
import com.example.scm.aiagent.toolcalling.answer.strategy.MaterialAnswerPromptStrategy;
import com.example.scm.aiagent.toolcalling.answer.ToolCallingAnswerPromptBuilder;
import com.example.scm.aiagent.toolcalling.answer.strategy.ToolCallingAnswerPromptStrategy;
import com.example.scm.aiagent.toolcalling.answer.strategy.ToolCallingAnswerPromptStrategyRegistry;
import com.example.scm.aiagent.toolcalling.display.ToolCallingDisplaySchemaBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolCallingAnswerPromptStrategyTest {

    private final ToolCallingDisplaySchemaBuilder displaySchemaBuilder = new ToolCallingDisplaySchemaBuilder();
    private final ToolCallingAnswerPromptBuilder promptBuilder = new ToolCallingAnswerPromptBuilder(
            new ObjectMapper(),
            new ToolCallingAnswerPromptStrategyRegistry(List.of(
                    new MaterialAnswerPromptStrategy(),
                    new InventoryBalanceAnswerPromptStrategy()
            ))
    );

    @Test
    void shouldUseMaterialPromptStrategy() {
        String prompt = promptBuilder.build(
                "帮我查物料 MAT-001",
                "mdm.getMaterial",
                Map.of("materialCode", "MAT-001"),
                ToolCallingExecutionView.builder()
                        .success(true)
                        .toolName("mdm.getMaterial")
                        .data(displaySchemaBuilder.build("mdm.getMaterial",
                                Map.of("materialCode", "MAT-001", "materialName", "标准零件", "status", "ENABLED")))
                        .latencyMs(8)
                        .build()
        );

        assertTrue(prompt.contains("物料查询结果"));
        assertTrue(prompt.contains("物料编码"));
        assertTrue(prompt.contains("\"displayTitle\""));
        assertFalse(prompt.contains("Authorization"));
        assertFalse(prompt.contains("Bearer "));
    }

    @Test
    void shouldUseInventoryPromptStrategy() {
        String prompt = promptBuilder.build(
                "帮我查库存",
                "inventory.getBalance",
                Map.of("materialId", 1001L),
                ToolCallingExecutionView.builder()
                        .success(true)
                        .toolName("inventory.getBalance")
                        .data(displaySchemaBuilder.build("inventory.getBalance",
                                Map.of("materialId", 1001L, "availableQty", 128, "lockedQty", 12)))
                        .latencyMs(8)
                        .build()
        );

        assertTrue(prompt.contains("库存余额查询结果"));
        assertTrue(prompt.contains("可用数量"));
        assertTrue(prompt.contains("\"displayTitle\""));
    }

    @Test
    void shouldUseFallbackPromptStrategy() {
        ToolCallingAnswerPromptBuilder fallbackPromptBuilder = new ToolCallingAnswerPromptBuilder(
                new ObjectMapper(),
                new ToolCallingAnswerPromptStrategyRegistry(List.<ToolCallingAnswerPromptStrategy>of())
        );

        String prompt = fallbackPromptBuilder.build(
                "查一下",
                "unknown.tool",
                Map.of(),
                ToolCallingExecutionView.builder()
                        .success(true)
                        .toolName("unknown.tool")
                        .data(displaySchemaBuilder.build("unknown.tool", Map.of("code", "X-001")))
                        .latencyMs(8)
                        .build()
        );

        assertTrue(prompt.contains("通用 Tool 查询结果"));
        assertTrue(prompt.contains("\"displayTitle\""));
    }
}
