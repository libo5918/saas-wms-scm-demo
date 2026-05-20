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
 * 主数据 Tool 的 HTTP 客户端。
 *
 * <p>仅在 `ai.agent.tools.adapter-mode=http` 时启用，当前优先适配物料详情查询。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ai.agent.tools", name = "adapter-mode", havingValue = "http")
public class HttpMdmToolClient extends AbstractToolClientSupport implements MdmToolClient {

    private static final ParameterizedTypeReference<Result<Map<String, Object>>> RESULT_TYPE = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;
    private final String mdmBaseUrl;

    public HttpMdmToolClient(RestClient.Builder restClientBuilder, AiAgentProperties properties) {
        AiAgentProperties.HttpToolClientProperties http = properties.getTools().getHttp();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(http.getConnectTimeoutMs());
        requestFactory.setReadTimeout(http.getReadTimeoutMs());
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
        this.mdmBaseUrl = properties.getTools().getHttp().getMdmBaseUrl();
    }

    @Override
    public Map<String, Object> getMaterial(ToolRequest request) {
        long startedAt = System.nanoTime();
        Long materialId = longParam(request.getParameters(), "materialId", 1001L);
        try {
            Result<Map<String, Object>> result = restClient.get()
                    .uri(mdmBaseUrl + "/api/v1/materials/{materialId}", materialId)
                    .headers(headers -> headers.addAll(identityHeaders(request)))
                    .retrieve()
                    .body(RESULT_TYPE);
            if (result == null) {
                throw new ToolClientException("MDM service returned empty response");
            }
            if (!result.success()) {
                throw new ToolClientException("MDM service failed: " + result.message());
            }
            log.info("MDM material tool HTTP call success, tenantId={}, userId={}, runId={}, materialId={}, latencyMs={}",
                    request.getContext().tenantId(), request.getContext().userId(), request.getRunId(),
                    materialId, elapsedMs(startedAt));
            Map<String, Object> data = result.data() == null ? Map.of() : result.data();
            return withAdapterMode(data);
        } catch (ToolClientException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ToolClientException("MDM service call failed: " + ex.getMessage(), ex);
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
