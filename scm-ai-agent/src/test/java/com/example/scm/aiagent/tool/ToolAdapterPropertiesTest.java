package com.example.scm.aiagent.tool;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.toolcalling.orchestrator.ToolOrchestrationPlanMode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolAdapterPropertiesTest {

    @Test
    void shouldBindHttpAdapterAndPlannerProperties() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("ai.agent.tools.adapter-mode", "http")
                .withProperty("ai.agent.tools.http.inventory-base-url", "http://localhost:18084")
                .withProperty("ai.agent.tools.http.mdm-base-url", "http://localhost:18082")
                .withProperty("ai.agent.tools.http.sales-base-url", "http://localhost:18085")
                .withProperty("ai.agent.tools.http.purchase-base-url", "http://localhost:18083")
                .withProperty("ai.agent.tools.http.connect-timeout-ms", "2000")
                .withProperty("ai.agent.tools.http.read-timeout-ms", "4000")
                .withProperty("ai.agent.tools.audit.mode", "in-memory")
                .withProperty("ai.agent.tools.audit.max-records", "200")
                .withProperty("ai.agent.tools.access-control.strict-enabled", "true")
                .withProperty("ai.agent.tools.access-control.default-allow-read-only", "false")
                .withProperty("ai.agent.tools.access-control.admin-roles[0]", "ROLE_SUPER_ADMIN")
                .withProperty("ai.agent.tools.runtime.timeout-ms", "2500")
                .withProperty("ai.agent.tools.runtime.retry-enabled", "true")
                .withProperty("ai.agent.tools.runtime.max-retries", "2")
                .withProperty("ai.agent.tools.runtime.circuit-breaker-enabled", "true")
                .withProperty("ai.agent.tools.runtime.failure-threshold", "3")
                .withProperty("ai.agent.tools.runtime.open-duration-ms", "60000")
                .withProperty("ai.agent.tool-calling.planner-mode", "spring-ai")
                .withProperty("ai.agent.tool-calling.answer-mode", "spring-ai")
                .withProperty("ai.agent.tool-calling.spring-ai-planner.enabled", "true")
                .withProperty("ai.agent.tool-calling.spring-ai-planner.fallback-to-mock", "false")
                .withProperty("ai.agent.tool-calling.spring-ai-planner.max-retries", "2")
                .withProperty("ai.agent.tool-calling.spring-ai-planner.task-type", "tool_calling")
                .withProperty("ai.agent.tool-calling.spring-ai-answer.enabled", "true")
                .withProperty("ai.agent.tool-calling.spring-ai-answer.fallback-to-template", "true")
                .withProperty("ai.agent.tool-calling.spring-ai-answer.max-retries", "3")
                .withProperty("ai.agent.tool-calling.spring-ai-answer.task-type", "tool_calling_answer")
                .withProperty("ai.agent.tool-calling.orchestrator.enabled", "true")
                .withProperty("ai.agent.tool-calling.orchestrator.record-runs", "true")
                .withProperty("ai.agent.tool-calling.orchestrator.max-records", "120")
                .withProperty("ai.agent.tool-calling.orchestrator.plan-mode", "multi-step-dry-run")
                .withProperty("ai.agent.tool-calling.orchestrator.max-steps", "3")
                .withProperty("ai.agent.tool-calling.orchestrator.multi-step-enabled", "true")
                .withProperty("ai.agent.tool-calling.orchestrator.dry-run-enabled", "true")
                .withProperty("ai.agent.tool-calling.orchestrator.controlled-execution-enabled", "true")
                .withProperty("ai.agent.tool-calling.orchestrator.max-executable-steps", "2")
                .withProperty("ai.agent.tool-calling.orchestrator.allow-second-step-read-only", "false")
                .withProperty("ai.agent.mcp.server.enabled", "true")
                .withProperty("ai.agent.mcp.server.transport", "http")
                .withProperty("ai.agent.mcp.server.endpoint", "/api/v1/ai/mcp/server")
                .withProperty("ai.agent.mcp.server.expose-tools", "true")
                .withProperty("ai.agent.multi-agent.enabled", "true")
                .withProperty("ai.agent.multi-agent.max-rounds", "4")
                .withProperty("ai.agent.multi-agent.max-agents", "6")
                .withProperty("ai.agent.multi-agent.max-tool-calls", "2")
                .withProperty("ai.agent.multi-agent.record-messages", "false")
                .withProperty("ai.agent.multi-agent.max-records", "80");

        AiAgentProperties properties = Binder.get(environment)
                .bind("ai.agent", Bindable.of(AiAgentProperties.class))
                .get();

        assertEquals("http", properties.getTools().getAdapterMode());
        assertEquals("http://localhost:18084", properties.getTools().getHttp().getInventoryBaseUrl());
        assertEquals("http://localhost:18082", properties.getTools().getHttp().getMdmBaseUrl());
        assertEquals("http://localhost:18085", properties.getTools().getHttp().getSalesBaseUrl());
        assertEquals("http://localhost:18083", properties.getTools().getHttp().getPurchaseBaseUrl());
        assertEquals(2000, properties.getTools().getHttp().getConnectTimeoutMs());
        assertEquals(4000, properties.getTools().getHttp().getReadTimeoutMs());
        assertEquals("in-memory", properties.getTools().getAudit().getMode());
        assertEquals(200, properties.getTools().getAudit().getMaxRecords());
        assertTrue(properties.getTools().getAccessControl().isStrictEnabled());
        assertFalse(properties.getTools().getAccessControl().isDefaultAllowReadOnly());
        assertEquals(List.of("ROLE_SUPER_ADMIN"), properties.getTools().getAccessControl().getAdminRoles());
        assertEquals(2500, properties.getTools().getRuntime().getTimeoutMs());
        assertTrue(properties.getTools().getRuntime().isRetryEnabled());
        assertEquals(2, properties.getTools().getRuntime().getMaxRetries());
        assertTrue(properties.getTools().getRuntime().isCircuitBreakerEnabled());
        assertEquals(3, properties.getTools().getRuntime().getFailureThreshold());
        assertEquals(60000, properties.getTools().getRuntime().getOpenDurationMs());
        assertEquals("spring-ai", properties.getToolCalling().getPlannerMode());
        assertEquals("spring-ai", properties.getToolCalling().getAnswerMode());
        assertTrue(properties.getToolCalling().getSpringAiPlanner().isEnabled());
        assertFalse(properties.getToolCalling().getSpringAiPlanner().isFallbackToMock());
        assertEquals(2, properties.getToolCalling().getSpringAiPlanner().getMaxRetries());
        assertEquals("tool_calling", properties.getToolCalling().getSpringAiPlanner().getTaskType());
        assertTrue(properties.getToolCalling().getSpringAiAnswer().isEnabled());
        assertTrue(properties.getToolCalling().getSpringAiAnswer().isFallbackToTemplate());
        assertEquals(3, properties.getToolCalling().getSpringAiAnswer().getMaxRetries());
        assertEquals("tool_calling_answer", properties.getToolCalling().getSpringAiAnswer().getTaskType());
        assertTrue(properties.getToolCalling().getOrchestrator().isEnabled());
        assertTrue(properties.getToolCalling().getOrchestrator().isRecordRuns());
        assertEquals(120, properties.getToolCalling().getOrchestrator().getMaxRecords());
        assertEquals(ToolOrchestrationPlanMode.MULTI_STEP_DRY_RUN, properties.getToolCalling().getOrchestrator().getPlanMode());
        assertEquals(3, properties.getToolCalling().getOrchestrator().getMaxSteps());
        assertTrue(properties.getToolCalling().getOrchestrator().isMultiStepEnabled());
        assertTrue(properties.getToolCalling().getOrchestrator().isDryRunEnabled());
        assertTrue(properties.getToolCalling().getOrchestrator().isControlledExecutionEnabled());
        assertEquals(2, properties.getToolCalling().getOrchestrator().getMaxExecutableSteps());
        assertFalse(properties.getToolCalling().getOrchestrator().isAllowSecondStepReadOnly());
        assertTrue(properties.getMcp().getServer().isEnabled());
        assertEquals("http", properties.getMcp().getServer().getTransport());
        assertEquals("/api/v1/ai/mcp/server", properties.getMcp().getServer().getEndpoint());
        assertTrue(properties.getMcp().getServer().isExposeTools());
        assertTrue(properties.getMultiAgent().isEnabled());
        assertEquals(4, properties.getMultiAgent().getMaxRounds());
        assertEquals(6, properties.getMultiAgent().getMaxAgents());
        assertEquals(2, properties.getMultiAgent().getMaxToolCalls());
        assertFalse(properties.getMultiAgent().isRecordMessages());
        assertEquals(80, properties.getMultiAgent().getMaxRecords());
    }

    @Test
    void shouldDefaultAuditModeToInMemory() {
        AiAgentProperties properties = Binder.get(new MockEnvironment())
                .bind("ai.agent", Bindable.of(AiAgentProperties.class))
                .orElseGet(AiAgentProperties::new);

        assertEquals("in-memory", properties.getTools().getAudit().getMode());
        assertEquals(500, properties.getTools().getAudit().getMaxRecords());
        assertFalse(properties.getTools().getAccessControl().isStrictEnabled());
        assertTrue(properties.getTools().getAccessControl().isDefaultAllowReadOnly());
        assertEquals(List.of("ROLE_ADMIN"), properties.getTools().getAccessControl().getAdminRoles());
        assertEquals(5000, properties.getTools().getRuntime().getTimeoutMs());
        assertTrue(properties.getTools().getRuntime().isRetryEnabled());
        assertEquals(1, properties.getTools().getRuntime().getMaxRetries());
        assertFalse(properties.getToolCalling().getOrchestrator().isEnabled());
        assertTrue(properties.getToolCalling().getOrchestrator().isRecordRuns());
        assertEquals(100, properties.getToolCalling().getOrchestrator().getMaxRecords());
        assertEquals(ToolOrchestrationPlanMode.SINGLE_STEP, properties.getToolCalling().getOrchestrator().getPlanMode());
        assertEquals(1, properties.getToolCalling().getOrchestrator().getMaxSteps());
        assertFalse(properties.getToolCalling().getOrchestrator().isMultiStepEnabled());
        assertFalse(properties.getToolCalling().getOrchestrator().isDryRunEnabled());
        assertFalse(properties.getToolCalling().getOrchestrator().isControlledExecutionEnabled());
        assertEquals(1, properties.getToolCalling().getOrchestrator().getMaxExecutableSteps());
        assertTrue(properties.getToolCalling().getOrchestrator().isAllowSecondStepReadOnly());
        assertFalse(properties.getMcp().getServer().isEnabled());
        assertEquals("http", properties.getMcp().getServer().getTransport());
        assertEquals("/api/v1/ai/mcp/server", properties.getMcp().getServer().getEndpoint());
        assertTrue(properties.getMcp().getServer().isExposeTools());
        assertFalse(properties.getMultiAgent().isEnabled());
        assertEquals(3, properties.getMultiAgent().getMaxRounds());
        assertEquals(5, properties.getMultiAgent().getMaxAgents());
        assertEquals(3, properties.getMultiAgent().getMaxToolCalls());
        assertTrue(properties.getMultiAgent().isRecordMessages());
        assertEquals(100, properties.getMultiAgent().getMaxRecords());
    }

    @Test
    void shouldBindMysqlAuditMode() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("ai.agent.tools.audit.mode", "mysql")
                .withProperty("ai.agent.tools.audit.max-records", "300");

        AiAgentProperties properties = Binder.get(environment)
                .bind("ai.agent", Bindable.of(AiAgentProperties.class))
                .get();

        assertEquals("mysql", properties.getTools().getAudit().getMode());
        assertEquals(300, properties.getTools().getAudit().getMaxRecords());
    }
}
