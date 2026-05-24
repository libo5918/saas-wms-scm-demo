package com.example.scm.aiagent.multiagent.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** Multi-Agent 协作定义，描述当前可用角色和边界。 */
@Getter
@Builder
public class MultiAgentDefinition {

    private String code;
    private String name;
    private String description;
    private List<AgentRoleDefinition> agents;
}
