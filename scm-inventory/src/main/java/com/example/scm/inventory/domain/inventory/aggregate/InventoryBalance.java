package com.example.scm.inventory.domain.inventory.aggregate;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.inventory.domain.inventory.entity.InventoryTransactionRecord;
import com.example.scm.inventory.domain.inventory.valueobject.InventoryKey;

import java.math.BigDecimal;

public class InventoryBalance {

    private Long id;
    private InventoryKey inventoryKey;
    private BigDecimal onHandQty;
    private BigDecimal lockedQty;
    private BigDecimal availableQty;
    private Long version;
    private Long createdBy;
    private Long updatedBy;

    public static InventoryBalance initialize(InventoryKey inventoryKey, Long operatorId) {
        InventoryBalance balance = new InventoryBalance();
        balance.inventoryKey = inventoryKey;
        balance.onHandQty = BigDecimal.ZERO;
        balance.lockedQty = BigDecimal.ZERO;
        balance.availableQty = BigDecimal.ZERO;
        balance.version = 0L;
        balance.createdBy = operatorId;
        balance.updatedBy = operatorId;
        return balance;
    }

    public InventoryTransactionRecord stockIn(String txnNo, String bizType, String bizNo, BigDecimal quantity, Long operatorId) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "stock-in quantity must be greater than zero");
        }

        BigDecimal beforeQty = onHandQty;
        BigDecimal afterQty = beforeQty.add(quantity);
        this.onHandQty = afterQty;
        this.availableQty = availableQty.add(quantity);
        this.updatedBy = operatorId;
        this.version = version + 1;

        return InventoryTransactionRecord.stockIn(
                inventoryKey.getTenantId(),
                txnNo,
                bizType,
                bizNo,
                inventoryKey.getMaterialId(),
                inventoryKey.getWarehouseId(),
                inventoryKey.getLocationId(),
                quantity,
                beforeQty,
                afterQty
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public InventoryKey getInventoryKey() {
        return inventoryKey;
    }

    public void setInventoryKey(InventoryKey inventoryKey) {
        this.inventoryKey = inventoryKey;
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

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}
