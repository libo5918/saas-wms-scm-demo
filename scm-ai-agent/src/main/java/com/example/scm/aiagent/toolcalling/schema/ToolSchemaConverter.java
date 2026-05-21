package com.example.scm.aiagent.toolcalling.schema;

import com.example.scm.aiagent.tool.model.ToolDefinition;
import com.example.scm.aiagent.toolcalling.model.SpringAiToolDescriptor;
import com.example.scm.aiagent.toolcalling.model.SpringAiToolInputSchema;
import com.example.scm.aiagent.toolcalling.model.SpringAiToolParameterSchema;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ToolDefinition 到 Spring AI Tool schema 的转换器。
 */
@Component
public class ToolSchemaConverter {

    /**
     * 将当前项目的 ToolDefinition 转换为模型可消费的工具 schema。
     */
    public SpringAiToolDescriptor convert(ToolDefinition definition) {
        List<String> requiredParameters = definition.getRequiredParameters() == null ? List.of() : definition.getRequiredParameters();
        List<List<String>> oneOfRequiredGroups = definition.getOneOfRequiredGroups() == null ? List.of() : definition.getOneOfRequiredGroups();
        Set<String> requiredSet = Set.copyOf(requiredParameters);
        Map<String, SpringAiToolParameterSchema> properties = new LinkedHashMap<>();
        Map<String, String> parameters = definition.getParameters() == null ? Map.of() : definition.getParameters();
        parameters.forEach((name, description) -> properties.put(name, SpringAiToolParameterSchema.builder()
                .type(inferType(name))
                .description(description)
                .required(requiredSet.contains(name))
                .build()));
        return SpringAiToolDescriptor.builder()
                .toolName(definition.getName())
                .description(definition.getDescription())
                .domain(definition.getDomain())
                .category(definition.getCategory())
                .readOnly(definition.isReadOnly())
                .routeTags(definition.getRouteTags() == null ? List.of() : definition.getRouteTags())
                .inputSchema(SpringAiToolInputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .required(requiredParameters)
                        .oneOfRequiredGroups(oneOfRequiredGroups)
                        .build())
                .build();
    }

    private String inferType(String parameterName) {
        String lowerName = parameterName == null ? "" : parameterName.toLowerCase();
        if (lowerName.endsWith("id")) {
            return "integer";
        }
        if (lowerName.contains("qty") || lowerName.contains("count") || lowerName.contains("size")) {
            return "number";
        }
        if (lowerName.startsWith("is") || lowerName.startsWith("has")) {
            return "boolean";
        }
        return "string";
    }
}
