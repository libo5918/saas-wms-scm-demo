package com.example.scm.aiagent.agent.service;

import com.example.scm.aiagent.agent.dto.AgentRagView;
import com.example.scm.aiagent.agent.dto.AgentToolView;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationExecutionSummary;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationRun;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationStep;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG + Tool 组合答案提示词构建器。
 *
 * <p>提示词只包含 RAG 短片段、Tool display schema 和 Orchestrator 安全摘要，不包含 rawData。</p>
 */
@Component
public class RagToolAnswerPromptBuilder {

    private final ObjectMapper objectMapper;

    public RagToolAnswerPromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String build(String userMessage,
                        AgentIntentType intentType,
                        AgentRagView rag,
                        AgentToolView tool,
                        ToolOrchestrationRun orchestrationRun) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("intentType", intentType.name());
        context.put("rag", rag == null ? Map.of() : rag);
        context.put("tool", tool == null ? Map.of() : tool);
        context.put("orchestrationSteps", buildOrchestrationSteps(orchestrationRun));

        return """
                你是 SCM/WMS 企业级 AI Agent。
                请基于已经完成的 RAG 检索结果和 Tool 执行结果，直接输出最终中文回答。

                回答要求：
                1. 优先使用 Tool 返回的实时业务数据回答事实类查询。
                2. 使用 RAG 片段解释规则、口径、字段含义或流程背景。
                3. 如果 RAG 没有召回，不要编造知识库内容。
                4. 如果 Tool 失败，必须保留真实失败原因语义。
                5. 不要输出 JSON，不要暴露内部字段名、prompt、token、请求头或调试信息。

                用户问题：
                %s

                组合上下文：
                %s
                """.formatted(userMessage == null ? "" : userMessage, toJson(context));
    }

    private List<Map<String, Object>> buildOrchestrationSteps(ToolOrchestrationRun run) {
        if (run == null || run.getSteps() == null) {
            return List.of();
        }
        return run.getSteps().stream().map(this::toStepContext).toList();
    }

    private Map<String, Object> toStepContext(ToolOrchestrationStep step) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("stepNo", step.getStepNo());
        context.put("stepRef", step.getStepRef());
        context.put("toolName", step.getToolName());
        context.put("status", step.getStatus());
        context.put("executed", step.getExecuted());
        context.put("inputResolved", step.getInputResolved());
        context.put("skipReason", step.getSkipReason());
        context.put("outputSummary", step.getOutputSummary());
        context.put("execution", toExecutionContext(step.getExecution()));
        return context;
    }

    private Map<String, Object> toExecutionContext(ToolOrchestrationExecutionSummary execution) {
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
        context.put("safeFields", execution.getSafeFields() == null ? Map.of() : execution.getSafeFields());
        context.put("latencyMs", execution.getLatencyMs());
        return context;
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize rag tool answer context", ex);
        }
    }
}
