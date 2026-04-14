package com.example.scm.inventory.domain.inventory.repository;

import com.example.scm.inventory.domain.inventory.entity.InventoryTransactionRecord;

import java.util.List;

public interface InventoryTransactionRecordRepository {

    boolean existsStockInRecord(Long tenantId,
                                String bizType,
                                String bizNo,
                                Long materialId,
                                Long warehouseId,
                                Long locationId);

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

    boolean existsAdjustInRecord(Long tenantId,
                                 String bizType,
                                 String bizNo,
                                 Long materialId,
                                 Long warehouseId,
                                 Long locationId);

    boolean existsAdjustOutRecord(Long tenantId,
                                  String bizType,
                                  String bizNo,
                                  Long materialId,
                                  Long warehouseId,
                                  Long locationId);

    boolean existsMoveOutRecord(Long tenantId,
                                String bizType,
                                String bizNo,
                                Long materialId,
                                Long warehouseId,
                                Long locationId);

    boolean existsMoveInRecord(Long tenantId,
                               String bizType,
                               String bizNo,
                               Long materialId,
                               Long warehouseId,
                               Long locationId);

    void save(InventoryTransactionRecord inventoryTransactionRecord);

    List<InventoryTransactionRecord> findByBizNo(Long tenantId, String bizType, String bizNo);
}
