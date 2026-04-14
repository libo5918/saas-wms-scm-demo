package com.example.scm.inventory.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "库存锁定结果明细展示对象。")
public class StockLockLineVO {
    @Schema(description = "库存流水号。")
    private String txnNo;
    @Schema(description = "物料ID。")
    private Long materialId;
    @Schema(description = "仓库ID。")
    private Long warehouseId;
    @Schema(description = "库位ID。")
    private Long locationId;
    @Schema(description = "本次锁定数量。")
    private BigDecimal quantity;
    @Schema(description = "变动前锁定数量。")
    private BigDecimal beforeQty;
    @Schema(description = "变动后锁定数量。")
    private BigDecimal afterQty;
    public String getTxnNo() { return txnNo; }
    public void setTxnNo(String txnNo) { this.txnNo = txnNo; }
    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getBeforeQty() { return beforeQty; }
    public void setBeforeQty(BigDecimal beforeQty) { this.beforeQty = beforeQty; }
    public BigDecimal getAfterQty() { return afterQty; }
    public void setAfterQty(BigDecimal afterQty) { this.afterQty = afterQty; }
}
