package com.example.scm.purchase.client;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.Result;
import com.example.scm.purchase.entity.PurchaseReceipt;
import com.example.scm.purchase.entity.PurchaseReceiptItem;
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

/**
 * 库存服务客户端，负责调用 inventory 服务执行库存入库。
 */
@Component
public class InventoryStockInClient {

    private final RestTemplate restTemplate;
    private final String inventoryBaseUrl;

    public InventoryStockInClient(RestTemplateBuilder restTemplateBuilder,
                                  @Value("${integration.inventory.base-url}") String inventoryBaseUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.inventoryBaseUrl = inventoryBaseUrl;
    }

    /**
     * 根据收货单调用库存服务执行入库。
     */
    public void stockIn(Long tenantId, Long operatorId, PurchaseReceipt receipt, List<PurchaseReceiptItem> items) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-Id", String.valueOf(tenantId));

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("bizType", "PURCHASE_RECEIPT");
        requestBody.put("bizNo", receipt.getReceiptNo());
        requestBody.put("operatorId", operatorId);
        requestBody.put("items", items.stream().map(item -> {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("materialId", item.getMaterialId());
            line.put("warehouseId", receipt.getWarehouseId());
            line.put("locationId", item.getLocationId());
            line.put("quantity", item.getReceiptQty());
            return line;
        }).toList());

        try {
            Result<?> response = restTemplate.postForObject(
                    inventoryBaseUrl + "/api/v1/inventory/stock-ins",
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
