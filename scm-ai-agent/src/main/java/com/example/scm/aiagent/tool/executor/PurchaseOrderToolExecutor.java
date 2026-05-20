package com.example.scm.aiagent.tool.executor;

import com.example.scm.aiagent.tool.client.PurchaseToolClient;
import com.example.scm.aiagent.tool.model.ToolRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 查询采购订单 Tool。
 */
@Component
public class PurchaseOrderToolExecutor extends AbstractMockReadOnlyToolExecutor {

    private final PurchaseToolClient purchaseToolClient;

    public PurchaseOrderToolExecutor(PurchaseToolClient purchaseToolClient) {
        super("purchase.getOrder", "purchase", "查询采购订单概要和明细",
                Map.of("orderId", "采购订单 ID", "orderNo", "采购订单号"));
        this.purchaseToolClient = purchaseToolClient;
    }

    @Override
    public Object execute(ToolRequest request) {
        return purchaseToolClient.getOrder(request);
    }
}
