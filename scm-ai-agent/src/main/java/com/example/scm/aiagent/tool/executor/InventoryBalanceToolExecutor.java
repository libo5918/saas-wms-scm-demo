package com.example.scm.aiagent.tool.executor;

import com.example.scm.aiagent.tool.client.InventoryToolClient;
import com.example.scm.aiagent.tool.model.ToolRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 查询库存余额 Tool。
 *
 * <p>执行器只负责 Tool 协议和参数定义，真实数据获取委托给 InventoryToolClient，
 * 以便在 mock/http 等 adapter 之间切换。</p>
 */
@Component
public class InventoryBalanceToolExecutor extends AbstractMockReadOnlyToolExecutor {

    private final InventoryToolClient inventoryToolClient;

    public InventoryBalanceToolExecutor(InventoryToolClient inventoryToolClient) {
        super("inventory.getBalance", "inventory", "查询指定物料在仓库中的库存余额",
                Map.of("materialId", "物料 ID", "warehouseId", "仓库 ID", "locationId", "库位 ID"),
                List.of("materialId", "warehouseId"),
                List.of());
        this.inventoryToolClient = inventoryToolClient;
    }

    @Override
    public Object execute(ToolRequest request) {
        return inventoryToolClient.getBalance(request);
    }
}
