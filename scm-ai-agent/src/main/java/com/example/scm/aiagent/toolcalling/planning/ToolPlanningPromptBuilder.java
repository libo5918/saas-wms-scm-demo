package com.example.scm.aiagent.toolcalling.planning;

import com.example.scm.aiagent.toolcalling.model.SpringAiToolDescriptor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 构造真实模型进行 Tool 规划时使用的提示词。
 */
@Component
public class ToolPlanningPromptBuilder {

    private final ObjectMapper objectMapper;

    public ToolPlanningPromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 构造只允许模型返回 JSON Tool Plan 的规划提示词。
     */
    public String build(String userMessage, List<SpringAiToolDescriptor> schemas) {
        try {
            String schemaJson = objectMapper.writeValueAsString(schemas);
            return "你是 SCM/WMS 项目的工具规划器。" +
                    "你只能从候选工具中选择一个最合适的工具，不要回答业务结果。" +
                    "你必须只返回一个 JSON 对象，不要输出 Markdown 代码块、解释文本或额外前后缀。\n\n" +
                    "返回 JSON 结构如下：\n" +
                    "{\n" +
                    "  \"toolName\": \"工具名\",\n" +
                    "  \"arguments\": {\"参数名\": \"参数值\"},\n" +
                    "  \"reason\": \"可选的简短规划理由\"\n" +
                    "}\n\n" +
                    "约束：\n" +
                    "1. toolName 必须来自候选工具列表。\n" +
                    "2. arguments 只能包含该工具定义中的参数。\n" +
                    "3. 如果用户问题里已经给出编号、编码、单号等信息，优先直接复用。\n" +
                    "4. 如果参数无法从问题中判断，可以返回空 arguments，由服务端后续校验。\n" +
                    "5. 不要输出多个工具，不要输出自然语言答案。\n\n" +
                    "候选工具 schema 列表：\n" + schemaJson + "\n\n" +
                    "用户问题：\n" + userMessage;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize tool schema for planner prompt", ex);
        }
    }
}
