package com.example.scm.aiagent.tool.client;

import com.example.scm.aiagent.tool.model.ToolRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 主数据 Tool 的 mock 客户端。
 *
 * <p>默认启用，用于本地开发、单元测试和无业务服务时的工具协议验证。</p>
 */
@Component
@ConditionalOnProperty(prefix = "ai.agent.tools", name = "adapter-mode", havingValue = "mock", matchIfMissing = true)
public class MockMdmToolClient extends AbstractToolClientSupport implements MdmToolClient {

    @Override
    public Map<String, Object> getMaterial(ToolRequest request) {
        Long materialId = longParam(request.getParameters(), "materialId", 1001L);
        String materialCode = stringParam(request.getParameters(), "materialCode", "MAT-1001");
        return Map.of(
                "tenantId", request.getContext().tenantId(),
                "materialId", materialId,
                "materialCode", materialCode,
                "materialName", "标准零件",
                "category", "spare-part",
                "status", "ENABLED",
                "adapterMode", "mock"
        );
    }
}
