package com.example.scm.aiagent.tool.client;

import com.example.scm.aiagent.tool.model.ToolRequest;

import java.util.Map;

/**
 * 采购域 Tool 的业务服务客户端抽象。
 */
public interface PurchaseToolClient {

    /**
     * 查询采购订单详情。
     *
     * @param request 标准 Tool 请求
     * @return 采购订单数据
     */
    Map<String, Object> getOrder(ToolRequest request);
}
