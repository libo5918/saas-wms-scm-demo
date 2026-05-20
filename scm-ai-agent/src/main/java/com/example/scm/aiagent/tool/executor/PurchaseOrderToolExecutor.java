package com.example.scm.aiagent.tool.executor;

import com.example.scm.aiagent.tool.model.ToolRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 查询采购订单 Tool 的 mock 实现。
 */
@Component
public class PurchaseOrderToolExecutor extends AbstractMockReadOnlyToolExecutor {

    public PurchaseOrderToolExecutor() {
        super("purchase.getOrder", "purchase", "查询采购订单概要和明细",
                Map.of("orderId", "采购订单 ID", "orderNo", "采购订单号"));
    }

    @Override
    public Object execute(ToolRequest request) {
        Long orderId = longParam(request.getParameters(), "orderId", 7001L);
        String orderNo = stringParam(request.getParameters(), "orderNo", "PO-20260520-001");
        return Map.of(
                "tenantId", request.getContext().tenantId(),
                "orderId", orderId,
                "orderNo", orderNo,
                "status", "RECEIVING",
                "supplierName", "mock supplier",
                "items", List.of(Map.of("materialId", 1001L, "orderedQty", 30, "receivedQty", 12)),
                "adapterMode", "mock"
        );
    }
}
