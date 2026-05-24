package com.example.scm.aiagent.workflow.service;

import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.workflow.dto.AgentWorkflowDefinitionView;
import com.example.scm.aiagent.workflow.dto.AgentWorkflowRunRequest;
import com.example.scm.aiagent.workflow.dto.AgentWorkflowRunResponse;
import com.example.scm.aiagent.workflow.engine.AgentWorkflowEngine;
import com.example.scm.aiagent.workflow.model.AgentWorkflowDefinition;
import com.example.scm.aiagent.workflow.model.AgentWorkflowRun;
import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Agent Workflow 门面服务。
 *
 * <p>对外负责定义查询、运行查询和启动 run；具体步骤执行交给 Workflow Engine。</p>
 */
@Service
public class AgentWorkflowService {

    private final AgentWorkflowDefinitionRegistry definitionRegistry;
    private final AgentWorkflowRunStore runStore;
    private final AgentWorkflowViewMapper viewMapper;
    private final AgentWorkflowEngine workflowEngine;

    public AgentWorkflowService(AgentWorkflowDefinitionRegistry definitionRegistry,
                                AgentWorkflowRunStore runStore,
                                AgentWorkflowViewMapper viewMapper,
                                AgentWorkflowEngine workflowEngine) {
        this.definitionRegistry = definitionRegistry;
        this.runStore = runStore;
        this.viewMapper = viewMapper;
        this.workflowEngine = workflowEngine;
    }

    public List<AgentWorkflowDefinitionView> listDefinitions() {
        return definitionRegistry.listDefinitions().stream()
                .map(viewMapper::toDefinitionView)
                .toList();
    }

    public AgentWorkflowRunResponse getRun(String runId) {
        return runStore.get(runId)
                .map(viewMapper::toRunResponse)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Workflow run not found: " + runId));
    }

    public List<AgentWorkflowRunResponse> listRuns(int limit) {
        return runStore.list(limit).stream().map(viewMapper::toRunResponse).toList();
    }

    public AgentWorkflowRunResponse run(String workflowCode,
                                        AgentWorkflowRunRequest request,
                                        AgentRequestContext context) {
        AgentWorkflowDefinition definition = definitionRegistry.findByCode(workflowCode)
                .filter(AgentWorkflowDefinition::isEnabled)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Workflow not found: " + workflowCode));
        AgentWorkflowRun run = workflowEngine.execute(definition, request, context);
        return viewMapper.toRunResponse(run);
    }
}
