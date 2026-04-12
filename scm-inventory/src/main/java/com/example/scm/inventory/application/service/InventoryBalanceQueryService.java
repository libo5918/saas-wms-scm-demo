package com.example.scm.inventory.application.service;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.TenantContext;
import com.example.scm.inventory.application.query.InventoryBalanceDTO;
import com.example.scm.inventory.domain.inventory.aggregate.InventoryBalance;
import com.example.scm.inventory.domain.inventory.repository.InventoryBalanceRepository;
import com.example.scm.inventory.domain.inventory.valueobject.InventoryKey;
import org.springframework.stereotype.Service;

@Service
public class InventoryBalanceQueryService {

    private final InventoryBalanceRepository inventoryBalanceRepository;

    public InventoryBalanceQueryService(InventoryBalanceRepository inventoryBalanceRepository) {
        this.inventoryBalanceRepository = inventoryBalanceRepository;
    }

    public InventoryBalanceDTO getBalance(Long materialId, Long warehouseId, Long locationId) {
        Long tenantId = TenantContext.getRequiredTenantId();
        InventoryKey inventoryKey = new InventoryKey(tenantId, materialId, warehouseId, locationId);
        InventoryBalance balance = inventoryBalanceRepository.findByKey(inventoryKey)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Inventory balance not found"));

        InventoryBalanceDTO dto = new InventoryBalanceDTO();
        dto.setMaterialId(balance.getInventoryKey().getMaterialId());
        dto.setWarehouseId(balance.getInventoryKey().getWarehouseId());
        dto.setLocationId(balance.getInventoryKey().getLocationId());
        dto.setOnHandQty(balance.getOnHandQty());
        dto.setLockedQty(balance.getLockedQty());
        dto.setAvailableQty(balance.getAvailableQty());
        dto.setVersion(balance.getVersion());
        return dto;
    }
}
