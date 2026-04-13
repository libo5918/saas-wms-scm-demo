package com.example.scm.inventory.interfaces.vo;

import java.math.BigDecimal;

public class InventoryTransactionRecordVO {

    private String txnNo;
    private String bizType;
    private String bizNo;
    private Long materialId;
    private Long warehouseId;
    private Long locationId;
    private String txnDirection;
    private BigDecimal txnQty;
    private BigDecimal beforeQty;
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
