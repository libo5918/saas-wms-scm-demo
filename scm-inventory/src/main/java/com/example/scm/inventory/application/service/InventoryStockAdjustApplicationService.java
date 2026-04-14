package com.example.scm.inventory.application.service;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.TenantContext;
import com.example.scm.inventory.application.command.StockAdjustCommand;
import com.example.scm.inventory.application.command.StockAdjustItemCommand;
import com.example.scm.inventory.application.query.StockAdjustLineResultDTO;
import com.example.scm.inventory.application.query.StockAdjustResultDTO;
import com.example.scm.inventory.domain.inventory.entity.InventoryTransactionRecord;
import com.example.scm.inventory.domain.inventory.service.InventoryStockAdjustDomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 库存调整应用服务。
 * 适用于调用方已经明确知道调整方向和调整数量的场景，
 * 例如人工修正、审批后的盘盈盘亏处理、外部系统下发调账结果。
 */
@Service
@Slf4j
public class InventoryStockAdjustApplicationService {

    private final InventoryStockAdjustDomainService inventoryStockAdjustDomainService;

    public InventoryStockAdjustApplicationService(InventoryStockAdjustDomainService inventoryStockAdjustDomainService) {
        this.inventoryStockAdjustDomainService = inventoryStockAdjustDomainService;
    }

    /**
     * 执行库存调整。
     * 这里不会根据当前库存自动推导差异，只会按调用方给出的调整方向和数量直接调账。
     */
    @Transactional
    public StockAdjustResultDTO adjust(StockAdjustCommand command) {
        validateCommand(command);
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Start stock-adjust application flow, tenantId={}, bizType={}, bizNo={}, adjustType={}, itemCount={}",
                tenantId, command.getBizType(), command.getBizNo(), command.getAdjustType(), command.getItems().size());

        StockAdjustResultDTO result = new StockAdjustResultDTO();
        result.setBizType(command.getBizType());
        result.setBizNo(command.getBizNo());
        result.setAdjustType(command.getAdjustType());

        for (StockAdjustItemCommand item : command.getItems()) {
            InventoryTransactionRecord transactionRecord = inventoryStockAdjustDomainService.adjust(
                    tenantId,
                    command.getBizType(),
                    command.getBizNo(),
                    command.getAdjustType(),
                    command.getOperatorId(),
                    item.getMaterialId(),
                    item.getWarehouseId(),
                    item.getLocationId(),
                    item.getQuantity()
            );
            StockAdjustLineResultDTO line = new StockAdjustLineResultDTO();
            line.setTxnNo(transactionRecord.getTxnNo());
            line.setMaterialId(transactionRecord.getMaterialId());
            line.setWarehouseId(transactionRecord.getWarehouseId());
            line.setLocationId(transactionRecord.getLocationId());
            line.setQuantity(transactionRecord.getTxnQty());
            line.setBeforeQty(transactionRecord.getBeforeQty());
            line.setAfterQty(transactionRecord.getAfterQty());
            result.getLines().add(line);
        }

        log.info("Finish stock-adjust application flow, tenantId={}, bizType={}, bizNo={}, adjustType={}, lineCount={}",
                tenantId, result.getBizType(), result.getBizNo(), result.getAdjustType(), result.getLines().size());
        return result;
    }

    /**
     * 校验库存调整请求的基本完整性。
     */
    private void validateCommand(StockAdjustCommand command) {
        if (!StringUtils.hasText(command.getBizType())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "bizType cannot be blank");
        }
        if (!StringUtils.hasText(command.getBizNo())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "bizNo cannot be blank");
        }
        if (!StringUtils.hasText(command.getAdjustType())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "adjustType cannot be blank");
        }
        if (command.getOperatorId() == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "operatorId cannot be null");
        }
        if (command.getItems() == null || command.getItems().isEmpty()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "stock-adjust items cannot be empty");
        }
    }
}
