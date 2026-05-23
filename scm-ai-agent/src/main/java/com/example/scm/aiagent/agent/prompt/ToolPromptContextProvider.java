package com.example.scm.aiagent.agent.prompt;

import com.example.scm.aiagent.agent.dto.AgentToolExecutionView;
import com.example.scm.aiagent.agent.dto.AgentToolView;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool 执行结果 Provider。
 */
@Component
public class ToolPromptContextProvider implements AgentPromptContextProvider {

    @Override
    public List<AgentPromptSection> provide(AgentPromptBuildRequest request) {
        AgentToolView tool = request.getTool();
        if (tool == null) {
            return List.of();
        }
        Map<String, Object> structuredData = new LinkedHashMap<>();
        structuredData.put("selectedTool", tool.getSelectedTool());
        structuredData.put("toolArguments", tool.getToolArguments() == null ? Map.of() : tool.getToolArguments());
        structuredData.put("execution", toExecutionContext(tool.getExecution()));
        return List.of(AgentPromptSection.builder()
                .type(AgentPromptContextType.TOOL_EXECUTION)
                .source(AgentPromptContextSource.TOOL)
                .title("工具执行结果")
                .content("以下是 Tool display schema 与执行状态摘要。")
                .structuredData(structuredData)
                .priority(30)
                .maxLength(2500)
                .included(true)
                .build());
    }

    private Map<String, Object> toExecutionContext(AgentToolExecutionView execution) {
        if (execution == null) {
            return Map.of();
        }
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("success", execution.isSuccess());
        context.put("toolName", execution.getToolName());
        context.put("errorCode", execution.getErrorCode());
        context.put("errorMessage", execution.getErrorMessage());
        context.put("displayTitle", execution.getDisplayTitle());
        context.put("displaySummary", execution.getDisplaySummary());
        context.put("displayFields", execution.getDisplayFields() == null ? List.of() : execution.getDisplayFields());
        context.put("displayItems", execution.getDisplayItems() == null ? List.of() : execution.getDisplayItems());
        context.put("latencyMs", execution.getLatencyMs());
        return context;
    }
}
