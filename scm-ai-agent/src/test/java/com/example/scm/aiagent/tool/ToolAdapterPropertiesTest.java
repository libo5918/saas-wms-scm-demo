package com.example.scm.aiagent.tool;

import com.example.scm.aiagent.config.AiAgentProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToolAdapterPropertiesTest {

    @Test
    void shouldBindHttpAdapterProperties() {
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
                .withProperty("ai.agent.tool-calling.planner-mode", "spring-ai");

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
        assertEquals("spring-ai", properties.getToolCalling().getPlannerMode());
    }
}
