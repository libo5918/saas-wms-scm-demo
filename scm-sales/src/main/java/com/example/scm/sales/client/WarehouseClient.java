package com.example.scm.sales.client;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * MDM 仓库服务客户端，负责校验仓库是否存在且为启用状态。
 */
@Component
public class WarehouseClient {

    private static final Integer WAREHOUSE_ENABLED = 1;

    private final RestTemplate restTemplate;
    private final String mdmBaseUrl;

    public WarehouseClient(RestTemplateBuilder restTemplateBuilder,
                           @Value("${integration.mdm.base-url}") String mdmBaseUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.mdmBaseUrl = mdmBaseUrl;
    }

    public void validateWarehouseEnabled(Long tenantId, Long warehouseId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-Id", String.valueOf(tenantId));

        try {
            ResponseEntity<Result> responseEntity = restTemplate.exchange(
                    mdmBaseUrl + "/api/v1/warehouses/" + warehouseId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Result.class
            );
            Result<?> response = responseEntity.getBody();
            if (response == null) {
                throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Warehouse service returned empty response");
            }
            if (!response.success()) {
                throw new BusinessException(response.code(), response.message());
            }
            if (!(response.data() instanceof Map<?, ?> warehouseData)) {
                throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Warehouse service returned invalid payload");
            }
            Object status = warehouseData.get("status");
            if (!(status instanceof Number statusValue) || statusValue.intValue() != WAREHOUSE_ENABLED) {
                throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Warehouse is disabled");
            }
        } catch (RestClientException ex) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Call warehouse service failed: " + ex.getMessage());
        }
    }
}
