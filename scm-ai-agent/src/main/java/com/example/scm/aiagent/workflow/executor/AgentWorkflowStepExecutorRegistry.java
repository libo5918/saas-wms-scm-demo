package com.example.scm.aiagent.workflow.executor;

import com.example.scm.aiagent.workflow.model.AgentWorkflowStepDefinition;
import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;

/** Workflow 步骤执行器注册表。 */
@Component
public class AgentWorkflowStepExecutorRegistry {

    private final List<AgentWorkflowStepExecutor> executors;

    public AgentWorkflowStepExecutorRegistry(List<AgentWorkflowStepExecutor> executors) {
        this.executors = executors;
    }

    public AgentWorkflowStepExecutor find(AgentWorkflowStepDefinition definition) {
        return executors.stream()
                .filter(executor -> executor.supports(definition))
                .findFirst()
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BAD_REQUEST.code(),
                        "Unsupported workflow step type: " + definition.getStepType()));
    }
}
