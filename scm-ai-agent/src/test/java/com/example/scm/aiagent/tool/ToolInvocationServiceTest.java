package com.example.scm.aiagent.tool;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.tool.client.MockInventoryToolClient;
import com.example.scm.aiagent.tool.client.MockMdmToolClient;
import com.example.scm.aiagent.tool.client.MockPurchaseToolClient;
import com.example.scm.aiagent.tool.client.MockSalesToolClient;
import com.example.scm.aiagent.tool.client.MockWarehouseToolClient;
import com.example.scm.aiagent.tool.client.ToolClientException;
import com.example.scm.aiagent.tool.dto.ToolInvokeRequest;
import com.example.scm.aiagent.tool.dto.ToolResponse;
import com.example.scm.aiagent.tool.executor.InventoryBalanceToolExecutor;
import com.example.scm.aiagent.tool.executor.MaterialInfoToolExecutor;
import com.example.scm.aiagent.tool.executor.PurchaseOrderToolExecutor;
import com.example.scm.aiagent.tool.executor.SalesOrderToolExecutor;
import com.example.scm.aiagent.tool.executor.WarehouseInfoToolExecutor;
import com.example.scm.aiagent.tool.service.ToolInvocationAuditService;
import com.example.scm.aiagent.tool.service.ToolInvocationService;
import com.example.scm.aiagent.tool.service.ToolRegistry;
import com.example.scm.aiagent.tool.store.InMemoryToolInvocationAuditStore;
import com.example.scm.aiagent.tool.spi.ToolExecutor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolInvocationServiceTest {

    @Test
    void shouldRegisterAndInvokeMockToolWithTenantContext() {
        ToolInvocationService service = createService();
        AgentRequestContext context = new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));

        ToolInvokeRequest request = new ToolInvokeRequest();
        request.setToolName("inventory.getBalance");
        request.setRunId("run-tools-1");
        request.setParameters(Map.of("materialId", 1001L, "warehouseId", 1L));

        ToolResponse response = service.invoke(request, context);

        assertTrue(response.isSuccess());
        assertEquals("inventory.getBalance", response.getToolName());
        assertEquals("run-tools-1", response.getRunId());
        assertTrue(response.getData() instanceof Map);
        assertEquals(1L, ((Map<?, ?>) response.getData()).get("tenantId"));
        assertEquals(1, service.listInvocations(context, "inventory.getBalance", "run-tools-1", 10).getCount());
    }

    @Test
    void shouldReturnFailureWhenToolNotFound() {
        ToolInvocationService service = createService();
        AgentRequestContext context = new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));
        ToolInvokeRequest request = new ToolInvokeRequest();
        request.setToolName("unknown.tool");

        ToolResponse response = service.invoke(request, context);

        assertFalse(response.isSuccess());
        assertEquals("unknown.tool", response.getToolName());
        assertEquals("404", response.getErrorCode());
        assertEquals(1, service.listInvocations(context, "unknown.tool", response.getRunId(), 10).getCount());
    }

    @Test
    void shouldListToolDefinitions() {
        ToolInvocationService service = createService();
        AgentRequestContext context = new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));

        var response = service.listTools(context);

        assertEquals(5, response.getToolCount());
        assertTrue(response.getTools().stream().allMatch(tool -> tool.isReadOnly()));
        assertTrue(response.getTools().stream().anyMatch(tool -> "mdm.getMaterial".equals(tool.getName())));
    }

    @Test
    void shouldReturnFailureWhenToolClientThrowsException() {
        ToolInvocationService service = new ToolInvocationService(new ToolRegistry(List.of(
                new InventoryBalanceToolExecutor(request -> {
                    throw new ToolClientException("Inventory service call failed");
                })
        )), new ToolInvocationAuditService(new InMemoryToolInvocationAuditStore(new AiAgentProperties())), new AiAgentProperties());
        AgentRequestContext context = new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));
        ToolInvokeRequest request = new ToolInvokeRequest();
        request.setToolName("inventory.getBalance");

        ToolResponse response = service.invoke(request, context);

        assertFalse(response.isSuccess());
        assertEquals("400", response.getErrorCode());
        assertEquals("Inventory service call failed", response.getErrorMessage());
        assertEquals(1, service.listInvocations(context, "inventory.getBalance", response.getRunId(), 10).getCount());
    }

    private ToolInvocationService createService() {
        AiAgentProperties properties = new AiAgentProperties();
        ToolInvocationAuditService auditService = new ToolInvocationAuditService(new InMemoryToolInvocationAuditStore(properties));
        List<ToolExecutor> executors = List.of(
                new InventoryBalanceToolExecutor(new MockInventoryToolClient()),
                new MaterialInfoToolExecutor(new MockMdmToolClient()),
                new SalesOrderToolExecutor(new MockSalesToolClient()),
                new PurchaseOrderToolExecutor(new MockPurchaseToolClient()),
                new WarehouseInfoToolExecutor(new MockWarehouseToolClient())
        );
        return new ToolInvocationService(new ToolRegistry(executors), auditService, properties);
    }
}
