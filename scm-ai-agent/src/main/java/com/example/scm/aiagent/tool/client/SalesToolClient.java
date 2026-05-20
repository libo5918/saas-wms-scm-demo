package com.example.scm.aiagent.tool.client;

import com.example.scm.aiagent.tool.model.ToolRequest;

import java.util.Map;

/**
 * 销售域 Tool 的业务服务客户端抽象。
 */
public interface SalesToolClient {

    /**
     * 查询销售订单详情。
     *
     * @param request 标准 Tool 请求
     * @return 销售订单数据
     */
    Map<String, Object> getOrder(ToolRequest request);
}
