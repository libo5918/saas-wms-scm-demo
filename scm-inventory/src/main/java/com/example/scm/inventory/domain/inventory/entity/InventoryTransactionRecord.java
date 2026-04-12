package com.example.scm.inventory.domain.inventory.entity;

import com.example.scm.inventory.domain.inventory.valueobject.InventoryTransactionDirection;

import java.math.BigDecimal;

public class InventoryTransactionRecord {

    private Long id;
    private Long tenantId;
    private String txnNo;
    private String bizType;
    private String bizNo;
    private Long materialId;
    private Long warehouseId;
    private Long locationId;
    private InventoryTransactionDirection txnDirection;
    private BigDecimal txnQty;
    private BigDecimal beforeQty;
    private BigDecimal afterQty;

    public static InventoryTransactionRecord stockIn(Long tenantId,
                                                     String txnNo,
                                                     String bizType,
                                                     String bizNo,
                                                     Long materialId,
                                                     Long warehouseId,
                                                     Long locationId,
                                                     BigDecimal txnQty,
                                                     BigDecimal beforeQty,
                                                     BigDecimal afterQty) {
        InventoryTransactionRecord record = new InventoryTransactionRecord();
        record.tenantId = tenantId;
        record.txnNo = txnNo;
        record.bizType = bizType;
        record.bizNo = bizNo;
        record.materialId = materialId;
        record.warehouseId = warehouseId;
        record.locationId = locationId;
        record.txnDirection = InventoryTransactionDirection.IN;
        record.txnQty = txnQty;
        record.beforeQty = beforeQty;
        record.afterQty = afterQty;
        return record;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

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

    public InventoryTransactionDirection getTxnDirection() {
        return txnDirection;
    }

    public void setTxnDirection(InventoryTransactionDirection txnDirection) {
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
