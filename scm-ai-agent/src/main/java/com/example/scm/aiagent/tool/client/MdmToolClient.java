package com.example.scm.aiagent.tool.client;

import com.example.scm.aiagent.tool.model.ToolRequest;

import java.util.Map;

/**
 * 主数据类 Tool 的业务服务客户端抽象。
 *
 * <p>后续物料、仓库、供应商等主数据工具都可以复用该抽象继续扩展。</p>
 */
public interface MdmToolClient {

    /**
     * 查询物料详情。
     *
     * @param request 标准 Tool 请求，包含租户、用户、runId 和工具参数
     * @return 物料主数据
     */
    Map<String, Object> getMaterial(ToolRequest request);
}
