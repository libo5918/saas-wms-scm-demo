package com.example.scm.aiagent.tool.client;

import com.example.scm.aiagent.tool.model.ToolRequest;

import java.util.Map;

/**
 * 仓库主数据 Tool 的业务服务客户端抽象。
 */
public interface WarehouseToolClient {

    /**
     * 查询仓库详情。
     *
     * @param request 标准 Tool 请求
     * @return 仓库主数据
     */
    Map<String, Object> getWarehouse(ToolRequest request);
}
