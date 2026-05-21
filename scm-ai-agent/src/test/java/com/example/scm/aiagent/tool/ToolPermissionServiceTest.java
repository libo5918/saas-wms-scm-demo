package com.example.scm.aiagent.tool;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.tool.model.ToolDefinition;
import com.example.scm.aiagent.tool.service.ToolPermissionService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolPermissionServiceTest {

    @Test
    void shouldAllowReadOnlyToolWhenStrictModeDisabled() {
        AiAgentProperties properties = new AiAgentProperties();
        ToolPermissionService service = new ToolPermissionService(properties);

        var decision = service.authorize(definition(), new AgentRequestContext(1L, 10001L, "user", List.of()));

        assertTrue(decision.allowed());
    }

    @Test
    void shouldAllowWhenPermissionMatchesInStrictMode() {
        AiAgentProperties properties = strictProperties();
        ToolPermissionService service = new ToolPermissionService(properties);

        var decision = service.authorize(definition(),
                new AgentRequestContext(1L, 10001L, "user", List.of("ai.tool.inventory.read")));

        assertTrue(decision.allowed());
    }

    @Test
    void shouldDenyWhenPermissionMissingInStrictMode() {
        AiAgentProperties properties = strictProperties();
        ToolPermissionService service = new ToolPermissionService(properties);

        var decision = service.authorize(definition(),
                new AgentRequestContext(1L, 10001L, "user", List.of("ai.tool.sales.read")));

        assertFalse(decision.allowed());
    }

    private AiAgentProperties strictProperties() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getTools().getAccessControl().setStrictEnabled(true);
        properties.getTools().getAccessControl().setAdminRoles(List.of());
        return properties;
    }

    private ToolDefinition definition() {
        return ToolDefinition.builder()
                .name("inventory.getBalance")
                .domain("inventory")
                .category("query")
                .description("查询库存")
                .readOnly(true)
                .requiredPermissions(List.of("ai.tool.read", "ai.tool.inventory.read"))
                .requiredRoles(List.of())
                .tenantScoped(true)
                .userScoped(true)
                .routeTags(List.of("inventory", "read", "query"))
                .parameters(Map.of("materialId", "物料 ID"))
                .requiredParameters(List.of("materialId"))
                .oneOfRequiredGroups(List.of())
                .build();
    }
}
