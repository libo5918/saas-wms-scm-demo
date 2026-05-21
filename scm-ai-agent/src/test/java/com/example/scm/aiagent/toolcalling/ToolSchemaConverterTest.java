package com.example.scm.aiagent.toolcalling;

import com.example.scm.aiagent.tool.model.ToolDefinition;
import com.example.scm.aiagent.toolcalling.model.SpringAiToolDescriptor;
import com.example.scm.aiagent.toolcalling.schema.ToolSchemaConverter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolSchemaConverterTest {

    private final ToolSchemaConverter converter = new ToolSchemaConverter();

    @Test
    void shouldConvertToolDefinitionToSpringAiSchema() {
        ToolDefinition definition = ToolDefinition.builder()
                .name("sales.getOrder")
                .domain("sales")
                .description("查询销售订单")
                .readOnly(true)
                .parameters(Map.of("orderId", "订单 ID", "orderNo", "订单号"))
                .requiredParameters(List.of())
                .oneOfRequiredGroups(List.of(List.of("orderId", "orderNo")))
                .build();

        SpringAiToolDescriptor descriptor = converter.convert(definition);

        assertEquals("sales.getOrder", descriptor.getToolName());
        assertEquals("查询销售订单", descriptor.getDescription());
        assertTrue(descriptor.isReadOnly());
        assertEquals("object", descriptor.getInputSchema().getType());
        assertEquals("integer", descriptor.getInputSchema().getProperties().get("orderId").getType());
        assertEquals("string", descriptor.getInputSchema().getProperties().get("orderNo").getType());
        assertFalse(descriptor.getInputSchema().getProperties().get("orderId").isRequired());
        assertEquals(List.of(List.of("orderId", "orderNo")), descriptor.getInputSchema().getOneOfRequiredGroups());
    }
}
