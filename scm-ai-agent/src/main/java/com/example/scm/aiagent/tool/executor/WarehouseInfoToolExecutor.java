package com.example.scm.aiagent.tool.executor;

import com.example.scm.aiagent.tool.model.ToolRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 查询仓库信息 Tool 的 mock 实现。
 */
@Component
public class WarehouseInfoToolExecutor extends AbstractMockReadOnlyToolExecutor {

    public WarehouseInfoToolExecutor() {
        super("mdm.getWarehouse", "mdm", "查询仓库主数据基础信息",
                Map.of("warehouseId", "仓库 ID", "warehouseCode", "仓库编码"));
    }

    @Override
    public Object execute(ToolRequest request) {
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
