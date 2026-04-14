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

    @Schema(description = "库存余额主键ID。")
    private Long id;

    @Schema(description = "库存维度键，唯一标识某个租户下物料在仓库库位上的库存。")
    private InventoryKey inventoryKey;

    @Schema(description = "现存数量。")
    private BigDecimal onHandQty;

    @Schema(description = "锁定数量。")
    private BigDecimal lockedQty;

    @Schema(description = "可用数量。")
    private BigDecimal availableQty;

    @Schema(description = "乐观锁版本号。")
    private Long version;

    @Schema(description = "创建人。")
    private Long createdBy;

    @Schema(description = "更新人。")
    private Long updatedBy;

    /**
     * 初始化一个全新的库存余额聚合。
     *
     * @param inventoryKey 库存维度键
     * @param operatorId 创建人/更新人
     * @return 初始数量均为 0 的库存余额聚合
     */
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

    /**
     * 执行入库并生成对应库存流水。
     *
     * @param txnNo 流水号
     * @param bizType 业务类型
     * @param bizNo 业务单号
     * @param quantity 入库数量
     * @param operatorId 操作人
     * @return 入库流水实体
     */
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

    /**
     * 执行出库并生成对应库存流水。
     *
     * @param txnNo 流水号
     * @param bizType 业务类型
     * @param bizNo 业务单号
     * @param quantity 出库数量
     * @param operatorId 操作人
     * @return 出库流水实体
     */
    public InventoryTransactionRecord stockOut(String txnNo, String bizType, String bizNo, BigDecimal quantity, Long operatorId) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "stock-out quantity must be greater than zero");
        }
        if (availableQty.compareTo(quantity) < 0) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Insufficient available inventory");
        }

        BigDecimal beforeQty = onHandQty;
        BigDecimal afterQty = beforeQty.subtract(quantity);
        this.onHandQty = afterQty;
        this.availableQty = availableQty.subtract(quantity);
        this.updatedBy = operatorId;
        this.version = version + 1;

        return InventoryTransactionRecord.stockOut(
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

    /**
     * 执行锁定库存出库。
     * 该动作用于先锁库后发货的场景，发货时应消耗锁定数量，而不是再次占用可用库存。
     *
     * @param txnNo 流水号
     * @param bizType 业务类型
     * @param bizNo 业务单号
     * @param quantity 出库数量
     * @param operatorId 操作人
     * @return 出库流水实体
     */
    public InventoryTransactionRecord lockedStockOut(String txnNo, String bizType, String bizNo, BigDecimal quantity, Long operatorId) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "locked stock-out quantity must be greater than zero");
        }
        if (lockedQty.compareTo(quantity) < 0) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Insufficient locked inventory");
        }

        BigDecimal beforeQty = onHandQty;
        BigDecimal afterQty = beforeQty.subtract(quantity);
        this.onHandQty = afterQty;
        this.lockedQty = lockedQty.subtract(quantity);
        this.updatedBy = operatorId;
        this.version = version + 1;

        return InventoryTransactionRecord.stockOut(
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

    public InventoryTransactionRecord lock(String txnNo, String bizType, String bizNo, BigDecimal quantity, Long operatorId) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "stock-lock quantity must be greater than zero");
        }
        if (availableQty.compareTo(quantity) < 0) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Insufficient available inventory");
        }

        BigDecimal beforeQty = lockedQty;
        BigDecimal afterQty = beforeQty.add(quantity);
        this.lockedQty = afterQty;
        this.availableQty = availableQty.subtract(quantity);
        this.updatedBy = operatorId;
        this.version = version + 1;

        return InventoryTransactionRecord.lock(
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

    public InventoryTransactionRecord unlock(String txnNo, String bizType, String bizNo, BigDecimal quantity, Long operatorId) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "stock-unlock quantity must be greater than zero");
        }
        if (lockedQty.compareTo(quantity) < 0) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Insufficient locked inventory");
        }

        BigDecimal beforeQty = lockedQty;
        BigDecimal afterQty = beforeQty.subtract(quantity);
        this.lockedQty = afterQty;
        this.availableQty = availableQty.add(quantity);
        this.updatedBy = operatorId;
        this.version = version + 1;

        return InventoryTransactionRecord.unlock(
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
