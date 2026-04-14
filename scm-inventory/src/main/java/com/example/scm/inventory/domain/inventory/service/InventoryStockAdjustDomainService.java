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
public class InventoryStockAdjustDomainService {

    public static final String ADJUST_TYPE_INCREASE = "INCREASE";
    public static final String ADJUST_TYPE_DECREASE = "DECREASE";
    private static final DateTimeFormatter TXN_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final InventoryTransactionRecordRepository inventoryTransactionRecordRepository;

    public InventoryStockAdjustDomainService(InventoryBalanceRepository inventoryBalanceRepository,
                                             InventoryTransactionRecordRepository inventoryTransactionRecordRepository) {
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.inventoryTransactionRecordRepository = inventoryTransactionRecordRepository;
    }

    public InventoryTransactionRecord adjust(Long tenantId,
                                             String bizType,
                                             String bizNo,
                                             String adjustType,
                                             Long operatorId,
                                             Long materialId,
                                             Long warehouseId,
                                             Long locationId,
                                             BigDecimal quantity) {
        validateArguments(tenantId, bizType, bizNo, adjustType, operatorId, materialId, warehouseId, locationId, quantity);

        InventoryKey inventoryKey = new InventoryKey(tenantId, materialId, warehouseId, locationId);
        InventoryBalance balance = inventoryBalanceRepository.findByKey(inventoryKey)
                .orElseGet(() -> InventoryBalance.initialize(inventoryKey, operatorId));

        if (ADJUST_TYPE_INCREASE.equals(adjustType)) {
            if (inventoryTransactionRecordRepository.existsAdjustInRecord(
                    tenantId, bizType, bizNo, materialId, warehouseId, locationId)) {
                throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Duplicate stock-adjust request");
            }
            InventoryTransactionRecord record = balance.adjustIn(
                    generateTxnNo("ADJIN", bizType, bizNo, materialId, warehouseId, locationId),
                    bizType,
                    bizNo,
                    quantity,
                    operatorId
            );
            inventoryBalanceRepository.save(balance);
            inventoryTransactionRecordRepository.save(record);
            return record;
        }

        if (inventoryTransactionRecordRepository.existsAdjustOutRecord(
                tenantId, bizType, bizNo, materialId, warehouseId, locationId)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Duplicate stock-adjust request");
        }
        InventoryTransactionRecord record = balance.adjustOut(
                generateTxnNo("ADJOUT", bizType, bizNo, materialId, warehouseId, locationId),
                bizType,
                bizNo,
                quantity,
                operatorId
        );
        inventoryBalanceRepository.save(balance);
        inventoryTransactionRecordRepository.save(record);
        return record;
    }

    private void validateArguments(Long tenantId,
                                   String bizType,
                                   String bizNo,
                                   String adjustType,
                                   Long operatorId,
                                   Long materialId,
                                   Long warehouseId,
                                   Long locationId,
                                   BigDecimal quantity) {
        if (tenantId == null || operatorId == null || materialId == null || warehouseId == null || locationId == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "stock-adjust arguments cannot be null");
        }
        if (!StringUtils.hasText(bizType) || !StringUtils.hasText(bizNo)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "bizType and bizNo cannot be blank");
        }
        if (!ADJUST_TYPE_INCREASE.equals(adjustType) && !ADJUST_TYPE_DECREASE.equals(adjustType)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "adjustType must be INCREASE or DECREASE");
        }
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "stock-adjust quantity must be greater than zero");
        }
    }

    private String generateTxnNo(String prefix, String bizType, String bizNo, Long materialId, Long warehouseId, Long locationId) {
        return prefix + "-" + bizType.toUpperCase() + "-" + TXN_TIME_FORMATTER.format(LocalDateTime.now())
                + "-" + bizNo + "-" + materialId + warehouseId + locationId;
    }
}
