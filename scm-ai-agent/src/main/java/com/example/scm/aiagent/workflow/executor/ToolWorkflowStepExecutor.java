package com.example.scm.aiagent.workflow.executor;

import com.example.scm.aiagent.tool.dto.ToolInvokeRequest;
import com.example.scm.aiagent.tool.dto.ToolResponse;
import com.example.scm.aiagent.tool.service.ToolInvocationService;
import com.example.scm.aiagent.toolcalling.display.ToolCallingDisplaySchemaBuilder;
import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayData;
import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayField;
import com.example.scm.aiagent.workflow.dto.AgentWorkflowRunRequest;
import com.example.scm.aiagent.workflow.engine.AgentWorkflowExecutionContext;
import com.example.scm.aiagent.workflow.model.AgentWorkflowStep;
import com.example.scm.aiagent.workflow.model.AgentWorkflowStepDefinition;
import com.example.scm.aiagent.workflow.model.AgentWorkflowStepStatus;
import com.example.scm.aiagent.workflow.model.AgentWorkflowStepType;
import com.example.scm.aiagent.workflow.service.AgentWorkflowParameterResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/** 只读 Tool 类型 Workflow 步骤执行器。 */
@Component
public class ToolWorkflowStepExecutor extends AbstractWorkflowStepExecutor {

    private static final String QUERY_MATERIAL = "query_material";
    private static final String QUERY_INVENTORY_BALANCE = "query_inventory_balance";

    private final AgentWorkflowParameterResolver parameterResolver;
    private final ToolInvocationService toolInvocationService;
    private final ToolCallingDisplaySchemaBuilder displaySchemaBuilder;

    public ToolWorkflowStepExecutor(AgentWorkflowParameterResolver parameterResolver,
                                    ToolInvocationService toolInvocationService,
                                    ToolCallingDisplaySchemaBuilder displaySchemaBuilder) {
        this.parameterResolver = parameterResolver;
        this.toolInvocationService = toolInvocationService;
        this.displaySchemaBuilder = displaySchemaBuilder;
    }

    @Override
    public boolean supports(AgentWorkflowStepDefinition definition) {
        return definition.getStepType() == AgentWorkflowStepType.TOOL;
    }

    @Override
    public void execute(AgentWorkflowExecutionContext context, AgentWorkflowStepDefinition definition) {
        if (QUERY_MATERIAL.equals(definition.getStepCode())) {
            executeMaterialStep(context, definition);
            return;
        }
        if (QUERY_INVENTORY_BALANCE.equals(definition.getStepCode())) {
            executeInventoryStep(context, definition);
            return;
        }
        AgentWorkflowStep step = newStep(definition);
        long startedAt = beginStep(context, step);
        skipStep(context, step, "暂不支持的 Tool Workflow 步骤：" + definition.getStepCode(), startedAt);
    }

    private void executeMaterialStep(AgentWorkflowExecutionContext context, AgentWorkflowStepDefinition definition) {
        AgentWorkflowStep step = newStep(definition);
        long startedAt = beginStep(context, step);
        AgentWorkflowRunRequest request = context.getRequest();
        String materialCode = parameterResolver.resolveMaterialCode(request.getMessage(), request.getParameters());
        if (!StringUtils.hasText(materialCode)) {
            skipStep(context, step, "缺少物料编码 materialCode", startedAt);
            context.setFinalAnswer("无法生成补货建议草案：缺少物料编码 materialCode。");
            return;
        }

        ToolResponse response = invokeTool(context, definition.getToolName(), Map.of("materialCode", materialCode));
        finishToolStep(context, step, response, startedAt);
        if (response.isSuccess()) {
            ToolCallingDisplayData displayData = displaySchemaBuilder.build(response.getToolName(), response.getData());
            Map<String, Object> safeFields = safeFields(displayData);
            Object materialId = firstNonNull(asMap(response.getData()).get("id"), safeFields.get("id"), safeFields.get("materialId"));
            safeFields.put("materialId", materialId);
            step.setDisplayTitle(displayData.displayTitle());
            step.setDisplaySummary(displayData.displaySummary());
            step.setSafeFields(safeFields);
            context.putStepOutput(step.getStepCode(), safeFields);
        } else {
            context.setFinalAnswer("无法生成补货建议草案：物料查询失败，原因：" + response.getErrorMessage());
        }
    }

    private void executeInventoryStep(AgentWorkflowExecutionContext context, AgentWorkflowStepDefinition definition) {
        AgentWorkflowStep step = newStep(definition);
        long startedAt = beginStep(context, step);
        Map<String, Object> materialOutput = context.getStepOutput(QUERY_MATERIAL);
        Object materialId = materialOutput.get("materialId");
        if (materialId == null) {
            skipStep(context, step, "缺少上一步物料ID", startedAt);
            return;
        }
        AgentWorkflowRunRequest request = context.getRequest();
        Object warehouseId = parameterResolver.resolveWarehouseId(request.getMessage(), request.getParameters());
        Object locationId = parameterResolver.resolveLocationId(request.getMessage(), request.getParameters());
        if (warehouseId == null || locationId == null) {
            String missing = warehouseId == null && locationId == null ? "warehouseId、locationId"
                    : warehouseId == null ? "warehouseId" : "locationId";
            skipStep(context, step, "缺少库存查询参数：" + missing, startedAt);
            context.setFinalAnswer("无法生成完整补货建议草案：缺少库存查询参数 " + missing + "。");
            return;
        }
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("materialId", materialId);
        parameters.put("warehouseId", warehouseId);
        parameters.put("locationId", locationId);
        ToolResponse response = invokeTool(context, definition.getToolName(), parameters);
        finishToolStep(context, step, response, startedAt);
        if (response.isSuccess()) {
            ToolCallingDisplayData displayData = displaySchemaBuilder.build(response.getToolName(), response.getData());
            Map<String, Object> safeFields = safeFields(displayData);
            step.setDisplayTitle(displayData.displayTitle());
            step.setDisplaySummary(displayData.displaySummary());
            step.setSafeFields(safeFields);
            context.putStepOutput(step.getStepCode(), safeFields);
        } else {
            context.setFinalAnswer("无法生成补货建议草案：库存查询失败，原因：" + response.getErrorMessage());
        }
    }

    private ToolResponse invokeTool(AgentWorkflowExecutionContext context, String toolName, Map<String, Object> parameters) {
        ToolInvokeRequest request = new ToolInvokeRequest();
        request.setRunId(context.getRun().getRunId());
        request.setToolName(toolName);
        request.setParameters(parameters);
        return toolInvocationService.invoke(request, context.getAgentRequestContext());
    }

    private void finishToolStep(AgentWorkflowExecutionContext context, AgentWorkflowStep step,
                                ToolResponse response, long startedAt) {
        step.setStatus(response.isSuccess() ? AgentWorkflowStepStatus.SUCCESS : AgentWorkflowStepStatus.FAILED);
        step.setInputResolved(true);
        step.setErrorCode(response.getErrorCode());
        step.setErrorMessage(response.getErrorMessage());
        if (!response.isSuccess()) {
            step.setDisplaySummary(response.getErrorMessage());
        }
        finishStep(context, step, startedAt);
    }

    private Map<String, Object> safeFields(ToolCallingDisplayData displayData) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (displayData.displayFields() != null) {
            for (ToolCallingDisplayField field : displayData.displayFields()) {
                result.put(field.key(), field.value());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object data) {
        if (data instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, value) -> result.put(String.valueOf(key), value));
            return result;
        }
        return Map.of();
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
