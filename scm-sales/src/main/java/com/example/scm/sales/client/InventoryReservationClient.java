package com.example.scm.sales.client;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.Result;
import com.example.scm.sales.entity.SalesOrder;
import com.example.scm.sales.entity.SalesOrderItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class InventoryReservationClient {

    private final RestTemplate restTemplate;
    private final String inventoryBaseUrl;

    public InventoryReservationClient(RestTemplateBuilder restTemplateBuilder,
                                      @Value("${integration.inventory.base-url}") String inventoryBaseUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.inventoryBaseUrl = inventoryBaseUrl;
    }

    public void lock(Long tenantId, Long operatorId, SalesOrder order, List<SalesOrderItem> items) {
        postInventoryAction(tenantId, "/api/v1/inventory/locks", "SALES_ORDER", operatorId, order, items);
    }

    public void stockOut(Long tenantId, Long operatorId, SalesOrder order, List<SalesOrderItem> items) {
        postInventoryAction(tenantId, "/api/v1/inventory/locked-stock-outs", "SALES_ORDER", operatorId, order, items);
    }

    public void unlock(Long tenantId, Long operatorId, SalesOrder order, List<SalesOrderItem> items) {
        postInventoryAction(tenantId, "/api/v1/inventory/unlocks", "SALES_ORDER", operatorId, order, items);
    }

    private void postInventoryAction(Long tenantId,
                                     String path,
                                     String bizType,
                                     Long operatorId,
                                     SalesOrder order,
                                     List<SalesOrderItem> items) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-Id", String.valueOf(tenantId));

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("bizType", bizType);
        requestBody.put("bizNo", order.getOrderNo());
        requestBody.put("operatorId", operatorId);
        requestBody.put("items", items.stream().map(item -> {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("materialId", item.getMaterialId());
            line.put("warehouseId", order.getWarehouseId());
            line.put("locationId", item.getLocationId());
            line.put("quantity", item.getSaleQty());
            return line;
        }).toList());

        try {
            Result<?> response = restTemplate.postForObject(
                    inventoryBaseUrl + path,
                    new HttpEntity<>(requestBody, headers),
                    Result.class
            );
            if (response == null) {
                throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Inventory service returned empty response");
            }
            if (!response.success()) {
                throw new BusinessException(response.code(), response.message());
            }
        } catch (RestClientException ex) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Call inventory service failed: " + ex.getMessage());
        }
    }
}
