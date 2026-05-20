package com.example.scm.aiagent.tool.client;

import com.example.scm.aiagent.tool.model.ToolRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 仓库 Tool 的 mock 客户端。
 */
@Component
@ConditionalOnProperty(prefix = "ai.agent.tools", name = "adapter-mode", havingValue = "mock", matchIfMissing = true)
public class MockWarehouseToolClient extends AbstractToolClientSupport implements WarehouseToolClient {

    @Override
    public Map<String, Object> getWarehouse(ToolRequest request) {
        Long warehouseId = longParam(request.getParameters(), "warehouseId", 1L);
        String warehouseCode = stringParam(request.getParameters(), "warehouseCode", "WH-001");
        return Map.of(
                "tenantId", request.getContext().tenantId(),
                "warehouseId", warehouseId,
                "warehouseCode", warehouseCode,
                "warehouseName", "华东主仓",
                "warehouseType", "MAIN",
                "status", "ENABLED",
                "adapterMode", "mock"
        );
    }
}
