package com.example.scm.inventory.domain.inventory.entity;

import com.example.scm.inventory.domain.inventory.valueobject.InventoryTransactionDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "库存流水实体，记录一次库存变动前后的数量快照。")
public class InventoryTransactionRecord {

    @Schema(description = "库存流水主键ID。")
    private Long id;

    @Schema(description = "租户ID。")
    private Long tenantId;

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
    private InventoryTransactionDirection txnDirection;

    @Schema(description = "本次变动数量。")
    private BigDecimal txnQty;

    @Schema(description = "变动前数量。")
    private BigDecimal beforeQty;

    @Schema(description = "变动后数量。")
    private BigDecimal afterQty;

    /**
     * 创建入库流水。
     */
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

    /**
     * 创建出库流水。
     */
    public static InventoryTransactionRecord stockOut(Long tenantId,
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
        record.txnDirection = InventoryTransactionDirection.OUT;
        record.txnQty = txnQty;
        record.beforeQty = beforeQty;
        record.afterQty = afterQty;
        return record;
    }

    public static InventoryTransactionRecord lock(Long tenantId,
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
        record.txnDirection = InventoryTransactionDirection.LOCK;
        record.txnQty = txnQty;
        record.beforeQty = beforeQty;
        record.afterQty = afterQty;
        return record;
    }

    public static InventoryTransactionRecord unlock(Long tenantId,
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
        record.txnDirection = InventoryTransactionDirection.UNLOCK;
        record.txnQty = txnQty;
        record.beforeQty = beforeQty;
        record.afterQty = afterQty;
        return record;
    }

    public static InventoryTransactionRecord adjustIn(Long tenantId,
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
        record.txnDirection = InventoryTransactionDirection.ADJUST_IN;
        record.txnQty = txnQty;
        record.beforeQty = beforeQty;
        record.afterQty = afterQty;
        return record;
    }

    public static InventoryTransactionRecord adjustOut(Long tenantId,
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
        record.txnDirection = InventoryTransactionDirection.ADJUST_OUT;
        record.txnQty = txnQty;
        record.beforeQty = beforeQty;
        record.afterQty = afterQty;
        return record;
    }

    public static InventoryTransactionRecord moveOut(Long tenantId,
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
        record.txnDirection = InventoryTransactionDirection.MOVE_OUT;
        record.txnQty = txnQty;
        record.beforeQty = beforeQty;
        record.afterQty = afterQty;
        return record;
    }

    public static InventoryTransactionRecord moveIn(Long tenantId,
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
        record.txnDirection = InventoryTransactionDirection.MOVE_IN;
        record.txnQty = txnQty;
        record.beforeQty = beforeQty;
        record.afterQty = afterQty;
        return record;
    }
}
