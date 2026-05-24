package com.example.scm.aiagent.workflow.executor;

import com.example.scm.aiagent.workflow.engine.AgentWorkflowExecutionContext;
import com.example.scm.aiagent.workflow.model.AgentWorkflowStepDefinition;

/** Workflow 步骤执行器扩展点。 */
public interface AgentWorkflowStepExecutor {

    boolean supports(AgentWorkflowStepDefinition definition);

    void execute(AgentWorkflowExecutionContext context, AgentWorkflowStepDefinition definition);
}
