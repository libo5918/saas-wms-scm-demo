package com.example.scm.inventory.application.command;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "库存锁定应用命令明细。")
public class StockLockItemCommand {

    @Schema(description = "物料ID。")
    private Long materialId;

    @Schema(description = "仓库ID。")
    private Long warehouseId;

    @Schema(description = "库位ID。")
    private Long locationId;

    @Schema(description = "本次锁定数量。")
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
