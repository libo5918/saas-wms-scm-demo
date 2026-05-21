package com.example.scm.aiagent.toolcalling;

import com.example.scm.aiagent.toolcalling.model.ToolCallingPlan;
import com.example.scm.aiagent.toolcalling.planning.ToolPlanParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToolPlanParserTest {

    private final ToolPlanParser parser = new ToolPlanParser(new ObjectMapper());

    @Test
    void shouldParsePlainJson() {
        ToolCallingPlan plan = parser.parse("""
                {"toolName":"mdm.getMaterial","arguments":{"materialCode":"MAT-001"},"reason":"material lookup"}
                """, "spring-ai");

        assertEquals("mdm.getMaterial", plan.selectedTool());
        assertEquals("MAT-001", plan.toolArguments().get("materialCode"));
        assertEquals("material lookup", plan.reason());
    }

    @Test
    void shouldParseMarkdownWrappedJson() {
        ToolCallingPlan plan = parser.parse("""
                ```json
                {"toolName":"sales.getOrder","arguments":{"orderNo":"SO-001"}}
                ```
                """, "spring-ai");

        assertEquals("sales.getOrder", plan.selectedTool());
        assertEquals("SO-001", plan.toolArguments().get("orderNo"));
    }
}
