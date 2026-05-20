package com.example.scm.aiagent.tool.executor;

import com.example.scm.aiagent.tool.client.MdmToolClient;
import com.example.scm.aiagent.tool.model.ToolRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 查询物料信息 Tool。
 *
 * <p>执行器不直接绑定 mock 数据，统一通过 MdmToolClient 访问主数据能力。</p>
 */
@Component
public class MaterialInfoToolExecutor extends AbstractMockReadOnlyToolExecutor {

    private final MdmToolClient mdmToolClient;

    public MaterialInfoToolExecutor(MdmToolClient mdmToolClient) {
        super("mdm.getMaterial", "mdm", "查询物料主数据基础信息",
                Map.of("materialId", "物料 ID", "materialCode", "物料编码"));
        this.mdmToolClient = mdmToolClient;
    }

    @Override
    public Object execute(ToolRequest request) {
        return mdmToolClient.getMaterial(request);
    }
}
