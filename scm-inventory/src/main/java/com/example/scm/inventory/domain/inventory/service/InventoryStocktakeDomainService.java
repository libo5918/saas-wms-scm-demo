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
 * 库存盘点领域服务。
 * 负责比较账面数量与实盘数量，并在存在差异时生成对应调整流水。
 */
@Service
public class InventoryStocktakeDomainService {

    public static final String ADJUST_TYPE_NONE = "NONE";
    public static final String ADJUST_TYPE_INCREASE = "INCREASE";
    public static final String ADJUST_TYPE_DECREASE = "DECREASE";
    private static final DateTimeFormatter TXN_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final InventoryTransactionRecordRepository inventoryTransactionRecordRepository;

    public InventoryStocktakeDomainService(InventoryBalanceRepository inventoryBalanceRepository,
                                           InventoryTransactionRecordRepository inventoryTransactionRecordRepository) {
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.inventoryTransactionRecordRepository = inventoryTransactionRecordRepository;
    }

    /**
     * 执行单条库存盘点。
     */
    public StocktakeExecutionResult stocktake(Long tenantId,
                                              String bizType,
                                              String bizNo,
                                              Long operatorId,
                                              Long materialId,
                                              Long warehouseId,
                                              Long locationId,
                                              BigDecimal countedQty) {
        // 盘点遵循“先实盘、后调账”：
        // 1. 读取当前账面库存
        // 2. 将账面库存与实盘数量比较
        // 3. 推导增量调整、减量调整或无差异
        // 4. 仅在存在差异时落调整流水
        validateArguments(tenantId, bizType, bizNo, operatorId, materialId, warehouseId, locationId, countedQty);
        InventoryKey inventoryKey = new InventoryKey(tenantId, materialId, warehouseId, locationId);
        InventoryBalance balance = inventoryBalanceRepository.findByKey(inventoryKey)
                .orElseGet(() -> InventoryBalance.initialize(inventoryKey, operatorId));
        BigDecimal systemQty = balance.getOnHandQty();
        BigDecimal varianceQty = countedQty.subtract(systemQty);

        if (varianceQty.signum() == 0) {
            return new StocktakeExecutionResult(systemQty, countedQty, varianceQty, ADJUST_TYPE_NONE, null);
        }

        if (varianceQty.signum() > 0) {
            if (inventoryTransactionRecordRepository.existsAdjustInRecord(
                    tenantId, bizType, bizNo, materialId, warehouseId, locationId)) {
                throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Duplicate stocktake request");
            }
            InventoryTransactionRecord record = balance.adjustIn(
                    generateTxnNo("STKIN", bizType, bizNo, materialId, warehouseId, locationId),
                    bizType,
                    bizNo,
                    varianceQty,
                    operatorId
            );
            inventoryBalanceRepository.save(balance);
            inventoryTransactionRecordRepository.save(record);
            return new StocktakeExecutionResult(systemQty, countedQty, varianceQty, ADJUST_TYPE_INCREASE, record);
        }

        if (inventoryTransactionRecordRepository.existsAdjustOutRecord(
                tenantId, bizType, bizNo, materialId, warehouseId, locationId)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Duplicate stocktake request");
        }
        InventoryTransactionRecord record = balance.adjustOut(
                generateTxnNo("STKOUT", bizType, bizNo, materialId, warehouseId, locationId),
                bizType,
                bizNo,
                varianceQty.abs(),
                operatorId
        );
        inventoryBalanceRepository.save(balance);
        inventoryTransactionRecordRepository.save(record);
        return new StocktakeExecutionResult(systemQty, countedQty, varianceQty, ADJUST_TYPE_DECREASE, record);
    }

    /**
     * 校验入参与基础业务规则。
     */
    private void validateArguments(Long tenantId,
                                   String bizType,
                                   String bizNo,
                                   Long operatorId,
                                   Long materialId,
                                   Long warehouseId,
                                   Long locationId,
                                   BigDecimal countedQty) {
        if (tenantId == null || operatorId == null || materialId == null || warehouseId == null || locationId == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "stocktake arguments cannot be null");
        }
        if (!StringUtils.hasText(bizType) || !StringUtils.hasText(bizNo)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "bizType and bizNo cannot be blank");
        }
        if (countedQty == null || countedQty.signum() < 0) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "countedQty cannot be negative");
        }
    }

    /**
     * 生成库存流水号。
     */
    private String generateTxnNo(String prefix, String bizType, String bizNo, Long materialId, Long warehouseId, Long locationId) {
        return prefix + "-" + bizType.toUpperCase() + "-" + TXN_TIME_FORMATTER.format(LocalDateTime.now())
                + "-" + bizNo + "-" + materialId + warehouseId + locationId;
    }

    /**
     * 盘点执行结果，包含账面数量、实盘数量、差异数量、调整类型以及可选的调整流水。
     */
    public record StocktakeExecutionResult(BigDecimal systemQty,
                                           BigDecimal countedQty,
                                           BigDecimal varianceQty,
                                           String adjustType,
                                           InventoryTransactionRecord transactionRecord) {
    }
}
