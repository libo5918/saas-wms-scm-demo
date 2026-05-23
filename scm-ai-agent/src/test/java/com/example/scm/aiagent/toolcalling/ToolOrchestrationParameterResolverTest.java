package com.example.scm.aiagent.toolcalling;

import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationExecutionSummary;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationParameterResolveResult;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationParameterResolver;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationRun;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationStep;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolOrchestrationParameterResolverTest {

    private final ToolOrchestrationParameterResolver resolver = new ToolOrchestrationParameterResolver();

    @Test
    void shouldResolveInventoryIdsFromMdmSafeFieldsAndUserMessage() {
        ToolOrchestrationParameterResolveResult result = resolver.resolve(
                run("帮我查物料 MAT-001 在仓库ID 1、库位ID 2 的库存"),
                previousStep(Map.of("materialCode", "MAT-001"), "tool=mdm.getMaterial", Map.of("materialId", 1001L)),
                targetStep("inventory.getBalance"));

        assertTrue(result.resolved());
        assertEquals(1001L, result.arguments().get("materialId"));
        assertEquals(1L, result.arguments().get("warehouseId"));
        assertEquals(2L, result.arguments().get("locationId"));
    }

    @Test
    void shouldPreferMdmReturnedMaterialIdOverPreviousArguments() {
        ToolOrchestrationParameterResolveResult result = resolver.resolve(
                run("帮我查这个物料在 warehouseId=1 的库存"),
                previousStep(Map.of("materialId", 1000L, "warehouseId", 9L), "tool=mdm.getMaterial",
                        Map.of("materialId", 1002L)),
                targetStep("inventory.getBalance"));

        assertTrue(result.resolved());
        assertEquals(1002L, result.arguments().get("materialId"));
        assertEquals(9L, result.arguments().get("warehouseId"));
    }

    @Test
    void shouldResolveMaterialIdFromSafeOutputSummary() {
        ToolOrchestrationParameterResolveResult result = resolver.resolve(
                run("帮我查这个物料在仓库ID 1 的库存"),
                previousStep(Map.of(), "tool=mdm.getMaterial, safeFields={materialId=1003}", Map.of()),
                targetStep("inventory.getBalance"));

        assertTrue(result.resolved());
        assertEquals(1003L, result.arguments().get("materialId"));
        assertEquals(1L, result.arguments().get("warehouseId"));
    }

    @Test
    void shouldSkipWhenWarehouseIdIsMissing() {
        ToolOrchestrationParameterResolveResult result = resolver.resolve(
                run("帮我查物料库存"),
                previousStep(Map.of(), "tool=mdm.getMaterial", Map.of("materialId", 1001L)),
                targetStep("inventory.getBalance"));

        assertFalse(result.resolved());
        assertTrue(result.error().contains("warehouseId"));
    }

    @Test
    void shouldNotReadSensitiveSummaryOrReturnSensitiveFields() {
        ToolOrchestrationParameterResolveResult result = resolver.resolve(
                run("帮我查这个物料在仓库ID 1 的库存"),
                previousStep(Map.of("authorization", "Bearer x", "cookie", "sid=1"),
                        "rawData={materialId=1004}, token=secret, prompt=hidden", Map.of()),
                targetStep("inventory.getBalance"));

        assertFalse(result.resolved());
        assertTrue(result.arguments().isEmpty());
        assertTrue(result.error().contains("materialId"));
    }

    @Test
    void shouldRejectUnsupportedFollowUpTool() {
        ToolOrchestrationParameterResolveResult result = resolver.resolve(
                run("帮我查物料 1001"),
                previousStep(Map.of("materialId", 1001L), "tool=mdm.getMaterial", Map.of("materialId", 1001L)),
                targetStep("sales.getOrder"));

        assertFalse(result.resolved());
        assertTrue(result.error().contains("unsupported"));
    }

    private ToolOrchestrationRun run(String message) {
        return ToolOrchestrationRun.builder()
                .runId("run-resolver")
                .userMessage(message)
                .build();
    }

    private ToolOrchestrationStep previousStep(Map<String, Object> arguments,
                                               String outputSummary,
                                               Map<String, Object> safeFields) {
        return ToolOrchestrationStep.builder()
                .stepNo(1)
                .stepRef("step-1")
                .toolName("mdm.getMaterial")
                .arguments(arguments)
                .outputSummary(outputSummary)
                .execution(ToolOrchestrationExecutionSummary.builder()
                        .success(true)
                        .toolName("mdm.getMaterial")
                        .safeFields(safeFields)
                        .build())
                .build();
    }

    private ToolOrchestrationStep targetStep(String toolName) {
        return ToolOrchestrationStep.builder()
                .stepNo(2)
                .stepRef("step-2")
                .toolName(toolName)
                .build();
    }
}
