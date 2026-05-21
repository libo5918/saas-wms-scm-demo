package com.example.scm.aiagent.toolcalling.planning;

import com.example.scm.aiagent.toolcalling.model.ToolCallingPlan;
import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 解析真实模型返回的 Tool Plan JSON。
 */
@Component
public class ToolPlanParser {

    private final ObjectMapper objectMapper;

    public ToolPlanParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将模型返回的 JSON 文本解析为 ToolCallingPlan。
     */
    public ToolCallingPlan parse(String content, String plannerMode) {
        String json = extractJson(content);
        try {
            JsonNode root = objectMapper.readTree(json);
            String toolName = textOf(root, "toolName");
            if (!StringUtils.hasText(toolName)) {
                toolName = textOf(root, "selectedTool");
            }
            if (!StringUtils.hasText(toolName)) {
                throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(),
                        "Spring AI planner result missing toolName");
            }

            JsonNode argumentsNode = root.path("arguments");
            if (argumentsNode.isMissingNode() || argumentsNode.isNull()) {
                argumentsNode = root.path("toolArguments");
            }
            Map<String, Object> arguments = argumentsNode.isObject()
                    ? objectMapper.convertValue(argumentsNode, new TypeReference<>() {})
                    : Map.of();

            String reason = textOf(root, "reason");
            if (!StringUtils.hasText(reason)) {
                reason = textOf(root, "planningNote");
            }

            return ToolCallingPlan.builder()
                    .plannerMode(plannerMode)
                    .planningSource("spring-ai")
                    .fallbackUsed(false)
                    .selectedTool(toolName)
                    .toolArguments(arguments)
                    .reason(reason)
                    .build();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(),
                    "Failed to parse Spring AI planner JSON: " + ex.getMessage());
        }
    }

    private String extractJson(String content) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(),
                    "Spring AI planner returned empty content");
        }
        String cleaned = content.trim();
        if (cleaned.startsWith("```") && cleaned.endsWith("```")) {
            cleaned = cleaned.replaceFirst("^```[a-zA-Z]*", "").replaceFirst("```$", "").trim();
        }
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');
        if (firstBrace < 0 || lastBrace < 0 || firstBrace >= lastBrace) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(),
                    "Spring AI planner did not return a valid JSON object");
        }
        return cleaned.substring(firstBrace, lastBrace + 1);
    }

    private String textOf(JsonNode root, String fieldName) {
        JsonNode node = root.path(fieldName);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return StringUtils.hasText(text) ? text : null;
    }
}
