package com.example.scm.aiagent.agent.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/** Tool Calling 执行结果的脱敏概要。 */
@Getter
@Builder
public class AgentToolView {

    private String selectedTool;
    private Map<String, Object> toolArguments;
    private AgentToolExecutionView execution;
}
