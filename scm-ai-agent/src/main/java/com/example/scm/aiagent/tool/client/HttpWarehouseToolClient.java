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

import java.util.HashMap;
import java.util.Map;

/**
 * 仓库 Tool 的 HTTP 客户端。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ai.agent.tools", name = "adapter-mode", havingValue = "http")
public class HttpWarehouseToolClient extends AbstractToolClientSupport implements WarehouseToolClient {

    private static final ParameterizedTypeReference<Result<Map<String, Object>>> RESULT_TYPE = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;
    private final String mdmBaseUrl;

    public HttpWarehouseToolClient(RestClient.Builder restClientBuilder, AiAgentProperties properties) {
        AiAgentProperties.HttpToolClientProperties http = properties.getTools().getHttp();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(http.getConnectTimeoutMs());
        requestFactory.setReadTimeout(http.getReadTimeoutMs());
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
        this.mdmBaseUrl = http.getMdmBaseUrl();
    }

    @Override
    public Map<String, Object> getWarehouse(ToolRequest request) {
        long startedAt = System.nanoTime();
        Long warehouseId = longParam(request.getParameters(), "warehouseId", null);
        if (warehouseId == null) {
            throw new ToolClientException("Warehouse tool requires warehouseId");
        }
        try {
            Result<Map<String, Object>> result = restClient.get()
                    .uri(mdmBaseUrl + "/api/v1/warehouses/{warehouseId}", warehouseId)
                    .headers(headers -> headers.addAll(identityHeaders(request)))
                    .retrieve()
                    .body(RESULT_TYPE);
            if (result == null) {
                throw new ToolClientException("Warehouse service returned empty response");
            }
            if (!result.success()) {
                throw new ToolClientException("Warehouse service failed: " + result.message());
            }
            log.info("Warehouse tool HTTP call success, tenantId={}, userId={}, runId={}, warehouseId={}, latencyMs={}",
                    request.getContext().tenantId(), request.getContext().userId(), request.getRunId(),
                    warehouseId, elapsedMs(startedAt));
            return withAdapterMode(result.data());
        } catch (ToolClientException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ToolClientException("Warehouse service call failed: " + ex.getMessage(), ex);
        }
    }

    private Map<String, Object> withAdapterMode(Map<String, Object> data) {
        HashMap<String, Object> result = new HashMap<>(data == null ? Map.of() : data);
        result.put("adapterMode", "http");
        return result;
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
