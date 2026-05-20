package com.example.scm.aiagent.tool.client;

import com.example.scm.aiagent.tool.model.ToolRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 库存 Tool 的 mock 客户端。
 *
 * <p>默认启用，保证本地无库存服务时 Tools API 仍可验证。</p>
 */
@Component
@ConditionalOnProperty(prefix = "ai.agent.tools", name = "adapter-mode", havingValue = "mock", matchIfMissing = true)
public class MockInventoryToolClient extends AbstractToolClientSupport implements InventoryToolClient {

    @Override
    public Map<String, Object> getBalance(ToolRequest request) {
        Long materialId = longParam(request.getParameters(), "materialId", 1001L);
        Long warehouseId = longParam(request.getParameters(), "warehouseId", 1L);
        Long locationId = longParam(request.getParameters(), "locationId", 1L);
        return Map.of(
                "tenantId", request.getContext().tenantId(),
                "materialId", materialId,
                "warehouseId", warehouseId,
                "locationId", locationId,
                "availableQty", 128,
                "lockedQty", 12,
                "unit", "PCS",
                "adapterMode", "mock"
        );
    }
}
