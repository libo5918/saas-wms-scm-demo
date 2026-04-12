package com.example.scm.inventory.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.scm.inventory.domain.inventory.entity.InventoryTransactionRecord;
import com.example.scm.inventory.domain.inventory.repository.InventoryTransactionRecordRepository;
import com.example.scm.inventory.domain.inventory.valueobject.InventoryTransactionDirection;
import com.example.scm.inventory.infrastructure.persistence.mapper.InventoryTxnRecordMapper;
import com.example.scm.inventory.infrastructure.persistence.po.InventoryTxnRecordPO;
import org.springframework.stereotype.Repository;

@Repository
public class InventoryTransactionRecordRepositoryImpl implements InventoryTransactionRecordRepository {

    private final InventoryTxnRecordMapper inventoryTxnRecordMapper;

    public InventoryTransactionRecordRepositoryImpl(InventoryTxnRecordMapper inventoryTxnRecordMapper) {
        this.inventoryTxnRecordMapper = inventoryTxnRecordMapper;
    }

    @Override
    public boolean existsStockInRecord(Long tenantId,
                                       String bizType,
                                       String bizNo,
                                       Long materialId,
                                       Long warehouseId,
                                       Long locationId) {
        LambdaQueryWrapper<InventoryTxnRecordPO> queryWrapper = new LambdaQueryWrapper<InventoryTxnRecordPO>()
                .eq(InventoryTxnRecordPO::getTenantId, tenantId)
                .eq(InventoryTxnRecordPO::getBizType, bizType)
                .eq(InventoryTxnRecordPO::getBizNo, bizNo)
                .eq(InventoryTxnRecordPO::getMaterialId, materialId)
                .eq(InventoryTxnRecordPO::getWarehouseId, warehouseId)
                .eq(InventoryTxnRecordPO::getLocationId, locationId)
                .eq(InventoryTxnRecordPO::getTxnDirection, InventoryTransactionDirection.IN.name())
                .last("LIMIT 1");
        return inventoryTxnRecordMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public void save(InventoryTransactionRecord inventoryTransactionRecord) {
        InventoryTxnRecordPO po = new InventoryTxnRecordPO();
        po.setTenantId(inventoryTransactionRecord.getTenantId());
        po.setTxnNo(inventoryTransactionRecord.getTxnNo());
        po.setBizType(inventoryTransactionRecord.getBizType());
        po.setBizNo(inventoryTransactionRecord.getBizNo());
        po.setMaterialId(inventoryTransactionRecord.getMaterialId());
        po.setWarehouseId(inventoryTransactionRecord.getWarehouseId());
        po.setLocationId(inventoryTransactionRecord.getLocationId());
        po.setTxnDirection(inventoryTransactionRecord.getTxnDirection().name());
        po.setTxnQty(inventoryTransactionRecord.getTxnQty());
        po.setBeforeQty(inventoryTransactionRecord.getBeforeQty());
        po.setAfterQty(inventoryTransactionRecord.getAfterQty());
        inventoryTxnRecordMapper.insert(po);
    }
}
