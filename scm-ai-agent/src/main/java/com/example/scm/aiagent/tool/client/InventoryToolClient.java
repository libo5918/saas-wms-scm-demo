package com.example.scm.aiagent.tool.client;

import com.example.scm.aiagent.tool.model.ToolRequest;

import java.util.Map;

/**
 * 库存类 Tool 的业务服务客户端抽象。
 *
 * <p>ToolExecutor 只依赖该接口，不关心底层是 mock 数据还是 HTTP 调用真实库存服务。</p>
 */
public interface InventoryToolClient {

    /**
     * 查询库存余额。
     *
     * @param request 标准 Tool 请求，包含租户、用户、runId 和工具参数
     * @return 库存余额数据
     */
    Map<String, Object> getBalance(ToolRequest request);
}
