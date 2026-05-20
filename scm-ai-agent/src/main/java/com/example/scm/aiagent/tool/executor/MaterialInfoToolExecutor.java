package com.example.scm.aiagent.tool.executor;

import com.example.scm.aiagent.tool.model.ToolRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 查询物料信息 Tool 的 mock 实现。
 */
@Component
public class MaterialInfoToolExecutor extends AbstractMockReadOnlyToolExecutor {

    public MaterialInfoToolExecutor() {
        super("mdm.getMaterial", "mdm", "查询物料主数据基础信息",
                Map.of("materialId", "物料 ID", "materialCode", "物料编码"));
    }

    @Override
    public Object execute(ToolRequest request) {
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
