package com.example.scm.aiagent.tool.client;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.tool.model.ToolRequest;
import com.example.scm.common.core.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 库存 Tool 的 HTTP 客户端。
 *
 * <p>仅在 `ai.agent.tools.adapter-mode=http` 时启用，调用 `scm-inventory` 的只读库存余额接口。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ai.agent.tools", name = "adapter-mode", havingValue = "http")
public class HttpInventoryToolClient extends AbstractToolClientSupport implements InventoryToolClient {

    private static final ParameterizedTypeReference<Result<Map<String, Object>>> RESULT_TYPE = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;
    private final String inventoryBaseUrl;

    public HttpInventoryToolClient(RestClient.Builder restClientBuilder, AiAgentProperties properties) {
        AiAgentProperties.HttpToolClientProperties http = properties.getTools().getHttp();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(http.getConnectTimeoutMs());
        requestFactory.setReadTimeout(http.getReadTimeoutMs());
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
        this.inventoryBaseUrl = properties.getTools().getHttp().getInventoryBaseUrl();
    }

    @Override
    public Map<String, Object> getBalance(ToolRequest request) {
        long startedAt = System.nanoTime();
        Long materialId = longParam(request.getParameters(), "materialId", 1001L);
        Long warehouseId = longParam(request.getParameters(), "warehouseId", 1L);
        Long locationId = longParam(request.getParameters(), "locationId", 1L);
        try {
            Result<Map<String, Object>> result = restClient.get()
                    .uri(inventoryBaseUrl + "/api/v1/inventory/balances?materialId={materialId}&warehouseId={warehouseId}&locationId={locationId}",
                            materialId, warehouseId, locationId)
                    .headers(headers -> headers.addAll(identityHeaders(request)))
                    .retrieve()
                    .body(RESULT_TYPE);
            if (result == null) {
                throw new ToolClientException("Inventory service returned empty response");
            }
            if (!result.success()) {
                throw new ToolClientException("Inventory service failed: " + result.message());
            }
            log.info("Inventory tool HTTP call success, tenantId={}, userId={}, runId={}, materialId={}, warehouseId={}, locationId={}, latencyMs={}",
                    request.getContext().tenantId(), request.getContext().userId(), request.getRunId(),
                    materialId, warehouseId, locationId, elapsedMs(startedAt));
            Map<String, Object> data = result.data() == null ? Map.of() : result.data();
            return withAdapterMode(data);
        } catch (ToolClientException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ToolClientException("Inventory service call failed: " + ex.getMessage(), ex);
        }
    }

    private Map<String, Object> withAdapterMode(Map<String, Object> data) {
        java.util.HashMap<String, Object> result = new java.util.HashMap<>(data);
        result.put("adapterMode", "http");
        return result;
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
