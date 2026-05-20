package com.example.scm.aiagent.tool.executor;

import com.example.scm.aiagent.tool.client.SalesToolClient;
import com.example.scm.aiagent.tool.model.ToolRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 查询销售订单 Tool。
 */
@Component
public class SalesOrderToolExecutor extends AbstractMockReadOnlyToolExecutor {

    private final SalesToolClient salesToolClient;

    public SalesOrderToolExecutor(SalesToolClient salesToolClient) {
        super("sales.getOrder", "sales", "查询销售订单概要和明细",
                Map.of("orderId", "销售订单 ID", "orderNo", "销售订单号"),
                List.of(),
                List.of(List.of("orderId", "orderNo")));
        this.salesToolClient = salesToolClient;
    }

    @Override
    public Object execute(ToolRequest request) {
        return salesToolClient.getOrder(request);
    }
}
