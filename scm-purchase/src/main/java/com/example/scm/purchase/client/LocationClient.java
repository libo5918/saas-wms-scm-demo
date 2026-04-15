package com.example.scm.purchase.client;

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
 * MDM 库位服务客户端，负责校验库位是否存在、启用且归属于指定仓库。
 */
@Component
public class LocationClient {

    private static final Integer LOCATION_ENABLED = 1;

    private final RestTemplate restTemplate;
    private final String mdmBaseUrl;

    public LocationClient(RestTemplateBuilder restTemplateBuilder,
                          @Value("${integration.mdm.base-url}") String mdmBaseUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.mdmBaseUrl = mdmBaseUrl;
    }

    public void validateLocationEnabled(Long tenantId, Long warehouseId, Long locationId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-Id", String.valueOf(tenantId));

        try {
            ResponseEntity<Result> responseEntity = restTemplate.exchange(
                    mdmBaseUrl + "/api/v1/locations/" + locationId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Result.class
            );
            Result<?> response = responseEntity.getBody();
            if (response == null) {
                throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Location service returned empty response");
            }
            if (!response.success()) {
                throw new BusinessException(response.code(), response.message());
            }
            if (!(response.data() instanceof Map<?, ?> locationData)) {
                throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Location service returned invalid payload");
            }
            Object status = locationData.get("status");
            if (!(status instanceof Number statusValue) || statusValue.intValue() != LOCATION_ENABLED) {
                throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Location is disabled");
            }
            Object actualWarehouseId = locationData.get("warehouseId");
            if (!(actualWarehouseId instanceof Number warehouseValue) || warehouseValue.longValue() != warehouseId) {
                throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Location does not belong to warehouse");
            }
        } catch (RestClientException ex) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Call location service failed: " + ex.getMessage());
        }
    }
}
