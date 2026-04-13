package com.example.scm.inventory.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.scm.inventory.domain.inventory.aggregate.InventoryBalance;
import com.example.scm.inventory.domain.inventory.repository.InventoryBalanceRepository;
import com.example.scm.inventory.domain.inventory.valueobject.InventoryKey;
import com.example.scm.inventory.infrastructure.persistence.mapper.InventoryBalanceMapper;
import com.example.scm.inventory.infrastructure.persistence.po.InventoryBalancePO;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class InventoryBalanceRepositoryImpl implements InventoryBalanceRepository {

    private final InventoryBalanceMapper inventoryBalanceMapper;

    public InventoryBalanceRepositoryImpl(InventoryBalanceMapper inventoryBalanceMapper) {
        this.inventoryBalanceMapper = inventoryBalanceMapper;
    }

    @Override
    public Optional<InventoryBalance> findByKey(InventoryKey inventoryKey) {
        LambdaQueryWrapper<InventoryBalancePO> queryWrapper = new LambdaQueryWrapper<InventoryBalancePO>()
                .eq(InventoryBalancePO::getTenantId, inventoryKey.getTenantId())
                .eq(InventoryBalancePO::getMaterialId, inventoryKey.getMaterialId())
                .eq(InventoryBalancePO::getWarehouseId, inventoryKey.getWarehouseId())
                .eq(InventoryBalancePO::getLocationId, inventoryKey.getLocationId())
                .eq(InventoryBalancePO::getDeleted, 0)
                .last("LIMIT 1");
        return Optional.ofNullable(inventoryBalanceMapper.selectOne(queryWrapper)).map(this::toDomain);
    }

    @Override
    /**
     * 保存库存余额聚合。
     * 首次落库执行 insert，已有记录执行 updateById。
     */
    public InventoryBalance save(InventoryBalance inventoryBalance) {
        InventoryBalancePO po = toPO(inventoryBalance);
        if (po.getId() == null) {
            inventoryBalanceMapper.insert(po);
            inventoryBalance.setId(po.getId());
            return inventoryBalance;
        }
        inventoryBalanceMapper.updateById(po);
        return inventoryBalance;
    }

    /**
     * 将持久化对象转换为领域聚合。
     */
    private InventoryBalance toDomain(InventoryBalancePO po) {
        InventoryBalance balance = new InventoryBalance();
        balance.setId(po.getId());
        balance.setInventoryKey(new InventoryKey(po.getTenantId(), po.getMaterialId(), po.getWarehouseId(), po.getLocationId()));
        balance.setOnHandQty(po.getOnHandQty());
        balance.setLockedQty(po.getLockedQty());
        balance.setAvailableQty(po.getAvailableQty());
        balance.setVersion(po.getVersion());
        balance.setCreatedBy(po.getCreatedBy());
        balance.setUpdatedBy(po.getUpdatedBy());
        return balance;
    }

    /**
     * 将领域聚合转换为持久化对象。
     */
    private InventoryBalancePO toPO(InventoryBalance balance) {
        InventoryBalancePO po = new InventoryBalancePO();
        po.setId(balance.getId());
        po.setTenantId(balance.getInventoryKey().getTenantId());
        po.setMaterialId(balance.getInventoryKey().getMaterialId());
        po.setWarehouseId(balance.getInventoryKey().getWarehouseId());
        po.setLocationId(balance.getInventoryKey().getLocationId());
        po.setOnHandQty(balance.getOnHandQty());
        po.setLockedQty(balance.getLockedQty());
        po.setAvailableQty(balance.getAvailableQty());
        po.setVersion(balance.getVersion());
        po.setCreatedBy(balance.getCreatedBy());
        po.setUpdatedBy(balance.getUpdatedBy());
        po.setDeleted(0);
        return po;
    }
}
