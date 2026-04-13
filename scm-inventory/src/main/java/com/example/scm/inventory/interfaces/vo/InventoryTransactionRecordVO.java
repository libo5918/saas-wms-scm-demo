package com.example.scm.inventory.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "库存流水展示对象。")
public class InventoryTransactionRecordVO {

    @Schema(description = "库存流水号。")
    private String txnNo;

    @Schema(description = "业务类型。")
    private String bizType;

    @Schema(description = "业务单号。")
    private String bizNo;

    @Schema(description = "物料ID。")
    private Long materialId;

    @Schema(description = "仓库ID。")
    private Long warehouseId;

    @Schema(description = "库位ID。")
    private Long locationId;

    @Schema(description = "出入库方向。")
    private String txnDirection;

    @Schema(description = "本次变动数量。")
    private BigDecimal txnQty;

    @Schema(description = "变动前数量。")
    private BigDecimal beforeQty;

    @Schema(description = "变动后数量。")
    private BigDecimal afterQty;

    public String getTxnNo() {
        return txnNo;
    }

    public void setTxnNo(String txnNo) {
        this.txnNo = txnNo;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public String getBizNo() {
        return bizNo;
    }

    public void setBizNo(String bizNo) {
        this.bizNo = bizNo;
    }

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

    public String getTxnDirection() {
        return txnDirection;
    }

    public void setTxnDirection(String txnDirection) {
        this.txnDirection = txnDirection;
    }

    public BigDecimal getTxnQty() {
        return txnQty;
    }

    public void setTxnQty(BigDecimal txnQty) {
        this.txnQty = txnQty;
    }

    public BigDecimal getBeforeQty() {
        return beforeQty;
    }

    public void setBeforeQty(BigDecimal beforeQty) {
        this.beforeQty = beforeQty;
    }

    public BigDecimal getAfterQty() {
        return afterQty;
    }

    public void setAfterQty(BigDecimal afterQty) {
        this.afterQty = afterQty;
    }
}
