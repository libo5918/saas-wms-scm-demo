package com.example.scm.javaagent.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentConfigTest {

    @Test
    void parseShouldUseSafeDefaults() {
        AgentConfig config = AgentConfig.parse("");

        assertTrue(config.enabled());
        assertEquals("com.example.scm.aiagent", config.includePackage());
        assertEquals("com.example.scm.javaagent", config.excludePackage());
        assertTrue(config.traceTool());
        assertTrue(config.traceWorkflow());
        assertTrue(config.traceMultiAgent());
        assertFalse(config.asmPrint());
        assertEquals(0L, config.slowThresholdMs());
    }

    @Test
    void parseShouldSupportBooleanAndLong() {
        AgentConfig config = AgentConfig.parse("enabled=false,traceTool=no,traceWorkflow=1,asmPrint=on,slowThresholdMs=200");

        assertFalse(config.enabled());
        assertFalse(config.traceTool());
        assertTrue(config.traceWorkflow());
        assertTrue(config.asmPrint());
        assertEquals(200L, config.slowThresholdMs());
    }

    @Test
    void parseShouldFallbackForInvalidBooleanAndLong() {
        AgentConfig config = AgentConfig.parse("enabled=maybe,slowThresholdMs=bad");

        assertTrue(config.enabled());
        assertEquals(0L, config.slowThresholdMs());
    }

    @Test
    void parseShouldSupportPipeAndCommaListValues() {
        AgentConfig config = AgentConfig.parse(
                "includeClasses=ToolInvocationService|AgentWorkflowEngine,"
                        + "excludeClasses=InternalService,"
                        + "includeMethods=invoke,execute,chat,"
                        + "excludeMethods=toString|hashCode");

        assertTrue(config.includeClasses().contains("ToolInvocationService"));
        assertTrue(config.includeClasses().contains("AgentWorkflowEngine"));
        assertTrue(config.excludeClasses().contains("InternalService"));
        assertTrue(config.includeMethods().contains("invoke"));
        assertTrue(config.includeMethods().contains("execute"));
        assertTrue(config.includeMethods().contains("chat"));
        assertTrue(config.excludeMethods().contains("toString"));
        assertTrue(config.excludeMethods().contains("hashCode"));
    }

    @Test
    void shouldTraceClassShouldRespectIncludeAndExcludePackage() {
        AgentConfig config = AgentConfig.parse("include=com.example.scm.aiagent,exclude=com.example.scm.aiagent.internal");

        assertTrue(config.shouldTraceClass("com.example.scm.aiagent.tool.service.ToolInvocationService"));
        assertFalse(config.shouldTraceClass("com.example.scm.mdm.MaterialService"));
        assertFalse(config.shouldTraceClass("com.example.scm.aiagent.internal.SecretService"));
    }

    @Test
    void shouldTraceKnownAiAgentClassShouldRespectClassWhiteAndBlackList() {
        AgentConfig config = AgentConfig.parse(
                "includeClasses=ToolInvocationService|MultiAgentCoordinatorService,"
                        + "excludeClasses=MultiAgentCoordinatorService");

        assertTrue(config.shouldTraceKnownAiAgentClass(
                "com.example.scm.aiagent.tool.service.ToolInvocationService"));
        assertFalse(config.shouldTraceKnownAiAgentClass(
                "com.example.scm.aiagent.multiagent.service.MultiAgentCoordinatorService"));
        assertFalse(config.shouldTraceKnownAiAgentClass(
                "com.example.scm.aiagent.workflow.engine.AgentWorkflowEngine"));
    }

    @Test
    void shouldTraceKnownAiAgentClassShouldKeepDefaultCompatibility() {
        AgentConfig config = AgentConfig.parse("");

        assertTrue(config.shouldTraceKnownAiAgentClass(
                "com.example.scm.aiagent.tool.service.ToolInvocationService"));
        assertTrue(config.shouldTraceKnownAiAgentClass(
                "com.example.scm.aiagent.workflow.engine.AgentWorkflowEngine"));
        assertTrue(config.shouldTraceKnownAiAgentClass(
                "com.example.scm.aiagent.multiagent.service.MultiAgentCoordinatorService"));
        assertTrue(config.shouldTraceKnownAiAgentClass(
                "com.example.scm.aiagent.web.AgentChatController"));
    }

    @Test
    void shouldTraceMethodShouldRespectWhiteAndBlackList() {
        AgentConfig config = AgentConfig.parse("includeMethods=invoke|execute,excludeMethods=execute");

        assertTrue(config.shouldTraceMethod("invoke"));
        assertFalse(config.shouldTraceMethod("execute"));
        assertFalse(config.shouldTraceMethod("chat"));
    }
}
