package com.example.scm.inventory.application.service;

import com.example.scm.inventory.integration.LocationClient;
import com.example.scm.inventory.integration.WarehouseClient;
import org.springframework.stereotype.Service;

/**
 * 仓库和库位校验服务。
 * 负责在库存写操作前统一校验仓库、库位是否存在且处于可用状态。
 */
@Service
public class StorageValidationService {

    private final WarehouseClient warehouseClient;
    private final LocationClient locationClient;

    public StorageValidationService(WarehouseClient warehouseClient, LocationClient locationClient) {
        this.warehouseClient = warehouseClient;
        this.locationClient = locationClient;
    }

    /**
     * 校验仓库和库位可用于库存业务。
     */
    public void validateStorageEnabled(Long tenantId, Long warehouseId, Long locationId) {
        warehouseClient.validateWarehouseEnabled(tenantId, warehouseId);
        locationClient.validateLocationEnabled(tenantId, warehouseId, locationId);
    }
}
