package com.example.scm.inventory.domain.inventory.service;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.inventory.domain.inventory.aggregate.InventoryBalance;
import com.example.scm.inventory.domain.inventory.entity.InventoryTransactionRecord;
import com.example.scm.inventory.domain.inventory.repository.InventoryBalanceRepository;
import com.example.scm.inventory.domain.inventory.repository.InventoryTransactionRecordRepository;
import com.example.scm.inventory.domain.inventory.valueobject.InventoryKey;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 锁定库存出库领域服务。
 * 用于销售发货等已锁库业务，扣减现存和锁定数量，不再重复扣减可用数量。
 */
@Service
public class InventoryLockedStockOutDomainService {

    private static final DateTimeFormatter TXN_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final InventoryTransactionRecordRepository inventoryTransactionRecordRepository;

    public InventoryLockedStockOutDomainService(InventoryBalanceRepository inventoryBalanceRepository,
                                                InventoryTransactionRecordRepository inventoryTransactionRecordRepository) {
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.inventoryTransactionRecordRepository = inventoryTransactionRecordRepository;
    }

    public InventoryTransactionRecord stockOut(Long tenantId,
                                               String bizType,
                                               String bizNo,
                                               Long operatorId,
                                               Long materialId,
                                               Long warehouseId,
                                               Long locationId,
                                               BigDecimal quantity) {
        validateArguments(tenantId, bizType, bizNo, operatorId, materialId, warehouseId, locationId, quantity);

        if (inventoryTransactionRecordRepository.existsStockOutRecord(
                tenantId, bizType, bizNo, materialId, warehouseId, locationId)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Duplicate stock-out request");
        }

        InventoryKey inventoryKey = new InventoryKey(tenantId, materialId, warehouseId, locationId);
        InventoryBalance balance = inventoryBalanceRepository.findByKey(inventoryKey)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Inventory balance not found"));

        String txnNo = generateTxnNo(bizType, bizNo, materialId, warehouseId, locationId);
        InventoryTransactionRecord record = balance.lockedStockOut(txnNo, bizType, bizNo, quantity, operatorId);
        inventoryBalanceRepository.save(balance);
        inventoryTransactionRecordRepository.save(record);
        return record;
    }

    private void validateArguments(Long tenantId,
                                   String bizType,
                                   String bizNo,
                                   Long operatorId,
                                   Long materialId,
                                   Long warehouseId,
                                   Long locationId,
                                   BigDecimal quantity) {
        if (tenantId == null || operatorId == null || materialId == null || warehouseId == null || locationId == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "locked stock-out arguments cannot be null");
        }
        if (!StringUtils.hasText(bizType) || !StringUtils.hasText(bizNo)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "bizType and bizNo cannot be blank");
        }
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "locked stock-out quantity must be greater than zero");
        }
    }

    private String generateTxnNo(String bizType, String bizNo, Long materialId, Long warehouseId, Long locationId) {
        return "OUT-LOCKED-" + bizType.toUpperCase() + "-" + TXN_TIME_FORMATTER.format(LocalDateTime.now())
                + "-" + bizNo + "-" + materialId + warehouseId + locationId;
    }
}
