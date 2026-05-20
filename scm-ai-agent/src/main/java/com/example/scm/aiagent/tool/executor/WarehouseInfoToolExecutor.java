package com.example.scm.aiagent.tool.executor;

import com.example.scm.aiagent.tool.client.WarehouseToolClient;
import com.example.scm.aiagent.tool.model.ToolRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 查询仓库信息 Tool。
 */
@Component
public class WarehouseInfoToolExecutor extends AbstractMockReadOnlyToolExecutor {

    private final WarehouseToolClient warehouseToolClient;

    public WarehouseInfoToolExecutor(WarehouseToolClient warehouseToolClient) {
        super("mdm.getWarehouse", "mdm", "查询仓库主数据基础信息",
                Map.of("warehouseId", "仓库 ID", "warehouseCode", "仓库编码"),
                List.of(),
                List.of(List.of("warehouseId", "warehouseCode")));
        this.warehouseToolClient = warehouseToolClient;
    }

    @Override
    public Object execute(ToolRequest request) {
        return warehouseToolClient.getWarehouse(request);
    }
}
