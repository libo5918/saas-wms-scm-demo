package com.example.scm.inventory.integration;

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
 * MDM 物料服务客户端，负责校验物料是否存在且为启用状态。
 */
@Component
public class MaterialClient {

    private static final Integer MATERIAL_ENABLED = 1;

    private final RestTemplate restTemplate;
    private final String materialBaseUrl;

    public MaterialClient(RestTemplateBuilder restTemplateBuilder,
                          @Value("${integration.mdm.base-url}") String materialBaseUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.materialBaseUrl = materialBaseUrl;
    }

    /**
     * 校验物料存在且为启用状态。
     */
    public void validateMaterialEnabled(Long tenantId, Long materialId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-Id", String.valueOf(tenantId));

        try {
            ResponseEntity<Result> responseEntity = restTemplate.exchange(
                    materialBaseUrl + "/api/v1/materials/" + materialId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Result.class
            );
            Result<?> response = responseEntity.getBody();
            if (response == null) {
                throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Material service returned empty response");
            }
            if (!response.success()) {
                throw new BusinessException(response.code(), response.message());
            }
            if (!(response.data() instanceof Map<?, ?> materialData)) {
                throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Material service returned invalid payload");
            }
            Object status = materialData.get("status");
            if (!(status instanceof Number statusValue) || statusValue.intValue() != MATERIAL_ENABLED) {
                throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Material is disabled");
            }
        } catch (RestClientException ex) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Call material service failed: " + ex.getMessage());
        }
    }
}
