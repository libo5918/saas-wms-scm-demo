package com.example.scm.inventory.domain.inventory.aggregate;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.inventory.domain.inventory.entity.InventoryTransactionRecord;
import com.example.scm.inventory.domain.inventory.valueobject.InventoryKey;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "库存余额聚合根，负责维护某个库存维度上的数量状态。")
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
}
