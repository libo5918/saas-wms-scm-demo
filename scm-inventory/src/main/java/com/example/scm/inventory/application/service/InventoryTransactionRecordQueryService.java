package com.example.scm.inventory.application.service;

import com.example.scm.common.core.TenantContext;
import com.example.scm.inventory.application.query.InventoryTransactionRecordDTO;
import com.example.scm.inventory.domain.inventory.entity.InventoryTransactionRecord;
import com.example.scm.inventory.domain.inventory.repository.InventoryTransactionRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 库存流水查询服务。
 * 负责按业务单维度查询库存流水，并转换为查询 DTO。
 */
@Service
public class InventoryTransactionRecordQueryService {

    private final InventoryTransactionRecordRepository inventoryTransactionRecordRepository;

    public InventoryTransactionRecordQueryService(InventoryTransactionRecordRepository inventoryTransactionRecordRepository) {
        this.inventoryTransactionRecordRepository = inventoryTransactionRecordRepository;
    }

    /**
     * 按业务类型和业务单号查询库存流水。
     */
    public List<InventoryTransactionRecordDTO> listByBizNo(String bizType, String bizNo) {
        Long tenantId = TenantContext.getRequiredTenantId();
        return inventoryTransactionRecordRepository.findByBizNo(tenantId, bizType, bizNo).stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * 将领域库存流水对象转换为查询 DTO。
     */
    private InventoryTransactionRecordDTO toDTO(InventoryTransactionRecord record) {
        InventoryTransactionRecordDTO dto = new InventoryTransactionRecordDTO();
        dto.setTxnNo(record.getTxnNo());
        dto.setBizType(record.getBizType());
        dto.setBizNo(record.getBizNo());
        dto.setMaterialId(record.getMaterialId());
        dto.setWarehouseId(record.getWarehouseId());
        dto.setLocationId(record.getLocationId());
        dto.setTxnDirection(record.getTxnDirection().name());
        dto.setTxnQty(record.getTxnQty());
        dto.setBeforeQty(record.getBeforeQty());
        dto.setAfterQty(record.getAfterQty());
        return dto;
    }
}
