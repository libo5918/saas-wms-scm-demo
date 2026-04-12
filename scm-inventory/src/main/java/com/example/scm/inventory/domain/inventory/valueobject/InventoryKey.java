package com.example.scm.inventory.domain.inventory.valueobject;

public class InventoryKey {

    private final Long tenantId;
    private final Long materialId;
    private final Long warehouseId;
    private final Long locationId;

    public InventoryKey(Long tenantId, Long materialId, Long warehouseId, Long locationId) {
        this.tenantId = tenantId;
        this.materialId = materialId;
        this.warehouseId = warehouseId;
        this.locationId = locationId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public Long getLocationId() {
        return locationId;
    }
}
