package com.example.scm.inventory.domain.inventory.repository;

import com.example.scm.inventory.domain.inventory.entity.InventoryTransactionRecord;

public interface InventoryTransactionRecordRepository {

    boolean existsStockInRecord(Long tenantId,
                                String bizType,
                                String bizNo,
                                Long materialId,
                                Long warehouseId,
                                Long locationId);

    void save(InventoryTransactionRecord inventoryTransactionRecord);
}
