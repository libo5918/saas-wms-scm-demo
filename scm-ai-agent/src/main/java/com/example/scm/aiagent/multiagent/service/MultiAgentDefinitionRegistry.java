package com.example.scm.aiagent.multiagent.service;

import com.example.scm.aiagent.multiagent.model.AgentRoleDefinition;
import com.example.scm.aiagent.multiagent.model.MultiAgentDefinition;
import com.example.scm.aiagent.multiagent.model.MultiAgentRole;
import org.springframework.stereotype.Service;

import java.util.List;

/** Multi-Agent 角色定义注册表，Phase 10.1 使用固定企业级角色边界。 */
@Service
public class MultiAgentDefinitionRegistry {

    private static final String DEFINITION_CODE = "enterprise_controlled_multi_agent";

    public MultiAgentDefinition getDefaultDefinition() {
        return MultiAgentDefinition.builder()
                .code(DEFINITION_CODE)
                .name("企业级受控 Multi-Agent 协作")
                .description("用于面试展示的可控多 Agent 协作骨架，不执行无约束多轮自治")
                .agents(List.of(
                        AgentRoleDefinition.builder()
                                .agentName("CoordinatorAgent")
                                .role(MultiAgentRole.COORDINATOR)
                                .description("统一调度、控制轮次、记录 run/step 状态并生成最终响应")
                                .executable(true)
                                .build(),
                        AgentRoleDefinition.builder()
                                .agentName("PlannerAgent")
                                .role(MultiAgentRole.PLANNER)
                                .description("识别用户问题后续是否需要 RAG、Tool、Workflow 或 MCP")
                                .executable(true)
                                .build(),
                        AgentRoleDefinition.builder()
                                .agentName("KnowledgeAgent")
                                .role(MultiAgentRole.KNOWLEDGE)
                                .description("后续负责 RAG 检索和知识口径解释")
                                .executable(false)
                                .build(),
                        AgentRoleDefinition.builder()
                                .agentName("ToolAgent")
                                .role(MultiAgentRole.TOOL)
                                .description("后续负责通过 ToolInvocationService 执行受治理 Tool")
                                .executable(false)
                                .build(),
                        AgentRoleDefinition.builder()
                                .agentName("ReviewerAgent")
                                .role(MultiAgentRole.REVIEWER)
                                .description("后续负责检查答案事实一致性、安全边界和失败原因保留")
                                .executable(false)
                                .build()))
                .build();
    }
}
