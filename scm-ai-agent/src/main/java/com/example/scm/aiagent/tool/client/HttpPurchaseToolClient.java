package com.example.scm.aiagent.tool.client;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.tool.model.ToolRequest;
import com.example.scm.common.core.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * 采购 Tool 的 HTTP 客户端。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ai.agent.tools", name = "adapter-mode", havingValue = "http")
public class HttpPurchaseToolClient extends AbstractToolClientSupport implements PurchaseToolClient {

    private static final ParameterizedTypeReference<Result<Map<String, Object>>> RESULT_TYPE = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;
    private final String purchaseBaseUrl;

    public HttpPurchaseToolClient(RestClient.Builder restClientBuilder, AiAgentProperties properties) {
        AiAgentProperties.HttpToolClientProperties http = properties.getTools().getHttp();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(http.getConnectTimeoutMs());
        requestFactory.setReadTimeout(http.getReadTimeoutMs());
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
        this.purchaseBaseUrl = http.getPurchaseBaseUrl();
    }

    @Override
    public Map<String, Object> getOrder(ToolRequest request) {
        long startedAt = System.nanoTime();
        Long orderId = longParam(request.getParameters(), "orderId", null);
        String orderNo = stringParam(request.getParameters(), "orderNo", null);
        try {
            Result<Map<String, Object>> result;
            if (orderId != null) {
                result = restClient.get()
                        .uri(purchaseBaseUrl + "/api/v1/purchase-orders/{id}", orderId)
                        .headers(headers -> headers.addAll(identityHeaders(request)))
                        .retrieve()
                        .body(RESULT_TYPE);
            } else if (StringUtils.hasText(orderNo)) {
                result = restClient.get()
                        .uri(purchaseBaseUrl + "/api/v1/purchase-orders/by-order-no?orderNo={orderNo}", orderNo)
                        .headers(headers -> headers.addAll(identityHeaders(request)))
                        .retrieve()
                        .body(RESULT_TYPE);
            } else {
                throw new ToolClientException("Purchase tool requires orderId or orderNo");
            }
            if (result == null) {
                throw new ToolClientException("Purchase service returned empty response");
            }
            if (!result.success()) {
                throw new ToolClientException("Purchase service failed: " + result.message());
            }
            log.info("Purchase order tool HTTP call success, tenantId={}, userId={}, runId={}, orderId={}, orderNo={}, latencyMs={}",
                    request.getContext().tenantId(), request.getContext().userId(), request.getRunId(),
                    orderId, orderNo, elapsedMs(startedAt));
            return withAdapterMode(result.data());
        } catch (ToolClientException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ToolClientException("Purchase service call failed: " + ex.getMessage(), ex);
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
