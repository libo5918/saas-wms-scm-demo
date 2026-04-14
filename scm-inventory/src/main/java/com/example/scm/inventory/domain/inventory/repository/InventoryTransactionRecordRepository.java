package com.example.scm.inventory.domain.inventory.repository;

import com.example.scm.inventory.domain.inventory.entity.InventoryTransactionRecord;

import java.util.List;

public interface InventoryTransactionRecordRepository {

    /**
     * 检查同一业务单是否已经生成过入库流水。
     */
    boolean existsStockInRecord(Long tenantId,
                                String bizType,
                                String bizNo,
                                Long materialId,
                                Long warehouseId,
                                Long locationId);

    /**
     * 检查同一业务单是否已经生成过出库流水。
     */
    boolean existsStockOutRecord(Long tenantId,
                                 String bizType,
                                 String bizNo,
                                 Long materialId,
                                 Long warehouseId,
                                 Long locationId);

    boolean existsLockRecord(Long tenantId,
                             String bizType,
                             String bizNo,
                             Long materialId,
                             Long warehouseId,
                             Long locationId);

    boolean existsUnlockRecord(Long tenantId,
                               String bizType,
                               String bizNo,
                               Long materialId,
                               Long warehouseId,
                               Long locationId);

    /**
     * 保存库存流水记录。
     */
    void save(InventoryTransactionRecord inventoryTransactionRecord);

    /**
     * 按业务单查询库存流水列表。
     */
    List<InventoryTransactionRecord> findByBizNo(Long tenantId, String bizType, String bizNo);
}
