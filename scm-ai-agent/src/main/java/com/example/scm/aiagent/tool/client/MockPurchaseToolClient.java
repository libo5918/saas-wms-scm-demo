package com.example.scm.aiagent.tool.client;

import com.example.scm.aiagent.tool.model.ToolRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 采购 Tool 的 mock 客户端。
 */
@Component
@ConditionalOnProperty(prefix = "ai.agent.tools", name = "adapter-mode", havingValue = "mock", matchIfMissing = true)
public class MockPurchaseToolClient extends AbstractToolClientSupport implements PurchaseToolClient {

    @Override
    public Map<String, Object> getOrder(ToolRequest request) {
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
