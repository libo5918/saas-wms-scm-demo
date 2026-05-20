package com.example.scm.aiagent.tool.executor;

import com.example.scm.aiagent.tool.model.ToolRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 查询销售订单 Tool 的 mock 实现。
 */
@Component
public class SalesOrderToolExecutor extends AbstractMockReadOnlyToolExecutor {

    public SalesOrderToolExecutor() {
        super("sales.getOrder", "sales", "查询销售订单概要和明细",
                Map.of("orderId", "销售订单 ID", "orderNo", "销售订单号"));
    }

    @Override
    public Object execute(ToolRequest request) {
        Long orderId = longParam(request.getParameters(), "orderId", 5001L);
        String orderNo = stringParam(request.getParameters(), "orderNo", "SO-20260520-001");
        return Map.of(
                "tenantId", request.getContext().tenantId(),
                "orderId", orderId,
                "orderNo", orderNo,
                "status", "ALLOCATED",
                "customerName", "mock customer",
                "items", List.of(Map.of("materialId", 1001L, "qty", 10, "locked", true)),
                "adapterMode", "mock"
        );
    }
}
