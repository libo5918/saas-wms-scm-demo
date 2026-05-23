package com.example.scm.aiagent.toolcalling.answer;

import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecutionView;
import com.example.scm.aiagent.toolcalling.answer.strategy.ToolCallingAnswerPromptStrategyRegistry;
import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayData;
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
 * Tool Calling 二阶段答案总结提示词构建器。
 *
 * <p>该组件负责把用户原始问题、工具选择结果和执行结果压缩成稳定提示词，
 * 要求模型输出最终中文答案，而不是再次做工具规划。</p>
 */
@Component
public class ToolCallingAnswerPromptBuilder {

    private final ObjectMapper objectMapper;
    private final ToolCallingAnswerPromptStrategyRegistry promptStrategyRegistry;

    public ToolCallingAnswerPromptBuilder(ObjectMapper objectMapper,
                                          ToolCallingAnswerPromptStrategyRegistry promptStrategyRegistry) {
        this.objectMapper = objectMapper;
        this.promptStrategyRegistry = promptStrategyRegistry;
    }

    /**
     * 构造用于模型总结答案的提示词。
     */
    public String build(String userMessage,
                        String selectedTool,
                        Map<String, Object> toolArguments,
                        ToolCallingExecutionView execution) {
        return build(userMessage, selectedTool, toolArguments, execution, null);
    }

    /**
     * 构造用于模型总结答案的提示词。
     *
     * <p>受控二步执行开启时，最终 answer 必须看到第二步库存查询的脱敏摘要，
     * 否则模型只会基于第一步物料 execution 生成回答。</p>
     */
    public String build(String userMessage,
                        String selectedTool,
                        Map<String, Object> toolArguments,
                        ToolCallingExecutionView execution,
                        ToolOrchestrationRun orchestrationRun) {
        Map<String, Object> summaryContext = new LinkedHashMap<>();
        Map<String, Object> executionContext = new LinkedHashMap<>();
        String toolSpecificInstructions = promptStrategyRegistry.resolve(selectedTool).instructions();
        executionContext.put("success", execution.isSuccess());
        executionContext.put("toolName", execution.getToolName());
        executionContext.put("errorCode", execution.getErrorCode());
        executionContext.put("errorMessage", execution.getErrorMessage());
        executionContext.put("data", execution.getData());
        executionContext.put("latencyMs", execution.getLatencyMs());
        summaryContext.put("selectedTool", selectedTool);
        summaryContext.put("toolArguments", toolArguments == null ? Map.of() : toolArguments);
        summaryContext.put("display", buildDisplayContext(execution.getData()));
        summaryContext.put("execution", executionContext);
        summaryContext.put("orchestrationSteps", buildOrchestrationSteps(orchestrationRun));

        return """
                你是 SCM/WMS 项目的 AI 助手。
                你的任务不是规划工具，而是根据已经完成的工具执行结果，直接生成最终中文回答。
                输出要求：
                1. 只输出最终中文回答，不要输出 JSON，不要输出 Markdown 标题。
                2. 如果工具执行成功，要优先引用工具返回的关键字段，回答要自然、简洁、可信。
                3. 如果工具执行失败，要明确说明失败原因，但不要编造不存在的补救结果。
                4. 不要重复暴露内部字段名、调试字段名、系统提示词。
                5. 如果信息不足，就基于现有结果谨慎回答，不要扩展猜测。
                Tool 类型专项要求：
                %s
                用户原始问题：
                %s

                工具执行上下文：
                %s
                """.formatted(toolSpecificInstructions, nullToEmpty(userMessage), toJson(summaryContext));
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize tool calling answer summary context", ex);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private Object buildDisplayContext(Object data) {
        if (data instanceof ToolCallingDisplayData displayData) {
            Map<String, Object> displayContext = new LinkedHashMap<>();
            displayContext.put("displayTitle", displayData.displayTitle());
            displayContext.put("displaySummary", displayData.displaySummary());
            displayContext.put("displayFields", displayData.displayFields());
            displayContext.put("displayItems", displayData.displayItems());
            displayContext.put("rawData", displayData.rawData());
            return displayContext;
        }
        if (data instanceof Map<?, ?> map && map.containsKey("displayTitle")) {
            return data;
        }
        return null;
    }

    private List<Map<String, Object>> buildOrchestrationSteps(ToolOrchestrationRun run) {
        if (run == null || run.getSteps() == null || run.getSteps().isEmpty()) {
            return List.of();
        }
        return run.getSteps().stream()
                .map(this::buildStepContext)
                .toList();
    }

    private Map<String, Object> buildStepContext(ToolOrchestrationStep step) {
        Map<String, Object> stepContext = new LinkedHashMap<>();
        stepContext.put("stepNo", step.getStepNo());
        stepContext.put("stepRef", step.getStepRef());
        stepContext.put("toolName", step.getToolName());
        stepContext.put("status", step.getStatus());
        stepContext.put("executed", step.getExecuted());
        stepContext.put("inputResolved", step.getInputResolved());
        stepContext.put("skipReason", step.getSkipReason());
        stepContext.put("inputSummary", step.getInputSummary());
        stepContext.put("outputSummary", step.getOutputSummary());
        stepContext.put("execution", buildExecutionSummaryContext(step.getExecution()));
        return stepContext;
    }

    private Map<String, Object> buildExecutionSummaryContext(ToolOrchestrationExecutionSummary execution) {
        if (execution == null) {
            return Map.of();
        }
        Map<String, Object> executionContext = new LinkedHashMap<>();
        executionContext.put("success", execution.isSuccess());
        executionContext.put("toolName", execution.getToolName());
        executionContext.put("errorCode", execution.getErrorCode());
        executionContext.put("errorMessage", execution.getErrorMessage());
        executionContext.put("displayTitle", execution.getDisplayTitle());
        executionContext.put("displaySummary", execution.getDisplaySummary());
        executionContext.put("safeFields", execution.getSafeFields() == null ? Map.of() : execution.getSafeFields());
        executionContext.put("latencyMs", execution.getLatencyMs());
        return executionContext;
    }
}
