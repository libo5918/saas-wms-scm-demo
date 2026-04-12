package com.example.scm.inventory.interfaces.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class StockInItemRequest {

    @NotNull(message = "materialId cannot be null")
    private Long materialId;

    @NotNull(message = "warehouseId cannot be null")
    private Long warehouseId;

    @NotNull(message = "locationId cannot be null")
    private Long locationId;

    @NotNull(message = "quantity cannot be null")
    @Positive(message = "quantity must be greater than zero")
    private BigDecimal quantity;

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}
