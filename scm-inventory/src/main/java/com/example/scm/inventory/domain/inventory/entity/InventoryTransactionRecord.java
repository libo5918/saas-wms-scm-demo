package com.example.scm.inventory.domain.inventory.entity;

import com.example.scm.inventory.domain.inventory.valueobject.InventoryTransactionDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "库存流水实体，记录一次库存变更前后的数量快照。")
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
}
