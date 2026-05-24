package com.example.scm.aiagent.multiagent.model;

import lombok.Builder;
import lombok.Getter;

/** 单个 Agent 角色的职责定义，用于展示和后续 Coordinator 调度。 */
@Getter
@Builder
public class AgentRoleDefinition {

    /** Agent 名称，例如 CoordinatorAgent。 */
    private String agentName;

    /** Agent 角色枚举。 */
    private MultiAgentRole role;

    /** 面试和状态接口可展示的职责说明。 */
    private String description;

    /** 当前 Phase 是否真实执行复杂逻辑。 */
    private boolean executable;
}
