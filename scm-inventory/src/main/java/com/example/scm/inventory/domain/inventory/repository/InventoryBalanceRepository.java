package com.example.scm.inventory.domain.inventory.repository;

import com.example.scm.inventory.domain.inventory.aggregate.InventoryBalance;
import com.example.scm.inventory.domain.inventory.valueobject.InventoryKey;

import java.util.Optional;

public interface InventoryBalanceRepository {

    Optional<InventoryBalance> findByKey(InventoryKey inventoryKey);

    InventoryBalance save(InventoryBalance inventoryBalance);
}
