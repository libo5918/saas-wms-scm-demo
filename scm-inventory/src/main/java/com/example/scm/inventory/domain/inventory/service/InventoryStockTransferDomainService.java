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
 * 库存移库领域服务。
 * 负责源库位移出、目标库位移入以及双流水落库。
 */
@Service
public class InventoryStockTransferDomainService {

    private static final DateTimeFormatter TXN_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final InventoryTransactionRecordRepository inventoryTransactionRecordRepository;

    public InventoryStockTransferDomainService(InventoryBalanceRepository inventoryBalanceRepository,
                                               InventoryTransactionRecordRepository inventoryTransactionRecordRepository) {
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.inventoryTransactionRecordRepository = inventoryTransactionRecordRepository;
    }

    /**
     * 执行单条库存移库。
     */
    public TransferExecutionResult transfer(Long tenantId,
                                            String bizType,
                                            String bizNo,
                                            Long operatorId,
                                            Long materialId,
                                            Long fromWarehouseId,
                                            Long fromLocationId,
                                            Long toWarehouseId,
                                            Long toLocationId,
                                            BigDecimal quantity) {
        validateArguments(tenantId, bizType, bizNo, operatorId, materialId, fromWarehouseId, fromLocationId, toWarehouseId, toLocationId, quantity);

        if (inventoryTransactionRecordRepository.existsMoveOutRecord(
                tenantId, bizType, bizNo, materialId, fromWarehouseId, fromLocationId)
                || inventoryTransactionRecordRepository.existsMoveInRecord(
                tenantId, bizType, bizNo, materialId, toWarehouseId, toLocationId)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Duplicate stock-transfer request");
        }

        InventoryKey fromKey = new InventoryKey(tenantId, materialId, fromWarehouseId, fromLocationId);
        InventoryBalance fromBalance = inventoryBalanceRepository.findByKey(fromKey)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Source inventory balance not found"));

        InventoryKey toKey = new InventoryKey(tenantId, materialId, toWarehouseId, toLocationId);
        InventoryBalance toBalance = inventoryBalanceRepository.findByKey(toKey)
                .orElseGet(() -> InventoryBalance.initialize(toKey, operatorId));

        InventoryTransactionRecord moveOutRecord = fromBalance.moveOut(
                generateTxnNo("MOVEOUT", bizType, bizNo, materialId, fromWarehouseId, fromLocationId),
                bizType,
                bizNo,
                quantity,
                operatorId
        );
        InventoryTransactionRecord moveInRecord = toBalance.moveIn(
                generateTxnNo("MOVEIN", bizType, bizNo, materialId, toWarehouseId, toLocationId),
                bizType,
                bizNo,
                quantity,
                operatorId
        );

        inventoryBalanceRepository.save(fromBalance);
        inventoryBalanceRepository.save(toBalance);
        inventoryTransactionRecordRepository.save(moveOutRecord);
        inventoryTransactionRecordRepository.save(moveInRecord);
        return new TransferExecutionResult(moveOutRecord, moveInRecord);
    }

    /**
     * 校验入参与基础业务规则。
     */
    private void validateArguments(Long tenantId,
                                   String bizType,
                                   String bizNo,
                                   Long operatorId,
                                   Long materialId,
                                   Long fromWarehouseId,
                                   Long fromLocationId,
                                   Long toWarehouseId,
                                   Long toLocationId,
                                   BigDecimal quantity) {
        if (tenantId == null || operatorId == null || materialId == null || fromWarehouseId == null
                || fromLocationId == null || toWarehouseId == null || toLocationId == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "stock-transfer arguments cannot be null");
        }
        if (!StringUtils.hasText(bizType) || !StringUtils.hasText(bizNo)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "bizType and bizNo cannot be blank");
        }
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "stock-transfer quantity must be greater than zero");
        }
        if (fromWarehouseId.equals(toWarehouseId) && fromLocationId.equals(toLocationId)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Source and target location cannot be the same");
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
     * 移库执行结果，包含移出流水和移入流水。
     */
    public record TransferExecutionResult(InventoryTransactionRecord moveOutRecord,
                                          InventoryTransactionRecord moveInRecord) {
    }
}
