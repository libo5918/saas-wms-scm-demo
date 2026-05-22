package com.example.scm.aiagent.tool;

import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.tool.model.ToolCandidateFilterRequest;
import com.example.scm.aiagent.tool.model.ToolCandidateFilterResult;
import com.example.scm.aiagent.tool.model.ToolDefinition;
import com.example.scm.aiagent.tool.service.ToolCandidateFilterService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolCandidateFilterServiceTest {

    private final ToolCandidateFilterService service = new ToolCandidateFilterService();
    private final AgentRequestContext context = new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));

    @Test
    void shouldFilterByDomain() {
        ToolCandidateFilterResult result = service.filter(definitions(), ToolCandidateFilterRequest.builder()
                .requestedDomain("inventory")
                .readOnlyOnly(true)
                .build(), context, "run-filter-domain");

        assertFalse(result.isFallbackUsed());
        assertEquals(1, result.getCandidates().size());
        assertEquals("inventory.getBalance", result.getCandidates().get(0).getName());
        assertEquals("inventory", result.getResolvedDomain());
    }

    @Test
    void shouldFilterByRouteTags() {
        ToolCandidateFilterResult result = service.filter(definitions(), ToolCandidateFilterRequest.builder()
                .routeTags(List.of("order", "purchase"))
                .readOnlyOnly(true)
                .build(), context, "run-filter-tag");

        assertFalse(result.isFallbackUsed());
        assertEquals(1, result.getCandidates().size());
        assertEquals("purchase.getOrder", result.getCandidates().get(0).getName());
        assertEquals(List.of("order", "purchase"), result.getResolvedRouteTags());
    }

    @Test
    void shouldFallbackToAllReadonlyToolsWhenFilterEmpty() {
        ToolCandidateFilterResult result = service.filter(definitions(), ToolCandidateFilterRequest.builder()
                .requestedDomain("finance")
                .readOnlyOnly(true)
                .build(), context, "run-filter-empty");

        assertTrue(result.isFallbackUsed());
        assertEquals(3, result.getCandidates().size());
        assertTrue(result.getCandidates().stream().allMatch(ToolDefinition::isReadOnly));
    }

    @Test
    void shouldInferDomainFromMessageKeywords() {
        assertEquals("inventory", service.inferDomain("帮我查库存余额"));
        assertEquals("mdm", service.inferDomain("查一下物料 MAT-001"));
        assertEquals("sales", service.inferDomain("查询销售订单 SO-001"));
        assertEquals("purchase", service.inferDomain("查询采购订单 PO-001"));
    }

    private List<ToolDefinition> definitions() {
        return List.of(
                ToolDefinition.builder()
                        .name("mdm.getMaterial")
                        .domain("mdm")
                        .category("master-data")
                        .routeTags(List.of("mdm", "material"))
                        .readOnly(true)
                        .build(),
                ToolDefinition.builder()
                        .name("inventory.getBalance")
                        .domain("inventory")
                        .category("stock")
                        .routeTags(List.of("inventory", "balance"))
                        .readOnly(true)
                        .build(),
                ToolDefinition.builder()
                        .name("purchase.getOrder")
                        .domain("purchase")
                        .category("order")
                        .routeTags(List.of("purchase", "order"))
                        .readOnly(true)
                        .build(),
                ToolDefinition.builder()
                        .name("sales.createOrder")
                        .domain("sales")
                        .category("order")
                        .routeTags(List.of("sales", "order"))
                        .readOnly(false)
                        .build()
        );
    }
}
