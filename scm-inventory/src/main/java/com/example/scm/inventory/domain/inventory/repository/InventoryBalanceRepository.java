package com.example.scm.inventory.domain.inventory.repository;

import com.example.scm.inventory.domain.inventory.aggregate.InventoryBalance;
import com.example.scm.inventory.domain.inventory.valueobject.InventoryKey;

import java.util.Optional;

public interface InventoryBalanceRepository {

    /**
     * 按库存维度查询库存余额聚合。
     */
    Optional<InventoryBalance> findByKey(InventoryKey inventoryKey);

    /**
     * 保存库存余额聚合。
     * 新聚合执行插入，已存在聚合执行更新。
     */
    InventoryBalance save(InventoryBalance inventoryBalance);
}
