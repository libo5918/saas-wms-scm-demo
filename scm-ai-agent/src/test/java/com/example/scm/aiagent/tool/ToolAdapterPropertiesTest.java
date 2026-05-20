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
                .withProperty("ai.agent.tools.http.connect-timeout-ms", "2000")
                .withProperty("ai.agent.tools.http.read-timeout-ms", "4000");

        AiAgentProperties properties = Binder.get(environment)
                .bind("ai.agent", Bindable.of(AiAgentProperties.class))
                .get();

        assertEquals("http", properties.getTools().getAdapterMode());
        assertEquals("http://localhost:18084", properties.getTools().getHttp().getInventoryBaseUrl());
        assertEquals("http://localhost:18082", properties.getTools().getHttp().getMdmBaseUrl());
        assertEquals(2000, properties.getTools().getHttp().getConnectTimeoutMs());
        assertEquals(4000, properties.getTools().getHttp().getReadTimeoutMs());
    }
}
