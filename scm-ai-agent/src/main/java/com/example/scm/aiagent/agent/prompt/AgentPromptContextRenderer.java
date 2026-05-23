package com.example.scm.aiagent.agent.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Agent prompt 渲染器。
 *
 * <p>把结构化上下文渲染为最终模型输入，保持分区清晰并避免泄露敏感信息。</p>
 */
@Component
public class AgentPromptContextRenderer {

    private final ObjectMapper objectMapper;

    public AgentPromptContextRenderer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String render(AgentPromptContext context) {
        StringBuilder builder = new StringBuilder();
        builder.append("你是 SCM/WMS 企业级 AI Agent。\n");
        builder.append("请基于下方已治理的上下文，直接输出最终中文回答。\n");
        builder.append("回答要求：优先使用实时业务数据回答事实查询；使用知识库片段解释规则、口径、字段含义或流程背景；如果没有召回知识片段，不要编造；如果工具失败，保留真实失败原因语义；不要输出 JSON 或调试信息。\n\n");
        if (context.getSections() != null) {
            for (AgentPromptSection section : context.getSections()) {
                if (!section.isIncluded()) {
                    continue;
                }
                builder.append("## ").append(titleOf(section)).append("\n");
                if (section.getContent() != null) {
                    builder.append(section.getContent()).append("\n");
                }
                if (section.getStructuredData() != null && !section.getStructuredData().isEmpty()) {
                    builder.append(toJson(section.getStructuredData())).append("\n");
                }
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    private String titleOf(AgentPromptSection section) {
        if (section.getTitle() != null && !section.getTitle().isBlank()) {
            return section.getTitle();
        }
        return switch (section.getType()) {
            case USER_MESSAGE -> "用户问题";
            case RAG_CONTEXT -> "知识库片段";
            case TOOL_EXECUTION -> "工具执行结果";
            case ORCHESTRATION_STEPS -> "编排步骤摘要";
            case SYSTEM_INSTRUCTIONS -> "系统指令";
            case SAFETY_CONSTRAINTS -> "安全约束";
        };
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize agent prompt context", ex);
        }
    }
}
