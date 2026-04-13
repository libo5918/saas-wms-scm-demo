package com.example.scm.inventory.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "库存余额展示对象。")
public class InventoryBalanceVO {

    @Schema(description = "物料ID。")
    private Long materialId;

    @Schema(description = "仓库ID。")
    private Long warehouseId;

    @Schema(description = "库位ID。")
    private Long locationId;

    @Schema(description = "现存数量。")
    private BigDecimal onHandQty;

    @Schema(description = "锁定数量。")
    private BigDecimal lockedQty;

    @Schema(description = "可用数量。")
    private BigDecimal availableQty;

    @Schema(description = "乐观锁版本号。")
    private Long version;

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

    public BigDecimal getOnHandQty() {
        return onHandQty;
    }

    public void setOnHandQty(BigDecimal onHandQty) {
        this.onHandQty = onHandQty;
    }

    public BigDecimal getLockedQty() {
        return lockedQty;
    }

    public void setLockedQty(BigDecimal lockedQty) {
        this.lockedQty = lockedQty;
    }

    public BigDecimal getAvailableQty() {
        return availableQty;
    }

    public void setAvailableQty(BigDecimal availableQty) {
        this.availableQty = availableQty;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
