package com.example.scm.aiagent.multiagent.service;

import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.multiagent.dto.MultiAgentChatRequest;
import com.example.scm.aiagent.multiagent.model.MultiAgentPlan;
import com.example.scm.aiagent.toolcalling.application.ToolCallingChatService;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatRequest;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatResponse;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecutionView;
import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayData;
import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayField;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** ToolAgent 执行器，复用 Tool Calling Chat/Orchestrator 主链路。 */
@Service
public class MultiAgentToolService {

    private final ToolCallingChatService toolCallingChatService;

    public MultiAgentToolService(ToolCallingChatService toolCallingChatService) {
        this.toolCallingChatService = toolCallingChatService;
    }

    public Map<String, Object> execute(MultiAgentChatRequest request, AgentRequestContext context,
                                       MultiAgentPlan plan, String runId, boolean enabled) {
        if (!plan.isNeedTool()) {
            return skipped("Planner 未要求执行 Tool");
        }
        if (!enabled) {
            return skipped("Multi-Agent Tool 能力未启用");
        }

        ToolCallingChatRequest toolRequest = new ToolCallingChatRequest();
        toolRequest.setRunId(runId);
        toolRequest.setMessage(request.getMessage());
        toolRequest.setPlannerMode(request.getPlannerMode());
        toolRequest.setRequestedTool(request.getRequestedTool());
        toolRequest.setRequestedDomain(request.getRequestedDomain());
        toolRequest.setRouteTags(request.getRouteTags());
        ToolCallingChatResponse response = toolCallingChatService.chat(toolRequest, context);
        return toSafeToolSummary(response);
    }

    private Map<String, Object> skipped(String reason) {
        return Map.of("status", "SKIPPED", "skipReason", reason);
    }

    private Map<String, Object> toSafeToolSummary(ToolCallingChatResponse response) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("status", response.getExecution() != null && response.getExecution().isSuccess() ? "SUCCESS" : "FAILED");
        summary.put("selectedTool", response.getSelectedTool());
        summary.put("toolArguments", response.getToolArguments() == null ? Map.of() : response.getToolArguments());
        summary.put("planningSource", response.getPlanningSource());
        summary.put("answer", safeSnippet(response.getAnswer(), 500));
        summary.put("latencyMs", response.getLatencyMs());
        summary.put("execution", toSafeExecution(response.getExecution()));
        return summary;
    }

    private Map<String, Object> toSafeExecution(ToolCallingExecutionView execution) {
        if (execution == null) {
            return Map.of("success", false);
        }
        Map<String, Object> executionMap = new LinkedHashMap<>();
        executionMap.put("success", execution.isSuccess());
        executionMap.put("toolName", execution.getToolName());
        executionMap.put("errorCode", execution.getErrorCode());
        executionMap.put("errorMessage", execution.getErrorMessage());
        executionMap.put("latencyMs", execution.getLatencyMs());
        executionMap.putAll(displaySummary(execution.getData()));
        return executionMap;
    }

    private Map<String, Object> displaySummary(Object data) {
        if (data instanceof ToolCallingDisplayData displayData) {
            return Map.of(
                    "displayTitle", safe(displayData.displayTitle()),
                    "displaySummary", safe(displayData.displaySummary()),
                    "safeFields", toSafeFields(displayData.displayFields()),
                    "displayItems", displayData.displayItems() == null ? List.of() : displayData.displayItems().stream()
                            .map(item -> Map.of("title", safe(item.title()))).toList()
            );
        }
        if (data instanceof Map<?, ?> map && map.containsKey("displayTitle")) {
            return Map.of(
                    "displayTitle", safe(map.get("displayTitle")),
                    "displaySummary", safe(map.get("displaySummary")),
                    "safeFields", Map.of()
            );
        }
        return Map.of("safeFields", Map.of());
    }

    private Map<String, Object> toSafeFields(List<ToolCallingDisplayField> fields) {
        Map<String, Object> safeFields = new LinkedHashMap<>();
        if (fields == null) {
            return safeFields;
        }
        for (ToolCallingDisplayField field : fields) {
            safeFields.put(field.key(), field.value());
        }
        return safeFields;
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String safeSnippet(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String sanitized = value
                .replaceAll("(?i)authorization\\s*[:=]\\s*\\S+", "[REDACTED]")
                .replaceAll("(?i)cookie\\s*[:=]\\s*\\S+", "[REDACTED]")
                .replaceAll("(?i)token\\s*[:=]\\s*\\S+", "[REDACTED]");
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
    }
}
