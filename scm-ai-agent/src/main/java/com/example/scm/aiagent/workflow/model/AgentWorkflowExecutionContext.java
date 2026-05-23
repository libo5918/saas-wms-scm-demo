package com.example.scm.aiagent.workflow.model;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/** Workflow 执行过程中的内部上下文，不对外暴露 raw data。 */
@Getter
@Builder
public class AgentWorkflowExecutionContext {

    private String materialCode;
    private Object materialId;
    private Object warehouseId;
    private Object locationId;
    private Map<String, Object> materialSafeFields;
    private Map<String, Object> inventorySafeFields;
}
