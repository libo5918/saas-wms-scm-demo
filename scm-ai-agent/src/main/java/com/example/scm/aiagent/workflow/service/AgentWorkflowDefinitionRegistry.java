package com.example.scm.aiagent.workflow.service;

import com.example.scm.aiagent.workflow.model.AgentWorkflowDefinition;
import com.example.scm.aiagent.workflow.model.AgentWorkflowStepDefinition;
import com.example.scm.aiagent.workflow.model.AgentWorkflowStepType;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * c 定义注册表。
 *
 * <p>Phase 6.1 仅注册固定只读演示流程，不做通用工作流引擎。</p>
 */
@Component
public class AgentWorkflowDefinitionRegistry {

    public static final String STOCK_REPLENISHMENT_WORKFLOW = "scm_stock_replenishment_advice";

    private final List<AgentWorkflowDefinition> definitions = List.of(stockReplenishmentDefinition());

    public List<AgentWorkflowDefinition> listDefinitions() {
        return definitions.stream()
                .sorted(Comparator.comparing(AgentWorkflowDefinition::getWorkflowCode))
                .toList();
    }

    public Optional<AgentWorkflowDefinition> findByCode(String workflowCode) {
        return definitions.stream()
                .filter(definition -> definition.getWorkflowCode().equals(workflowCode))
                .findFirst();
    }

    private AgentWorkflowDefinition stockReplenishmentDefinition() {
        return AgentWorkflowDefinition.builder()
                .workflowCode(STOCK_REPLENISHMENT_WORKFLOW)
                .workflowName("库存补货建议草案")
                .description("只读查询物料和库存余额，并生成补货建议草案，不创建任何业务单据。")
                .version("1.0.0")
                .enabled(true)
                .steps(List.of(
                        AgentWorkflowStepDefinition.builder()
                                .stepCode("query_material")
                                .stepName("查询物料")
                                .stepNo(1)
                                .stepType(AgentWorkflowStepType.TOOL)
                                .toolName("mdm.getMaterial")
                                .inputMapping(Map.of("materialCode", "message.materialCode"))
                                .dependsOnStepCodes(List.of())
                                .description("根据物料编码查询物料主数据。")
                                .build(),
                        AgentWorkflowStepDefinition.builder()
                                .stepCode("query_inventory_balance")
                                .stepName("查询库存余额")
                                .stepNo(2)
                                .stepType(AgentWorkflowStepType.TOOL)
                                .toolName("inventory.getBalance")
                                .inputMapping(Map.of(
                                        "materialId", "query_material.id",
                                        "warehouseId", "messageOrParameters.warehouseId",
                                        "locationId", "messageOrParameters.locationId"))
                                .dependsOnStepCodes(List.of("query_material"))
                                .description("根据物料ID、仓库ID、库位ID查询库存余额。")
                                .build(),
                        AgentWorkflowStepDefinition.builder()
                                .stepCode("generate_advice")
                                .stepName("生成补货建议草案")
                                .stepNo(3)
                                .stepType(AgentWorkflowStepType.SUMMARY)
                                .dependsOnStepCodes(List.of("query_material", "query_inventory_balance"))
                                .description("基于只读查询结果生成中文补货建议草案，不执行写操作。")
                                .build()))
                .build();
    }
}
