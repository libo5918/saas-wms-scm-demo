package com.example.scm.inventory.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "库存锁定明细请求。")
public class StockLockItemRequest {
    @Schema(description = "物料ID。")
    @NotNull(message = "materialId cannot be null")
    private Long materialId;
    @Schema(description = "仓库ID。")
    @NotNull(message = "warehouseId cannot be null")
    private Long warehouseId;
    @Schema(description = "库位ID。")
    @NotNull(message = "locationId cannot be null")
    private Long locationId;
    @Schema(description = "锁定数量。")
    @NotNull(message = "quantity cannot be null")
    @DecimalMin(value = "0.0001", message = "quantity must be greater than zero")
    private BigDecimal quantity;

    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
}
