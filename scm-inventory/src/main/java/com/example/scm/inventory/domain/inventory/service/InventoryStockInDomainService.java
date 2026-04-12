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

@Service
public class InventoryStockInDomainService {

    private static final DateTimeFormatter TXN_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final InventoryTransactionRecordRepository inventoryTransactionRecordRepository;

    public InventoryStockInDomainService(InventoryBalanceRepository inventoryBalanceRepository,
                                         InventoryTransactionRecordRepository inventoryTransactionRecordRepository) {
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.inventoryTransactionRecordRepository = inventoryTransactionRecordRepository;
    }

    public InventoryTransactionRecord stockIn(Long tenantId,
                                              String bizType,
                                              String bizNo,
                                              Long operatorId,
                                              Long materialId,
                                              Long warehouseId,
                                              Long locationId,
                                              BigDecimal quantity) {
        validateArguments(tenantId, bizType, bizNo, operatorId, materialId, warehouseId, locationId, quantity);

        if (inventoryTransactionRecordRepository.existsStockInRecord(
                tenantId, bizType, bizNo, materialId, warehouseId, locationId)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Duplicate stock-in request");
        }

        InventoryKey inventoryKey = new InventoryKey(tenantId, materialId, warehouseId, locationId);
        InventoryBalance balance = inventoryBalanceRepository.findByKey(inventoryKey)
                .orElseGet(() -> InventoryBalance.initialize(inventoryKey, operatorId));

        String txnNo = generateTxnNo(bizType, bizNo, materialId, warehouseId, locationId);
        InventoryTransactionRecord record = balance.stockIn(txnNo, bizType, bizNo, quantity, operatorId);
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
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "stock-in arguments cannot be null");
        }
        if (!StringUtils.hasText(bizType) || !StringUtils.hasText(bizNo)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "bizType and bizNo cannot be blank");
        }
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "stock-in quantity must be greater than zero");
        }
    }

    private String generateTxnNo(String bizType, String bizNo, Long materialId, Long warehouseId, Long locationId) {
        return "IN-" + bizType.toUpperCase() + "-" + TXN_TIME_FORMATTER.format(LocalDateTime.now())
                + "-" + bizNo + "-" + materialId + warehouseId + locationId;
    }
}
