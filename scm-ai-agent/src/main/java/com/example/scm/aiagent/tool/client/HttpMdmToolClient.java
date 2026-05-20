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
 * 主数据 Tool 的 HTTP 客户端。
 *
 * <p>仅在 `ai.agent.tools.adapter-mode=http` 时启用，当前支持按物料 ID
 * 或物料编码查询物料详情。</p>
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
        String materialCode = stringParam(request.getParameters(), "materialCode", null);
        Long materialId = longParam(request.getParameters(), "materialId", null);
        try {
            Result<Map<String, Object>> result;
            Map<String, Object> queryMetadata = new HashMap<>();
            if (StringUtils.hasText(materialCode)) {
                result = restClient.get()
                        .uri(mdmBaseUrl + "/api/v1/materials/by-code?materialCode={materialCode}", materialCode)
                        .headers(headers -> headers.addAll(identityHeaders(request)))
                        .retrieve()
                        .body(RESULT_TYPE);
                queryMetadata.put("queryType", "materialCode");
                queryMetadata.put("materialCode", materialCode);
            } else if (materialId != null) {
                result = restClient.get()
                        .uri(mdmBaseUrl + "/api/v1/materials/{materialId}", materialId)
                        .headers(headers -> headers.addAll(identityHeaders(request)))
                        .retrieve()
                        .body(RESULT_TYPE);
                queryMetadata.put("queryType", "materialId");
                queryMetadata.put("materialId", materialId);
            } else {
                throw new ToolClientException("MDM material tool requires materialId or materialCode");
            }
            if (result == null) {
                throw new ToolClientException("MDM service returned empty response");
            }
            if (!result.success()) {
                throw new ToolClientException("MDM service failed: " + result.message());
            }
            log.info("MDM material tool HTTP call success, tenantId={}, userId={}, runId={}, queryType={}, materialId={}, materialCode={}, latencyMs={}",
                    request.getContext().tenantId(), request.getContext().userId(), request.getRunId(),
                    queryMetadata.get("queryType"), queryMetadata.get("materialId"), queryMetadata.get("materialCode"),
                    elapsedMs(startedAt));
            Map<String, Object> data = result.data() == null ? Map.of() : result.data();
            return withAdapterMode(data, queryMetadata);
        } catch (ToolClientException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ToolClientException("MDM service call failed: " + ex.getMessage(), ex);
        }
    }

    private Map<String, Object> withAdapterMode(Map<String, Object> data, Map<String, Object> queryMetadata) {
        HashMap<String, Object> result = new HashMap<>(data);
        result.put("adapterMode", "http");
        result.putAll(queryMetadata);
        return result;
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
