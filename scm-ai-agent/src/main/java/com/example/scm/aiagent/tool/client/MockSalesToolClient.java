package com.example.scm.aiagent.tool.client;

import com.example.scm.aiagent.tool.model.ToolRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 销售 Tool 的 mock 客户端。
 */
@Component
@ConditionalOnProperty(prefix = "ai.agent.tools", name = "adapter-mode", havingValue = "mock", matchIfMissing = true)
public class MockSalesToolClient extends AbstractToolClientSupport implements SalesToolClient {

    @Override
    public Map<String, Object> getOrder(ToolRequest request) {
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
