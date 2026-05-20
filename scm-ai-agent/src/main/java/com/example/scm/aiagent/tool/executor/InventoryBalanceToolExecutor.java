package com.example.scm.aiagent.tool.executor;

import com.example.scm.aiagent.tool.model.ToolRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 查询库存余额 Tool 的 mock 实现。
 */
@Component
public class InventoryBalanceToolExecutor extends AbstractMockReadOnlyToolExecutor {

    public InventoryBalanceToolExecutor() {
        super("inventory.getBalance", "inventory", "查询指定物料在仓库中的库存余额",
                Map.of("materialId", "物料 ID", "warehouseId", "仓库 ID"));
    }

    @Override
    public Object execute(ToolRequest request) {
        Long materialId = longParam(request.getParameters(), "materialId", 1001L);
        Long warehouseId = longParam(request.getParameters(), "warehouseId", 1L);
        return Map.of(
                "tenantId", request.getContext().tenantId(),
                "materialId", materialId,
                "warehouseId", warehouseId,
                "availableQty", 128,
                "lockedQty", 12,
                "unit", "PCS",
                "adapterMode", "mock"
        );
    }
}
