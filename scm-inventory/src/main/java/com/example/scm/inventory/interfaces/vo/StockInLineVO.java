package com.example.scm.inventory.interfaces.vo;

import java.math.BigDecimal;

public class StockInLineVO {

    private String txnNo;
    private Long materialId;
    private Long warehouseId;
    private Long locationId;
    private BigDecimal quantity;
    private BigDecimal beforeQty;
    private BigDecimal afterQty;

    public String getTxnNo() {
        return txnNo;
    }

    public void setTxnNo(String txnNo) {
        this.txnNo = txnNo;
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

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
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
